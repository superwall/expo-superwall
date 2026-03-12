# SDK build specification

This document defines the contract required to rebuild the underlying
Superwall-backed SDK exposed by this repository. It focuses on the native iOS
and Android behavior, the shared Expo/native-module bridge, and the backend
APIs the SDK depends on. It does not describe product strategy or app-level
integration guidance.

This is a target specification, not a pure snapshot of the current wrapper.
Where the current implementation is inconsistent, incomplete, or explicitly
flagged by TODOs, this document defines the behavior the rebuilt SDK must
implement and calls out current-state differences separately.

## Sources of truth

This specification is derived from the current repository and the backend
repository at `Documents/superwall/paywall-next`. The most important source
files are:

- `ios/SuperwallExpoModule.swift`
- `ios/Bridges/PurchaseControllerBridge.swift`
- `ios/Bridges/SuperwallDelegateBridge.swift`
- `android/src/main/java/expo/modules/superwallexpo/SuperwallExpoModule.kt`
- `android/src/main/java/expo/modules/superwallexpo/bridges/PurchaseControllerBridge.kt`
- `android/src/main/java/expo/modules/superwallexpo/bridges/SuperwallDelegateBridge.kt`
- `src/SuperwallExpoModule.ts`
- `src/SuperwallExpoModule.types.ts`
- `src/useSuperwall.ts`
- `src/useSuperwallEvents.ts`
- `src/usePlacement.ts`
- `src/compat/index.ts`
- `ui_test_app/app/*`
- `paywall-next/packages/subscriptions/src/api/public-schema/groups/*`
- `paywall-next/packages/config/src/config-api-schema/groups/*`
- `paywall-next/packages/config/src/config-api-impl/groups/*`
- `paywall-next/apps/web/fapi/*`

## Scope and non-goals

This specification covers the underlying SDK contract. That includes methods,
events, result types, state transitions, purchase-controller behavior, and the
backend calls needed to configure, resolve entitlements, and support checkout
and redemption flows.

This specification does not define dashboard authoring, paywall rendering
internals, or the full web-paywall runtime. It also does not treat the hooks
layer as primary. The hooks layer is documented later as a thin wrapper over
the core contract.

## Architecture

The rebuilt SDK must preserve the current split of responsibilities.

- The native layer owns direct interaction with `SuperwallKit` on iOS and
  `com.superwall.sdk` on Android.
- The shared JS layer owns argument normalization, event subscription,
  handler scoping, and compatibility wrappers.
- The purchase controller bridge owns the manual purchase round-trip from
  native to JS and back to native.
- The delegate bridge owns native lifecycle and analytics events emitted into
  JS.
- The backend contract provides config data, entitlements, products, checkout,
  and redemption data required by native and web-backed purchase flows.

The rebuilt SDK must expose one coherent cross-platform contract with clearly
marked platform-specific behavior where symmetry is not possible.

## Lifecycle and state model

The SDK must implement these lifecycle states:

- `unconfigured`: no native configuration has been completed.
- `configuring`: `configure()` has started and has not yet resolved.
- `configured`: configuration completed successfully and delegate bridging is
  active.
- `configFailed`: configuration failed or the SDK emitted a config failure
  event.
- `identityKnown`: an app user has been identified.
- `identityReset`: the SDK has cleared user-linked identity state.

The rebuilt SDK must enforce these lifecycle rules:

- `configure()` must be the first meaningful method call.
- `configure()` must install the delegate bridge before resolving success.
- `identify()`, `reset()`, `registerPlacement()`, `getAssignments()`,
  `confirmAllAssignments()`, `getEntitlements()`, `getSubscriptionStatus()`,
  `setSubscriptionStatus()`, `getUserAttributes()`, `setUserAttributes()`,
  `preloadPaywalls()`, `preloadAllPaywalls()`, `setIntegrationAttributes()`,
  and `getIntegrationAttributes()` must be treated as post-configuration
  methods.
- `dismiss()`, `setLogLevel()`, and `setInterfaceStyle()` may be treated as
  safe no-op or permissive calls if configuration is already in progress, but
  they must not crash.
- `dismiss()` must be idempotent when no paywall is active.

## Public SDK contract

This section defines the normative method contract.

### `configure(apiKey, options?, usingPurchaseController?, sdkVersion?)`

This method must initialize the native SDK using the platform-specific public
API key and the supplied options.

Inputs:

- `apiKey: string`
- `options?: SuperwallOptions`
- `usingPurchaseController?: boolean`
- `sdkVersion?: string`

Required behavior:

- Parse and normalize options before passing them to native.
- If `usingPurchaseController` is `true`, attach the purchase controller
  bridge. If it is `false`, native must manage purchases itself.
- Install the delegate bridge before resolving.
- Set the platform wrapper to `Expo` and pass the wrapper version string if
  provided.
- On Android, wire `onBackPressed` routing through the paywall options.

Failure behavior:

- Reject on native configuration failure.
- Emit or preserve downstream config-failure signaling through
  `handleSuperwallEvent`.

### `getConfigurationStatus()`

This method must return the native configuration status as a stable string
representation. The current code exposes at least configured, pending, and
failed states through wrapper types.

### `identify(userId, options?)`

This method must identify the current app user.

Inputs:

- `userId: string`
- `options?: { restorePaywallAssignments?: boolean }`

Required behavior:

- Forward `restorePaywallAssignments` if provided.
- Preserve configuration state.
- Update downstream user and subscription views after completion.

### `reset()`

This method must clear the current identity and user-linked SDK state. It must
reset the current user, identity aliasing state, and local assignment context.

### `registerPlacement(placement, params?, handlerId?)`

This method must register a placement and allow native to decide whether a
paywall presents, a paywall is skipped, or the feature path continues.

Inputs:

- `placement: string`
- `params?: Record<string, any>`
- `handlerId?: string | null`

Required behavior:

- Forward `params` as a JSON-compatible object.
- If `handlerId` exists, create a presentation handler and scope the
  paywall-related events to that handler.
- Emit `onPaywallPresent`, `onPaywallDismiss`, `onPaywallError`,
  `onPaywallSkip`, and `onCustomCallback` with the originating `handlerId`.

Target behavior:

- Feature execution must be decided by native paywall outcome, not by blindly
  resolving the wrapper promise. The current compat layer has a TODO because it
  executes the feature block too early.

### `getPresentationResult(placement, params?)`

This method must evaluate the placement without presenting a paywall. It must
return a presentation result object that tells the caller what native would do
for that placement and parameter set.

### `dismiss()`

This method must dismiss an active paywall if one is visible and must resolve
without error if none is visible.

### `confirmAllAssignments()`

This method must confirm all pending assignments and return the confirmed
assignment list.

### `getAssignments()`

This method must return the user’s known experiment assignments.

### `getEntitlements()`

This method must return canonical entitlement state using one schema across
platforms.

Canonical shape:

```ts
type EntitlementsInfo = {
  active: Entitlement[]
  inactive: Entitlement[]
  all?: Entitlement[]
}
```

Target rule:

- `active` and `inactive` are required.
- `all` may be returned for backend parity, but consumers must not rely on it
  unless the public contract explicitly exposes it.

### `getSubscriptionStatus()`

This method must return one of:

- `{ status: "UNKNOWN" }`
- `{ status: "INACTIVE" }`
- `{ status: "ACTIVE", entitlements: Entitlement[] }`

### `setSubscriptionStatus(status)`

This method must let the app update subscription state when manual purchase
management is in use.

Required behavior:

- Accept the same status union returned by `getSubscriptionStatus()`.
- Unknown values must coerce safely to `UNKNOWN`.
- `ACTIVE` without a valid entitlement list must not crash.

### `setInterfaceStyle(style?)`

This method must override the native interface style if a supported value is
provided. `undefined` or `null` must revert to system default behavior.

### `getUserAttributes()` and `setUserAttributes(userAttributes)`

These methods must expose the SDK’s custom user attributes.

Required behavior:

- Values must be JSON-compatible.
- Keys reserved by Superwall may be dropped by native or backend.
- `undefined` values must be filtered before passing to native.

### `getDeviceAttributes()`

This method must return the device attribute map resolved by native.

### `handleDeepLink(url)`

This method must attempt native deep-link handling and return `boolean`.

Required behavior:

- Invalid URL input must return `false`.
- Native deep-link errors must not crash the caller.

### `preloadPaywalls(placementNames)` and `preloadAllPaywalls()`

These methods must start paywall preloading. They do not need to await the
entire preload lifecycle, but they must not silently drop the request.

### `setLogLevel(level)`

This method must set native log level for supported values:

- `debug`
- `info`
- `warn`
- `error`
- `none`

Unknown values must be ignored, not rejected.

### `setIntegrationAttributes(attributes)` and `getIntegrationAttributes()`

These methods must round-trip supported integration IDs. Unknown keys must be
ignored rather than causing the whole call to fail.

Supported keys currently include:

- `adjustId`
- `amplitudeDeviceId`
- `amplitudeUserId`
- `appsflyerId`
- `brazeAliasName`
- `brazeAliasLabel`
- `onesignalId`
- `fbAnonId`
- `firebaseAppInstanceId`
- `iterableUserId`
- `iterableCampaignId`
- `iterableTemplateId`
- `mixpanelDistinctId`
- `mparticleId`
- `clevertapId`
- `airshipChannelId`
- `kochavaDeviceId`
- `tenjinId`
- `posthogUserId`
- `customerioId`
- `appstackId`

### Purchase-controller completion methods

These methods complete native work started by `onPurchase`,
`onPurchaseRestore`, `onCustomCallback`, and `onBackPressed`.

- `didPurchase(result)`
- `didRestore(result)`
- `didHandleCustomCallback(callbackId, status, data?)`
- `didHandleBackPressed(shouldConsume)` on Android only

Required behavior:

- Unknown `callbackId` must resolve as a safe no-op.
- Purchase and restore completion must resolve the currently pending native
  continuation or future.
- Android back-press completion must resolve the waiting promise.

## Event contract

This section defines all events emitted by the native module.

### Handler-scoped paywall events

These events must include `handlerId` when they come from a scoped placement
handler.

| Event | Payload | Notes |
| --- | --- | --- |
| `onPaywallPresent` | `{ paywallInfoJson, handlerId }` | Fired when a paywall presents |
| `onPaywallDismiss` | `{ paywallInfoJson, result, handlerId }` | Fired on dismissal |
| `onPaywallError` | `{ errorString, handlerId }` | Fired on handler-level error |
| `onPaywallSkip` | `{ skippedReason, handlerId }` | Fired when placement does not present |
| `onCustomCallback` | `{ callbackId, name, variables?, handlerId }` | Fired for paywall callback round-trip |

Rules:

- Scoped listeners must ignore events whose `handlerId` does not match.
- Unscoped listeners must not complete custom callbacks intended for scoped
  handlers.

### Purchase and restore events

| Event | Payload | Notes |
| --- | --- | --- |
| `onPurchase` | iOS: `{ productId, platform: "ios" }` | Manual purchase flow |
| `onPurchase` | Android: `{ productId, platform: "android", basePlanId, offerId? }` | Manual purchase flow |
| `onPurchaseRestore` | no payload | Manual restore flow |

Rules:

- iOS purchase payload does not include base plan or offer IDs.
- Android purchase payload must include `basePlanId` and may include `offerId`.

### Android-only back button event

`onBackPressed` must only exist on Android and only when back-button rerouting
is enabled in paywall options.

Payload:

```ts
{ paywallInfo: PaywallInfo }
```

Rules:

- Native must wait for `didHandleBackPressed`.
- Default JS behavior is `false` if no handler responds.

### Delegate bridge events

These events must remain globally observable:

- `subscriptionStatusDidChange`
- `handleSuperwallEvent`
- `handleCustomPaywallAction`
- `willDismissPaywall`
- `willPresentPaywall`
- `didDismissPaywall`
- `didPresentPaywall`
- `paywallWillOpenURL`
- `paywallWillOpenDeepLink`
- `handleLog`
- `willRedeemLink`
- `didRedeemLink`

Payload rules:

- `subscriptionStatusDidChange` must provide `{ from, to }`.
- `handleSuperwallEvent` must provide `{ eventInfo }`.
- paywall lifecycle events must provide `{ info: PaywallInfo }`.
- `handleLog` must provide `level`, `scope`, `message`, `info`, and `error`.
- `didRedeemLink` must provide a single `RedemptionResult`.

## Core data models

This section captures the minimum model contract the rebuilt SDK must preserve.

### Experiments and entitlements

```ts
type VariantType = "TREATMENT" | "HOLDOUT"

type Variant = {
  id: string
  type: VariantType
  paywallId: string
}

type Experiment = {
  id: string
  groupId: string
  variant: Variant
}

type Entitlement = {
  id: string
  type: "SERVICE_LEVEL"
}
```

### Paywall result and skip reason

```ts
type PaywallSkippedReason =
  | { type: "Holdout"; experiment: Experiment }
  | { type: "NoAudienceMatch" }
  | { type: "PlacementNotFound" }

type PaywallResult =
  | { type: "purchased"; productId: string }
  | { type: "declined" }
  | { type: "restored" }
```

### Paywall info

`PaywallInfo` must include at least:

- identity fields: `identifier`, `name`, `url`
- experiment context: `experiment`
- product context: `products`, `productIds`
- presentation source:
  `presentedByEventWithName`, `presentedByEventWithId`, `presentedByEventAt`,
  `presentedBy`, `presentationSourceType`
- timing fields for response, web view, and product loading
- `isFreeTrialAvailable`
- `featureGatingBehavior`
- `closeReason`
- `computedPropertyRequests`
- `surveys`
- `localNotifications`
- `state`

### Custom callback

```ts
type CustomCallback = {
  name: string
  variables?: Record<string, any>
}

type CustomCallbackResult = {
  status: "success" | "failure"
  data?: Record<string, any>
}
```

### Redemption result

The rebuilt SDK must preserve the current union:

- `SUCCESS`
- `ERROR`
- `CODE_EXPIRED`
- `INVALID_CODE`
- `EXPIRED_SUBSCRIPTION`

Each successful or expired-subscription result must include redemption info
with ownership, purchaser info, optional paywall info, and entitlements.

### Trigger result

```ts
type TriggerResult =
  | { result: "placementNotFound" }
  | { result: "noAudienceMatch" }
  | { result: "paywall"; experiment: Experiment }
  | { result: "holdout"; experiment: Experiment }
  | { result: "error"; error: string }
```

### Paywall presentation request event

The rebuilt SDK must preserve the reason model currently encoded by
`PaywallPresentationRequestStatus` and
`PaywallPresentationRequestStatusReason`.

Known status values:

- `presentation`
- `noPresentation`
- `timeout`

Known no-presentation reasons:

- `debuggerPresented`
- `paywallAlreadyPresented`
- `holdout`
- `noRuleMatch`
- `eventNotFound`
- `noPaywallViewController`
- `noViewController`
- `userIsSubscribed`
- `error`
- `paywallIsGated`

## Internal analytics event surface

`handleSuperwallEvent` delivers a `SuperwallEventInfo` payload that wraps a
large discriminated union. The rebuilt SDK must preserve the event names below
even if some payload details are thinly typed in JS today.

Core lifecycle and identity:

- `firstSeen`
- `appOpen`
- `appLaunch`
- `identityAlias`
- `appInstall`
- `sessionStart`
- `reset`
- `configRefresh`
- `configFail`
- `configAttributes`
- `confirmAllAssignments`
- `deviceAttributes`
- `subscriptionStatusDidChange`
- `appClose`
- `deepLink`
- `userAttributes`
- `integrationAttributes`

Restoration, redemption, and enrichment:

- `restoreStart`
- `restoreComplete`
- `restoreFail`
- `redemptionStart`
- `redemptionComplete`
- `redemptionFail`
- `enrichmentStart`
- `enrichmentComplete`
- `enrichmentFail`
- `willRedeemLink`
- `didRedeemLink`

Trigger and paywall events:

- `triggerFire`
- `paywallOpen`
- `paywallClose`
- `paywallDecline`
- `paywallPresentationRequest`
- `customPlacement`
- `paywallResponseLoadStart`
- `paywallResponseLoadNotFound`
- `paywallResponseLoadFail`
- `paywallResponseLoadComplete`
- `paywallWebviewLoadStart`
- `paywallWebviewLoadFail`
- `paywallWebviewLoadComplete`
- `paywallWebviewLoadTimeout`
- `paywallWebviewLoadFallback`
- `paywallWebviewProcessTerminated`
- `paywallProductsLoadStart`
- `paywallProductsLoadFail`
- `paywallProductsLoadComplete`
- `paywallProductsLoadRetry`
- `paywallProductsLoadMissingProducts`
- `paywallPreloadStart`
- `paywallPreloadComplete`

Transactions and purchases:

- `transactionStart`
- `transactionFail`
- `transactionAbandon`
- `transactionComplete`
- `subscriptionStart`
- `freeTrialStart`
- `transactionRestore`
- `transactionTimeout`
- `nonRecurringProductPurchase`
- `stripeCheckoutStart`
- `stripeCheckoutSubmit`
- `stripeCheckoutComplete`
- `stripeCheckoutFail`

Other current events:

- `touchesBegan`
- `surveyClose`
- `surveyResponse`
- `adServicesTokenRequestStart`
- `adServicesTokenRequestFail`
- `adServicesTokenRequestComplete`
- `shimmerViewStart`
- `shimmerViewComplete`
- `networkDecodingFail`
- `customerInfoDidChange`
- `reviewRequested`
- `permissionRequested`
- `permissionGranted`
- `permissionDenied`
- `testModeModalOpen`
- `testModeModalClose`
- `unknown`

## Backend API contract

This section defines the backend calls the rebuilt SDK must understand. Paths
are marked as verified or inferred.

### Authentication

The verified public subscriptions API uses bearer auth with the public API key.

Header:

```http
Authorization: Bearer <public_api_key>
```

This is verified by
`paywall-next/packages/subscriptions/src/api/public-schema/authn/PublicApiKeyAuthn.ts`.

### Public subscriptions API base path

Verified prefix:

```text
/subscriptions-api/public
```

All verified endpoint paths below sit under that prefix. For example, the full
checkout session path is:

```text
/subscriptions-api/public/v1/checkout/session
```

### Checkout session API

This API is verified by
`paywall-next/packages/subscriptions/src/api/public-schema/groups/checkout-public.ts`.

#### `POST /subscriptions-api/public/v1/checkout/session`

This endpoint initiates a checkout session and returns a checkout directive and
a sealed checkout-context identifier.

Request body:

```json
{
  "currencyCode": "USD",
  "productIdentifier": "com.example.premium.monthly",
  "store": "stripe",
  "allowedCheckoutDirectives": ["redirect", "drawer", "embedded"],
  "context": {
    "paywall": {
      "paywallId": "456",
      "paywallIdentifier": "test-paywall",
      "paywallName": "Test Paywall",
      "paywallUrl": "https://example.com/paywall",
      "paywallProductIds": "com.example.premium.monthly"
    },
    "experiment": {
      "experimentId": "123",
      "variantId": "456"
    },
    "presentment": {
      "isFreeTrialAvailable": true,
      "presentationSourceType": "register",
      "presentedBy": "placement",
      "presentedByEventId": "evt_123",
      "presentedByEventName": "campaign_trigger"
    },
    "placementParams": {
      "placementParams": {}
    },
    "identity": {
      "appUserId": "user_123",
      "aliasId": null,
      "deviceId": "$SuperwallDevice:uuid",
      "email": "user@example.com"
    },
    "device": {
      "publicApiKey": "pk_test_123",
      "platform": "ios",
      "appVersion": "1.0.0",
      "osVersion": "17.4",
      "deviceModel": "iPhone 15",
      "deviceLocale": "en_US",
      "deviceLanguageCode": "en",
      "deviceCurrencyCode": "USD",
      "deviceCurrencySymbol": "$",
      "timezoneOffset": "0"
    },
    "product": {
      "identifier": "com.example.premium.monthly",
      "currencyCode": "USD",
      "currencySymbol": "$",
      "dailyPrice": "0.33",
      "weeklyPrice": "2.31",
      "monthlyPrice": "9.99",
      "yearlyPrice": "119.88",
      "languageCode": "en",
      "locale": "en_US",
      "localizedPeriod": "per month",
      "period": "month",
      "periodAlt": "mo",
      "periodDays": 30,
      "periodMonths": 1,
      "periodWeeks": 4,
      "periodYears": 0.0833,
      "periodly": "monthly",
      "price": "9.99",
      "rawPrice": 999,
      "rawTrialPeriodPrice": 0,
      "trialPeriodDailyPrice": "0.00",
      "trialPeriodDays": 7,
      "trialPeriodWeeks": 1,
      "trialPeriodMonths": 0,
      "trialPeriodEndDate": "2026-03-13T00:00:00.000Z",
      "trialPeriodMonthlyPrice": "0.00",
      "trialPeriodPrice": "0.00",
      "trialPeriodText": "7-day free trial",
      "trialPeriodWeeklyPrice": "0.00",
      "trialPeriodYearlyPrice": "0.00",
      "trialPeriodYears": 0
    }
  },
  "stripeCustomerId": null
}
```

Success response:

```json
{
  "directive": {
    "directive": "redirect",
    "checkoutUrl": "https://checkout.example.com/...",
    "checkoutSessionId": "cs_123"
  },
  "checkoutContextId": "v1:keyId:encryptedPayload"
}
```

Directive variants:

- `redirect`
- `drawer`
- `embedded`

The `drawer` variant may additionally include:

- `publishableKey`
- `checkoutSessionId`
- `subscriptionDetails`
- `paymentMethodTypes`
- `intentType`
- optional application details like `iconUrl`

The `embedded` variant includes:

- `publishableKey`
- `checkoutSessionId`

Errors:

- invalid product format
- missing store application id
- missing web paywall domain
- checkout-context encoding, storage, or sealing failures
- unexpected checkout directive
- internal server error

#### `POST /subscriptions-api/public/v1/checkout/status`

This endpoint polls the session state.

Request body:

```json
{
  "checkoutId": "v1:keyId:encryptedPayload"
}
```

Success response:

```json
{
  "status": "pending",
  "paymentStatus": "requires_action",
  "redemptionUrl": "https://..."
}
```

Allowed `status` values:

- `pending`
- `completed`
- `abandoned`

#### `POST /subscriptions-api/public/v1/checkout/session/confirm`

This endpoint confirms a checkout session after payment setup.

Request body:

```json
{
  "setupIntentId": "seti_123",
  "checkoutContextId": "v1:keyId:encryptedPayload",
  "email": "user@example.com"
}
```

Success response:

```json
{
  "customerId": "cus_123",
  "subscriptionId": "sub_123",
  "redemptionUrl": "https://..."
}
```

#### `POST /subscriptions-api/public/v1/checkout/session/complete`

This endpoint marks checkout completion.

Request body:

```json
{
  "checkoutContextId": "v1:keyId:encryptedPayload"
}
```

Success response:

```json
{
  "redemptionUrl": "https://..."
}
```

#### `POST /subscriptions-api/public/v1/checkout/session/poll-redemption-result`

This endpoint polls the redemption state after checkout completion.

Request body:

```json
{
  "checkoutContextId": "v1:keyId:encryptedPayload",
  "deviceId": "$SuperwallDevice:uuid",
  "appUserId": "user_123"
}
```

Success response:

```json
{
  "status": "complete",
  "codes": [],
  "entitlements": [],
  "customerInfo": {
    "subscriptions": [],
    "nonSubscriptions": [],
    "entitlements": []
  }
}
```

Allowed poll status values:

- `pending`
- `failed`
- `complete`

### Redemption API

This API is verified by
`paywall-next/packages/subscriptions/src/api/public-schema/groups/redemption-public.ts`.

#### `POST /subscriptions-api/public/v1/redeem`

This endpoint redeems one or more codes and returns entitlement state.

Request body:

```json
{
  "deviceId": "$SuperwallDevice:uuid",
  "aliasId": "$SuperwallAlias:uuid",
  "appUserId": "user_123",
  "appTransactionId": "txn_123",
  "externalAccountId": "external_123",
  "codes": [
    {
      "code": "PROMO-CODE",
      "firstRedemption": true
    }
  ],
  "receipts": {},
  "metadata": {}
}
```

Success response:

```json
{
  "codes": [
    {
      "status": "SUCCESS",
      "code": "PROMO-CODE",
      "redemptionInfo": {
        "ownership": {
          "type": "APP_USER",
          "appUserId": "user_123"
        },
        "purchaserInfo": {
          "appUserId": "user_123",
          "email": "user@example.com",
          "storeIdentifiers": {
            "store": "STRIPE",
            "stripeCustomerId": "cus_123",
            "stripeSubscriptionIds": ["sub_123"]
          }
        },
        "paywallInfo": null,
        "entitlements": []
      }
    }
  ],
  "entitlements": [],
  "customerInfo": {
    "subscriptions": [],
    "nonSubscriptions": [],
    "entitlements": []
  }
}
```

### Entitlements API

This API is verified by
`paywall-next/packages/subscriptions/src/api/public-schema/groups/entitlements-public.ts`.

#### `GET /subscriptions-api/public/v1/users/:appUserIdOrDeviceId/entitlements`

This endpoint returns entitlements and customer info for a user or device.

Path parameter:

- `appUserIdOrDeviceId`

Optional URL parameter:

- `deviceId`

Success response:

```json
{
  "entitlements": [
    {
      "identifier": "pro",
      "type": "SERVICE_LEVEL"
    }
  ],
  "customerInfo": {
    "subscriptions": [],
    "nonSubscriptions": [],
    "entitlements": []
  }
}
```

### Products API

This API is verified by
`paywall-next/packages/subscriptions/src/api/public-schema/groups/products-public.ts`.

#### `GET /subscriptions-api/public/v1/products/`

This endpoint returns products known to the backend.

Success response:

```json
{
  "object": "list",
  "data": [
    {
      "object": "product",
      "identifier": "com.example.premium.monthly",
      "platform": "ios",
      "price": {
        "amount": 9.99,
        "currency": "USD"
      },
      "subscription": {
        "period": "month",
        "period_count": 1,
        "trial_period_days": 7
      },
      "entitlements": [
        {
          "identifier": "pro",
          "type": "SERVICE_LEVEL"
        }
      ],
      "storefront": "USA"
    }
  ]
}
```

### Config API artifact contract

The config API uses artifact groups rather than ordinary REST resources. Two
path styles are visible in the backend repo:

- verified direct read paths used by internal artifact consumers
- verified public manifest paths used by at least the retention worker

The rebuilt SDK must treat the artifact namespace as the source of config
truth.

#### Verified namespace

```text
config-api
```

#### Verified group: `v1-application-public-api-key`

Resource:

- `application`

Verified data shape:

```json
{
  "applicationId": 1,
  "organizationId": 1,
  "projectId": 1,
  "stripeApplicationId": 9,
  "storeApplicationIds": {
    "stripe": 9,
    "paddle": null,
    "ios": 3,
    "android": 4,
    "promotional": null
  },
  "webPaywallDomain": "paywall.example.com",
  "applicationIcon": "https://..."
}
```

Verified direct-read path pattern from tests:

```text
/config-api/read/v1-application-public-api-key/{publicApiKey}/application/{responseVersion}
```

Public manifest path:

- exact runtime usage is not shown for this group
- the manifest form is therefore inferred as:

```text
/config-api/public/manifest/v1-application-public-api-key/{publicApiKey}/application
```

#### Verified group: `v1-web-paywall-domain`

Resources:

- `static-config`
- `application`

Verified direct-read path patterns from tests:

```text
/config-api/read/v1-web-paywall-domain/{domain}/static-config/{responseVersion}
/config-api/read/v1-web-paywall-domain/{domain}/application/{responseVersion}
```

The `application` resource returns:

```json
{
  "applicationId": 9,
  "storeApplicationIds": {
    "stripe": 9,
    "paddle": null,
    "ios": 3,
    "android": 4
  }
}
```

#### Verified group: `v1-application-id`

Resource relevant to SDK/backend parity:

- `entitlements`

Verified direct-read path pattern from tests:

```text
/config-api/read/v1-application-id/{applicationId}/entitlements/{responseVersion}
```

Verified response shape:

```json
{
  "products": [
    {
      "productId": "live:price_123:no-trial",
      "store": "stripe",
      "entitlements": [
        {
          "identifier": "pro_example",
          "type": "SERVICE_LEVEL"
        }
      ],
      "rawPrice": "9.99",
      "period": "month",
      "trialPeriodDays": 7
    }
  ]
}
```

#### Verified group: `v1-retention-messaging-config`

Resource:

- `config`

Verified public manifest path from the retention worker:

```text
/config-api/public/manifest/v1-retention-messaging-config/{publicApiKey}/config
```

Verified response shape:

```json
{
  "production": {
    "realtime": {},
    "defaultMessages": {}
  },
  "sandbox": {
    "realtime": {},
    "defaultMessages": {}
  },
  "signingKey": {
    "keyId": "key-id",
    "issuerId": "issuer-id",
    "privateKey": "pem-private-key",
    "bundleId": "com.example.app"
  }
}
```

### Legacy assignment APIs

These endpoints are visible in `paywall-next/apps/web/fapi` and align with the
assignment methods exposed by the current SDK wrappers. They must be documented
as compatibility endpoints unless the backend is upgraded to a newer public
contract.

#### `GET /api/v1/assignments`

Success response:

```json
{
  "assignments": [
    {
      "experiment_id": "123",
      "variant_id": "456"
    }
  ]
}
```

Behavior:

- returns an empty list if alias resolution fails
- triggers a `userAlias_potentiallyMerge` event as a side effect

#### `POST /api/v1/confirm_assignments`

Request body:

```json
{
  "assignments": [
    {
      "experiment_id": "123",
      "variant_id": "456"
    }
  ]
}
```

Success response:

```json
{
  "status": "ok",
  "assignments": [
    {
      "experiment_id": "123",
      "variant_id": "456"
    }
  ]
}
```

Rules:

- reject more than 100 assignments
- validate each experiment and variant against the current application
- support idempotent duplicate confirmation
- emit `assignment_confirm` side effects

## End-to-end flows

This section defines how the main flows must operate.

### Configuration flow

1. JS calls `configure()`.
2. Native configures the SDK with the public API key and options.
3. Native installs the purchase controller bridge if enabled.
4. Native installs the delegate bridge.
5. Native sets the platform wrapper metadata.
6. The call resolves.
7. Downstream code may fetch user and subscription state.

### Placement flow

1. JS calls `registerPlacement()`.
2. Native evaluates the placement.
3. Native either:
   - presents a paywall and emits present/dismiss events,
   - skips and emits `onPaywallSkip`, or
   - errors and emits `onPaywallError`.
4. If the paywall invokes a custom callback, native emits `onCustomCallback`
   and waits for completion.

### Manual purchase flow

1. Native requests purchase through the purchase controller bridge.
2. Native emits `onPurchase`.
3. JS performs the store-specific purchase.
4. JS calls `didPurchase(result)`.
5. Native resumes the pending purchase continuation.

### Restore flow

1. Native emits `onPurchaseRestore`.
2. JS performs restore.
3. JS calls `didRestore(result)`.
4. Native resumes the pending restore continuation.

### Android back-button flow

1. Native paywall intercepts back press.
2. Native emits `onBackPressed`.
3. JS decides whether to consume.
4. JS calls `didHandleBackPressed(shouldConsume)`.
5. Native either consumes the event or lets dismissal continue.

### Checkout flow

1. SDK gathers paywall, experiment, identity, device, placement, and product
   context.
2. SDK calls `POST /subscriptions-api/public/v1/checkout/session`.
3. Backend returns a checkout directive and sealed checkout-context ID.
4. SDK executes redirect, drawer, or embedded checkout.
5. SDK polls status or confirms completion using the sealed context ID.
6. SDK may poll redemption result to retrieve entitlements and customer info.

### Redemption flow

1. SDK calls `POST /subscriptions-api/public/v1/redeem`.
2. Backend returns per-code redemption results, entitlements, and customer
   info.
3. Native delegate and JS listeners propagate `willRedeemLink` and
   `didRedeemLink`.

## Platform differences

The rebuilt SDK must preserve these platform differences explicitly.

- Android supports `onBackPressed`. iOS does not.
- iOS purchase payload is `{ productId, platform: "ios" }`.
- Android purchase payload includes `basePlanId` and optional `offerId`.
- iOS and Android may serialize some internal native models differently, but
  the JS-facing contract must remain canonical.

## Current implementation deltas to normalize

The rebuilt SDK must normalize these current issues:

- Compat `register(..., feature)` currently runs the feature block too early.
- Hooks-side identity refresh relies on a `setTimeout(0)` workaround.
- Entitlements serialization differs by platform.
- Event payload naming is inconsistent between `paywallInfoJson` and
  `paywallInfo`.
- Assignment APIs live in an older `/api/v1/*` surface while newer backend
  functionality is schema-driven under `/subscriptions-api/public`.

## Edge cases and failure modes

The rebuilt SDK must handle these cases deterministically.

- Invalid deep-link URL: return `false`.
- Unknown custom-callback completion ID: resolve with no side effect.
- Unsupported log level: ignore.
- Unknown integration attribute key: ignore that key only.
- `ACTIVE` subscription status with malformed entitlements: coerce safely or
  reject without crashing.
- Missing paywall handler for scoped events: drop the event.
- Missing custom callback handler: return callback failure to native.
- Duplicate assignment confirmation: remain idempotent.
- Empty or missing checkout session state: surface a typed backend error.
- Missing config artifact: surface a typed not-found or config-read error.

## Hooks mapping

The hooks layer is a convenience wrapper over the core contract.

- `useSuperwall` wraps the shared store and exposes methods like
  `identify`, `registerPlacement`, `dismiss`, `getEntitlements`, and
  `setUserAttributes`.
- `useSuperwallEvents` binds module events to React callbacks and owns the
  default JS response path for custom callbacks and Android back presses.
- `usePlacement` creates a scoped `handlerId`, subscribes to scoped events, and
  exposes a local paywall state machine.

The hooks contract must not redefine native behavior. It must map onto the
core method and event rules defined above.

## Acceptance criteria

An implementation satisfies this specification only if all of the following are
true:

- Every method in the public SDK contract exists and follows the rules in this
  document.
- Every event listed in the event contract is emitted with the required
  payload shape.
- Manual purchase and restore round-trips work on both platforms.
- Android back-button rerouting works exactly as defined.
- Checkout, redemption, entitlements, and product APIs are implemented against
  the verified backend contract.
- Config retrieval uses the artifact-group model and distinguishes verified
  paths from inferred public-manifest paths.
- Legacy assignment endpoints are either supported for compatibility or
  explicitly replaced by an intentional migration plan.
- Current wrapper inconsistencies are resolved in favor of the target behavior
  defined in this document.

## Next steps

If you use this document as the implementation source, build the SDK in this
order:

1. Native method and event parity.
2. Purchase-controller and callback round-trips.
3. Backend client for config, checkout, redemption, entitlements, and products.
4. Hooks wrapper on top of the stabilized core contract.
