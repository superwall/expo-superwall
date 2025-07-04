import {
  PaywallInfo,
  SubscriptionStatus,
  SuperwallEventInfo,
} from "expo-superwall/compat"

// Abstract base class for delegate events (equivalent to Dart sealed class)
export abstract class TestDelegateEvent {
  abstract readonly type: string
}

export class DidDismissPaywallEvent extends TestDelegateEvent {
  readonly type = 'didDismissPaywall'
  
  constructor(public readonly paywallInfo: PaywallInfo) {
    super()
  }
}

export class DidPresentPaywallEvent extends TestDelegateEvent {
  readonly type = 'didPresentPaywall'
  
  constructor(public readonly paywallInfo: PaywallInfo) {
    super()
  }
}

export class HandleCustomPaywallActionEvent extends TestDelegateEvent {
  readonly type = 'handleCustomPaywallAction'
  
  constructor(public readonly name: string) {
    super()
  }
}

export class HandleLogEvent extends TestDelegateEvent {
  readonly type = 'handleLog'
  
  constructor(
    public readonly level: string,
    public readonly scope: string,
    public readonly message?: string,
    public readonly info?: { [key: string]: any },
    public readonly error?: string
  ) {
    super()
  }
}

export class HandleSuperwallEventEvent extends TestDelegateEvent {
  readonly type = 'handleSuperwallEvent'
  
  constructor(public readonly eventInfo: SuperwallEventInfo) {
    super()
  }
}

export class PaywallWillOpenDeepLinkEvent extends TestDelegateEvent {
  readonly type = 'paywallWillOpenDeepLink'
  
  constructor(public readonly url: URL) {
    super()
  }
}

export class PaywallWillOpenURLEvent extends TestDelegateEvent {
  readonly type = 'paywallWillOpenURL'
  
  constructor(public readonly url: URL) {
    super()
  }
}

export class SubscriptionStatusDidChangeEvent extends TestDelegateEvent {
  readonly type = 'subscriptionStatusDidChange'
  
  constructor(public readonly newValue: SubscriptionStatus) {
    super()
  }
}

export class WillDismissPaywallEvent extends TestDelegateEvent {
  readonly type = 'willDismissPaywall'
  
  constructor(public readonly paywallInfo: PaywallInfo) {
    super()
  }
}

export class WillPresentPaywallEvent extends TestDelegateEvent {
  readonly type = 'willPresentPaywall'
  
  constructor(public readonly paywallInfo: PaywallInfo) {
    super()
  }
}
