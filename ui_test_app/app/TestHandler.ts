import * as Superwall from "expo-superwall"

// Type definitions for Superwall events (these would ideally come from the expo-superwall package)
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

interface PaywallResult {
  type: 'purchased' | 'declined' | 'restored' | 'closed'
}

enum PaywallSkippedReason {
  holdout = 'holdout',
  noRuleMatch = 'noRuleMatch',
  eventNotFound = 'eventNotFound',
  userIsSubscribed = 'userIsSubscribed'
}

interface PaywallPresentationHandler {
  onPresentHandler?: (info: PaywallInfo) => void
  onDismissHandler?: (info: PaywallInfo, result: PaywallResult) => void
  onErrorHandler?: (error: string) => void
  onSkipHandler?: (reason: PaywallSkippedReason) => void
}

// Event classes
export abstract class HandlerEvent {
  abstract type: string
}

export class PresentEvent extends HandlerEvent {
  type = 'present'
  
  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
}

export class DismissEvent extends HandlerEvent {
  type = 'dismiss'
  
  constructor(
    public paywallInfo: PaywallInfo,
    public result: PaywallResult
  ) {
    super()
  }
}

export class ErrorEvent extends HandlerEvent {
  type = 'error'
  
  constructor(public errorMessage: string) {
    super()
  }
}

export class SkipEvent extends HandlerEvent {
  type = 'skip'
  
  constructor(public reason: PaywallSkippedReason) {
    super()
  }
}

export class TestHandler {
  public events: HandlerEvent[] = []
  private handler: PaywallPresentationHandler

  constructor() {
    this.handler = {
      onDismissHandler: (info: PaywallInfo, result: PaywallResult) => {
        console.log('onDismissHandler:', info, result)
        this.events.push(new DismissEvent(info, result))
      },
      onErrorHandler: (error: string) => {
        console.log('onErrorHandler:', error)
        this.events.push(new ErrorEvent(error))
      },
      onPresentHandler: (info: PaywallInfo) => {
        console.log('onPresentHandler:', info)
        this.events.push(new PresentEvent(info))
      },
      onSkipHandler: (reason: PaywallSkippedReason) => {
        console.log('onSkipHandler:', reason)
        this.events.push(new SkipEvent(reason))
      }
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
      case 'present':
        return 'OnPresent'
      case 'dismiss':
        return 'OnDismiss'
      case 'error':
        return 'OnError'
      case 'skip':
        return 'OnSkip'
      default:
        return 'Unknown'
    }
  }
} 