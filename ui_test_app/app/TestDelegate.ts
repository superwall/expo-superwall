import * as Superwall from "expo-superwall/compat"
import {
  TestDelegateEvent,
  SuperwallDelegate,
  PaywallInfo,
  PaywallResult,
  PaywallSkippedReason,
  SubscriptionStatus,
  SuperwallEventInfo,
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
}  from "expo-superwall"

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