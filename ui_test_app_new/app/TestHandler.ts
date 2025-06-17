import type {
  PaywallInfo,
  PaywallPresentationHandler,
  PaywallResult,
  PaywallSkippedReason,
} from "expo-superwall/compat"

export abstract class HandlerEvent {
  abstract type: string
}

export class PresentEvent extends HandlerEvent {
  type = "present"

  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
}

export class DismissEvent extends HandlerEvent {
  type = "dismiss"

  constructor(
    public paywallInfo: PaywallInfo,
    public result: PaywallResult,
  ) {
    super()
  }
}

export class ErrorEvent extends HandlerEvent {
  type = "error"

  constructor(public errorMessage: string) {
    super()
  }
}

export class SkipEvent extends HandlerEvent {
  type = "skip"

  constructor(public reason: PaywallSkippedReason) {
    super()
  }
}

export class TestHandler {
  public events: HandlerEvent[] = []
  private handler: PaywallPresentationHandler

  constructor() {
    // @ts-expect-error
    this.handler = {
      onDismissHandler: (info: PaywallInfo, result) => {
        console.log("onDismissHandler:", info, result)
        this.events.push(new DismissEvent(info, result))
      },
      onErrorHandler: (error: string) => {
        console.log("onErrorHandler:", error)
        this.events.push(new ErrorEvent(error))
      },
      onPresentHandler: (info: PaywallInfo) => {
        console.log("onPresentHandler:", info)
        this.events.push(new PresentEvent(info))
      },
      onSkipHandler: (reason: PaywallSkippedReason) => {
        console.log("onSkipHandler:", reason)
        this.events.push(new SkipEvent(reason))
      },
    }
  }

  getHandler(): PaywallPresentationHandler {
    return this.handler
  }

  clearEvents(): void {
    this.events = []
  }

  getEventCount(): number {
    return this.events.length
  }

  getEventName(event: HandlerEvent): string {
    switch (event.type) {
      case "present":
        return "OnPresent"
      case "dismiss":
        return "OnDismiss"
      case "error":
        return "OnError"
      case "skip":
        return "OnSkip"
      default:
        return "Unknown"
    }
  }
}
