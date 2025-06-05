import * as Superwall from "expo-superwall"

// Type definitions for Superwall delegate events (these would ideally come from the expo-superwall package)
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

// Sealed class for delegate events - using abstract base class in TypeScript
export abstract class TestDelegateEvent {
  abstract type: string
}

export class DidDismissPaywallEvent extends TestDelegateEvent {
  type = 'didDismissPaywall'
  
  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
}

export class DidPresentPaywallEvent extends TestDelegateEvent {
  type = 'didPresentPaywall'
  
  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
}

export class HandleCustomPaywallActionEvent extends TestDelegateEvent {
  type = 'handleCustomPaywallAction'
  
  constructor(public name: string) {
    super()
  }
}

export class HandleLogEvent extends TestDelegateEvent {
  type = 'handleLog'
  
  constructor(
    public level: string,
    public scope: string,
    public message?: string,
    public info?: { [key: string]: any },
    public error?: string
  ) {
    super()
  }
}

export class HandleSuperwallEventEvent extends TestDelegateEvent {
  type = 'handleSuperwallEvent'
  
  constructor(public eventInfo: SuperwallEventInfo) {
    super()
  }
}

export class PaywallWillOpenDeepLinkEvent extends TestDelegateEvent {
  type = 'paywallWillOpenDeepLink'
  
  constructor(public url: URL) {
    super()
  }
}

export class PaywallWillOpenURLEvent extends TestDelegateEvent {
  type = 'paywallWillOpenURL'
  
  constructor(public url: URL) {
    super()
  }
}

export class SubscriptionStatusDidChangeEvent extends TestDelegateEvent {
  type = 'subscriptionStatusDidChange'
  
  constructor(public newValue: SubscriptionStatus) {
    super()
  }
}

export class WillDismissPaywallEvent extends TestDelegateEvent {
  type = 'willDismissPaywall'
  
  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
}

export class WillPresentPaywallEvent extends TestDelegateEvent {
  type = 'willPresentPaywall'
  
  constructor(public paywallInfo: PaywallInfo) {
    super()
  }
} 