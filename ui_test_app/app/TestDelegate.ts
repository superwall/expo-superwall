import * as Superwall from "expo-superwall"
import {
  TestDelegateEvent,
  DidDismissPaywallEvent,
  DidPresentPaywallEvent,
  HandleCustomPaywallActionEvent,
  HandleLogEvent,
  HandleSuperwallEventEvent,
  PaywallWillOpenDeepLinkEvent,
  PaywallWillOpenURLEvent,
  SubscriptionStatusDidChangeEvent,
  WillDismissPaywallEvent,
  WillPresentPaywallEvent
} from "./TestDelegateEvent"

// Type definitions for Superwall delegate (these would ideally come from the expo-superwall package)
interface PaywallInfo {
  identifier?: string
  experiment?: any
  triggerSessionId?: string
  products?: any[]
  productIds?: string[]
  name?: string
  url?: string
  presentedByEventWithName?: string
  presentedByEventWithId?: string
  presentedByEventAt?: string
  presentedBy?: string
  presentationSourceType?: string
  responseLoadStartTime?: string
  responseLoadCompleteTime?: string
  responseLoadFailTime?: string
  webViewLoadStartTime?: string
  webViewLoadCompleteTime?: string
  webViewLoadFailTime?: string
  productsLoadStartTime?: string
  productsLoadCompleteTime?: string
  productsLoadFailTime?: string
  paywalljsVersion?: string
  isFreeTrialAvailable?: boolean
  featureGating?: any
  closeReason?: any
  localNotifications?: any[]
  computedPropertyRequests?: any[]
  surveys?: any[]
}

interface SuperwallEventInfo {
  event?: any
  params?: { [key: string]: any }
}

interface SubscriptionStatus {
  type: 'active' | 'inactive' | 'unknown'
  entitlements?: Array<{ id: string }>
}

// SuperwallDelegate interface - mock implementation based on Flutter version
interface SuperwallDelegate {
  didDismissPaywall?(paywallInfo: PaywallInfo): void
  didPresentPaywall?(paywallInfo: PaywallInfo): void
  handleCustomPaywallAction?(name: string): void
  handleLog?(level: string, scope: string, message?: string, info?: { [key: string]: any }, error?: string): void
  handleSuperwallEvent?(eventInfo: SuperwallEventInfo): void
  paywallWillOpenDeepLink?(url: URL): void
  paywallWillOpenURL?(url: URL): void
  subscriptionStatusDidChange?(newValue: SubscriptionStatus): void
  willDismissPaywall?(paywallInfo: PaywallInfo): void
  willPresentPaywall?(paywallInfo: PaywallInfo): void
}

export class TestDelegate implements SuperwallDelegate {
  private _events: TestDelegateEvent[] = []

  get events(): TestDelegateEvent[] {
    return this._events
  }

  get eventsWithoutLog(): TestDelegateEvent[] {
    return this._events.filter((event) => 
      event.type !== 'handleLog' && event.type !== 'handleSuperwallEvent'
    )
  }

  didDismissPaywall(paywallInfo: PaywallInfo): void {
    this._events.push(new DidDismissPaywallEvent(paywallInfo))
  }

  didPresentPaywall(paywallInfo: PaywallInfo): void {
    this._events.push(new DidPresentPaywallEvent(paywallInfo))
  }

  handleCustomPaywallAction(name: string): void {
    this._events.push(new HandleCustomPaywallActionEvent(name))
  }

  handleLog(
    level: string, 
    scope: string, 
    message?: string, 
    info?: { [key: string]: any }, 
    error?: string
  ): void {
    this._events.push(new HandleLogEvent(level, scope, message, info, error))
  }

  handleSuperwallEvent(eventInfo: SuperwallEventInfo): void {
    this._events.push(new HandleSuperwallEventEvent(eventInfo))
  }

  paywallWillOpenDeepLink(url: URL): void {
    this._events.push(new PaywallWillOpenDeepLinkEvent(url))
  }

  paywallWillOpenURL(url: URL): void {
    this._events.push(new PaywallWillOpenURLEvent(url))
  }

  subscriptionStatusDidChange(newValue: SubscriptionStatus): void {
    this._events.push(new SubscriptionStatusDidChangeEvent(newValue))
  }

  willDismissPaywall(paywallInfo: PaywallInfo): void {
    this._events.push(new WillDismissPaywallEvent(paywallInfo))
  }

  willPresentPaywall(paywallInfo: PaywallInfo): void {
    this._events.push(new WillPresentPaywallEvent(paywallInfo))
  }

  // Additional utility methods
  clearEvents(): void {
    this._events = []
  }

  getEventCount(): number {
    return this._events.length
  }

  getEventsWithoutLogCount(): number {
    return this.eventsWithoutLog.length
  }

  getEventsByType(type: string): TestDelegateEvent[] {
    return this._events.filter(event => event.type === type)
  }

  getLastEvent(): TestDelegateEvent | undefined {
    return this._events[this._events.length - 1]
  }
} 