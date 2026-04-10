import SuperwallExpoModule from "../SuperwallExpoModule"
import type { SuperwallEventCallbacks } from "../SuperwallEventCallbacks"

type NativeEventName =
  | "onPaywallPresent"
  | "onPaywallDismiss"
  | "onPaywallError"
  | "onPaywallSkip"
  | "subscriptionStatusDidChange"
  | "handleSuperwallEvent"
  | "handleCustomPaywallAction"
  | "willDismissPaywall"
  | "willPresentPaywall"
  | "didDismissPaywall"
  | "didPresentPaywall"
  | "paywallWillOpenURL"
  | "paywallWillOpenDeepLink"
  | "handleLog"
  | "willRedeemLink"
  | "didRedeemLink"
  | "onPurchase"
  | "onPurchaseRestore"
  | "onBackPressed"
  | "onCustomCallback"

type BufferedEventName = Exclude<
  NativeEventName,
  "onPurchase" | "onPurchaseRestore" | "onBackPressed" | "onCustomCallback"
>

type Subscriber = {
  id: number
  handlerId?: string
  getCallbacks: () => SuperwallEventCallbacks
}

type BufferedEvent = {
  eventName: BufferedEventName
  payload: any
}

const subscribers = new Map<number, Subscriber>()
const bufferedEvents: BufferedEvent[] = []

let nextSubscriberId = 1

const isMatchingSubscriber = (
  subscriber: Subscriber,
  payload: { handlerId?: string },
  replay: boolean,
): boolean => {
  if (payload.handlerId == null) {
    return true
  }

  if (replay) {
    return subscriber.handlerId === payload.handlerId
  }

  return subscriber.handlerId == null || subscriber.handlerId === payload.handlerId
}

const deliverToSubscriber = (
  subscriber: Subscriber,
  eventName: NativeEventName,
  payload: any,
  replay = false,
): boolean => {
  const callbacks = subscriber.getCallbacks()

  switch (eventName) {
    case "onPaywallPresent":
      if (!isMatchingSubscriber(subscriber, payload, replay)) return false
      callbacks.onPaywallPresent?.(payload.paywallInfoJson)
      return callbacks.onPaywallPresent != null
    case "onPaywallDismiss":
      if (!isMatchingSubscriber(subscriber, payload, replay)) return false
      callbacks.onPaywallDismiss?.(payload.paywallInfoJson, payload.result)
      return callbacks.onPaywallDismiss != null
    case "onPaywallError":
      if (!isMatchingSubscriber(subscriber, payload, replay)) return false
      callbacks.onPaywallError?.(payload.errorString)
      return callbacks.onPaywallError != null
    case "onPaywallSkip":
      if (!isMatchingSubscriber(subscriber, payload, replay)) return false
      callbacks.onPaywallSkip?.(payload.skippedReason)
      return callbacks.onPaywallSkip != null
    case "subscriptionStatusDidChange":
      callbacks.onSubscriptionStatusChange?.(payload.to)
      return callbacks.onSubscriptionStatusChange != null
    case "handleSuperwallEvent":
      callbacks.onSuperwallEvent?.(payload.eventInfo)
      return callbacks.onSuperwallEvent != null
    case "handleCustomPaywallAction":
      callbacks.onCustomPaywallAction?.(payload.name)
      return callbacks.onCustomPaywallAction != null
    case "willDismissPaywall":
      callbacks.willDismissPaywall?.(payload.info)
      return callbacks.willDismissPaywall != null
    case "willPresentPaywall":
      callbacks.willPresentPaywall?.(payload.info)
      return callbacks.willPresentPaywall != null
    case "didDismissPaywall":
      callbacks.didDismissPaywall?.(payload.info)
      return callbacks.didDismissPaywall != null
    case "didPresentPaywall":
      callbacks.didPresentPaywall?.(payload.info)
      return callbacks.didPresentPaywall != null
    case "paywallWillOpenURL":
      callbacks.onPaywallWillOpenURL?.(payload.url)
      return callbacks.onPaywallWillOpenURL != null
    case "paywallWillOpenDeepLink":
      callbacks.onPaywallWillOpenDeepLink?.(payload.url)
      return callbacks.onPaywallWillOpenDeepLink != null
    case "handleLog":
      callbacks.onLog?.(payload)
      return callbacks.onLog != null
    case "willRedeemLink":
      callbacks.willRedeemLink?.()
      return callbacks.willRedeemLink != null
    case "didRedeemLink":
      callbacks.didRedeemLink?.(payload)
      return callbacks.didRedeemLink != null
    case "onPurchase":
      callbacks.onPurchase?.(payload)
      return callbacks.onPurchase != null
    case "onPurchaseRestore":
      callbacks.onPurchaseRestore?.()
      return callbacks.onPurchaseRestore != null
    case "onBackPressed": {
      const shouldConsume = callbacks.onBackPressed?.(payload.paywallInfo)
      if (callbacks.onBackPressed == null) return false
      SuperwallExpoModule.didHandleBackPressed(shouldConsume ?? false)
      return true
    }
    case "onCustomCallback":
      if (!subscriber.handlerId || subscriber.handlerId !== payload.handlerId) return false

      if (!callbacks.onCustomCallback) {
        void SuperwallExpoModule.didHandleCustomCallback(payload.callbackId, "failure", undefined)
        return true
      }

      Promise.resolve(
        callbacks.onCustomCallback({
          name: payload.name,
          variables: payload.variables,
        }),
      ).then(
        (result) => {
          void SuperwallExpoModule.didHandleCustomCallback(
            payload.callbackId,
            result.status,
            result.data,
          )
        },
        () => {
          void SuperwallExpoModule.didHandleCustomCallback(payload.callbackId, "failure", undefined)
        },
      )
      return true
    default:
      return false
  }
}

const deliverLiveEvent = (eventName: NativeEventName, payload: any): boolean => {
  let delivered = false

  for (const subscriber of subscribers.values()) {
    delivered = deliverToSubscriber(subscriber, eventName, payload) || delivered
  }

  return delivered
}

const deliverBufferedEvent = (bufferedEvent: BufferedEvent): boolean => {
  for (const subscriber of subscribers.values()) {
    if (deliverToSubscriber(subscriber, bufferedEvent.eventName, bufferedEvent.payload, true)) {
      return true
    }
  }

  return false
}

const flushBufferedEvents = (): void => {
  for (let index = 0; index < bufferedEvents.length; ) {
    if (deliverBufferedEvent(bufferedEvents[index])) {
      bufferedEvents.splice(index, 1)
      continue
    }

    index += 1
  }
}

const enqueueBufferedEvent = (eventName: BufferedEventName, payload: any): void => {
  bufferedEvents.push({ eventName, payload })
}

const handleBufferedNativeEvent = (eventName: BufferedEventName, payload: any): void => {
  if (!deliverLiveEvent(eventName, payload)) {
    enqueueBufferedEvent(eventName, payload)
  }
}

const handleImmediateNativeEvent = (
  eventName: Exclude<NativeEventName, BufferedEventName>,
  payload: any,
): void => {
  if (eventName === "onPurchase" || eventName === "onPurchaseRestore") {
    deliverLiveEvent(eventName, payload)
    return
  }

  if (eventName === "onBackPressed" || eventName === "onCustomCallback") {
    for (const subscriber of subscribers.values()) {
      if (deliverToSubscriber(subscriber, eventName, payload)) {
        return
      }
    }

    if (eventName === "onBackPressed") {
      SuperwallExpoModule.didHandleBackPressed(false)
    } else {
      void SuperwallExpoModule.didHandleCustomCallback(payload.callbackId, "failure", undefined)
    }

    return
  }

  for (const subscriber of subscribers.values()) {
    if (deliverToSubscriber(subscriber, eventName, payload)) {
      return
    }
  }
}

const installNativeListeners = (): void => {
  SuperwallExpoModule.addListener("onPaywallPresent", (payload) => {
    handleBufferedNativeEvent("onPaywallPresent", payload)
  })
  SuperwallExpoModule.addListener("onPaywallDismiss", (payload) => {
    handleBufferedNativeEvent("onPaywallDismiss", payload)
  })
  SuperwallExpoModule.addListener("onPaywallError", (payload) => {
    handleBufferedNativeEvent("onPaywallError", payload)
  })
  SuperwallExpoModule.addListener("onPaywallSkip", (payload) => {
    handleBufferedNativeEvent("onPaywallSkip", payload)
  })
  SuperwallExpoModule.addListener("subscriptionStatusDidChange", (payload) => {
    handleBufferedNativeEvent("subscriptionStatusDidChange", payload)
  })
  SuperwallExpoModule.addListener("handleSuperwallEvent", (payload) => {
    handleBufferedNativeEvent("handleSuperwallEvent", payload)
  })
  SuperwallExpoModule.addListener("handleCustomPaywallAction", (payload) => {
    handleBufferedNativeEvent("handleCustomPaywallAction", payload)
  })
  SuperwallExpoModule.addListener("willDismissPaywall", (payload) => {
    handleBufferedNativeEvent("willDismissPaywall", payload)
  })
  SuperwallExpoModule.addListener("willPresentPaywall", (payload) => {
    handleBufferedNativeEvent("willPresentPaywall", payload)
  })
  SuperwallExpoModule.addListener("didDismissPaywall", (payload) => {
    handleBufferedNativeEvent("didDismissPaywall", payload)
  })
  SuperwallExpoModule.addListener("didPresentPaywall", (payload) => {
    handleBufferedNativeEvent("didPresentPaywall", payload)
  })
  SuperwallExpoModule.addListener("paywallWillOpenURL", (payload) => {
    handleBufferedNativeEvent("paywallWillOpenURL", payload)
  })
  SuperwallExpoModule.addListener("paywallWillOpenDeepLink", (payload) => {
    handleBufferedNativeEvent("paywallWillOpenDeepLink", payload)
  })
  SuperwallExpoModule.addListener("handleLog", (payload) => {
    handleBufferedNativeEvent("handleLog", payload)
  })
  SuperwallExpoModule.addListener("willRedeemLink", (payload) => {
    handleBufferedNativeEvent("willRedeemLink", payload)
  })
  SuperwallExpoModule.addListener("didRedeemLink", (payload) => {
    handleBufferedNativeEvent("didRedeemLink", payload)
  })
  SuperwallExpoModule.addListener("onPurchase", (payload) => {
    handleImmediateNativeEvent("onPurchase", payload)
  })
  SuperwallExpoModule.addListener("onPurchaseRestore", () => {
    handleImmediateNativeEvent("onPurchaseRestore", undefined)
  })
  SuperwallExpoModule.addListener("onBackPressed", (payload) => {
    handleImmediateNativeEvent("onBackPressed", payload)
  })
  SuperwallExpoModule.addListener("onCustomCallback", (payload) => {
    handleImmediateNativeEvent("onCustomCallback", payload)
  })
}

installNativeListeners()

export const subscribeToSuperwallEvents = (
  handlerId: string | undefined,
  getCallbacks: () => SuperwallEventCallbacks,
): (() => void) => {
  const id = nextSubscriberId
  nextSubscriberId += 1

  subscribers.set(id, {
    id,
    handlerId,
    getCallbacks,
  })
  flushBufferedEvents()

  return () => {
    subscribers.delete(id)
  }
}

export const __resetSuperwallEventBridgeForTests = (): void => {
  subscribers.clear()
  bufferedEvents.splice(0, bufferedEvents.length)
  nextSubscriberId = 1
}
