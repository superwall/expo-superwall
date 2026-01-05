# Testing Your Purchase Flow

This guide covers how to test in-app purchases with expo-superwall using sandbox environments before releasing to production. Proper testing ensures your paywall integration works correctly and users can successfully complete purchases.

## Overview

Testing purchases requires:
1. **Sandbox/Test Environment**: Use Apple's StoreKit sandbox (iOS) or Google Play's test tracks (Android)
2. **Test Products**: Products configured in App Store Connect or Google Play Console that match your Superwall campaign
3. **Custom Purchase Controller**: Handle purchase events from Superwall and process them through the native store APIs

> **Important**: Never test with real payments. Always use sandbox accounts (iOS) or test tracks with license testers (Android).

---

## iOS Sandbox Testing

### Setting Up StoreKit Configuration (Local Testing)

For rapid local testing without needing App Store Connect, create a StoreKit Configuration file:

1. In Xcode, select **File > New > File**
2. Choose **StoreKit Configuration File**
3. Name it (e.g., `Products.storekit`)
4. Add products matching your Superwall campaign:
   - Click **+** and select subscription or product type
   - Set the **Product ID** to match your App Store Connect product ID
   - Configure price, duration, and other details

5. Enable the configuration in your scheme:
   - **Product > Scheme > Edit Scheme**
   - Select **Run > Options**
   - Set **StoreKit Configuration** to your `.storekit` file

This allows testing purchases in the iOS Simulator without sandbox accounts.

### Sandbox Accounts (Device Testing)

For testing on physical devices, create sandbox tester accounts:

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Navigate to **Users and Access > Sandbox > Testers**
3. Click **+** to add a new sandbox tester
4. Use a unique email (can be fake, e.g., `tester1@example.com`)
5. Set a password and complete the form

On your test device:

1. Sign out of your personal Apple ID in **Settings > App Store**
2. Open your app and initiate a purchase
3. When prompted, sign in with the sandbox account credentials
4. The device will recognize it as a sandbox account

**Best Practices**:
- Create multiple sandbox accounts for different test scenarios
- Sandbox subscriptions renew faster (monthly = 5 minutes, yearly = 1 hour)
- Use **Settings > App Store > Sandbox Account** (iOS 14+) to manage sandbox sessions

### TestFlight Testing

TestFlight builds use the sandbox environment for purchases:

1. Upload your build to App Store Connect
2. Add internal or external testers
3. Testers install via TestFlight and make purchases using sandbox accounts
4. All purchases are free and use accelerated renewal times

---

## Android Testing

### Google Play Console Setup

#### 1. Create Your App and Products

1. Go to [Google Play Console](https://play.google.com/console)
2. Create your app or select an existing one
3. Navigate to **Monetize > Products > Subscriptions** (or In-app products)
4. Create products matching your Superwall campaign product IDs

#### 2. Configure License Testers

License testers can make test purchases without being charged:

1. Go to **Settings > License testing**
2. Add email addresses of your testers (must be Google accounts)
3. Set **License response** to "RESPOND_NORMALLY" for realistic testing

#### 3. Set Up a Test Track

1. Go to **Release > Testing > Internal testing** (or Closed/Open testing)
2. Create a new release and upload your APK/AAB
3. Add testers by email or create a Google Group
4. Share the opt-in URL with testers

### Testing on Device

1. Install your app from the test track (not via `adb` or direct install)
2. Ensure you're signed into the Play Store with a license tester account
3. When making a purchase, you'll see **"Test card"** payment options:
   - **Test card, always approves** - Simulates successful purchase
   - **Test card, always declines** - Simulates failed payment
   - **Test card, slow** - Tests timeout handling

**Important**: The app must be installed from the Play Store test track for test instruments to appear.

---

## SDK Integration for Testing

Use `CustomPurchaseControllerProvider` to handle purchase events from Superwall:

```tsx
import {
  CustomPurchaseControllerProvider,
  SuperwallProvider,
  SuperwallLoaded,
  SuperwallLoading,
  usePlacement,
  useUser,
} from "expo-superwall";
import { ActivityIndicator, Button, Text, View } from "react-native";

// Your Superwall API keys
const API_KEYS = {
  ios: "pk_your_ios_key",
  android: "pk_your_android_key",
};

function App() {
  return (
    <CustomPurchaseControllerProvider
      controller={{
        onPurchase: async (params) => {
          console.log("Purchase requested:", params);

          // Platform-specific handling
          if (params.platform === "ios") {
            console.log("iOS Product ID:", params.productId);
            // StoreKit handles the purchase natively
            // The Superwall native SDK processes the transaction
          } else {
            console.log("Android Product ID:", params.productId);
            console.log("Base Plan ID:", params.basePlanId);
            console.log("Offer ID:", params.offerId);
            // Play Billing handles the purchase natively
          }

          // Return the purchase result
          // The native SDK handles the actual purchase flow
          // Return 'purchased' for success, 'failed' with error for failures
          return { type: "purchased" };

          // For failures:
          // return { type: "failed", error: "Payment declined" };

          // For cancellation:
          // return { type: "cancelled" };

          // For pending (e.g., awaiting payment):
          // return { type: "pending" };
        },

        onPurchaseRestore: async () => {
          console.log("Restore purchases requested");

          // Native SDK handles restore
          // Return result based on whether purchases were found
          return { type: "restored" };

          // If no purchases found:
          // return { type: "failed", error: "No purchases to restore" };
        },
      }}
    >
      <SuperwallProvider apiKeys={API_KEYS}>
        <SuperwallLoading>
          <ActivityIndicator style={{ flex: 1 }} />
        </SuperwallLoading>
        <SuperwallLoaded>
          <MainScreen />
        </SuperwallLoaded>
      </SuperwallProvider>
    </CustomPurchaseControllerProvider>
  );
}

function MainScreen() {
  const { subscriptionStatus } = useUser();
  const { registerPlacement } = usePlacement({
    onPresent: (info) => console.log("Paywall presented:", info.name),
    onDismiss: (info, result) => console.log("Paywall dismissed:", result),
    onError: (error) => console.error("Paywall error:", error),
  });

  return (
    <View style={{ flex: 1, padding: 20, gap: 16 }}>
      <Text>Status: {subscriptionStatus?.status ?? "UNKNOWN"}</Text>
      <Button
        title="Show Paywall"
        onPress={() =>
          registerPlacement({
            placement: "your_placement_id",
            feature: () => {
              console.log("Feature unlocked!");
            },
          })
        }
      />
    </View>
  );
}

export default App;
```

---

## Verifying Your Integration

### Check Subscription Status

Use the `useUser` hook to monitor subscription status:

```tsx
import { useUser } from "expo-superwall";

function SubscriptionStatus() {
  const { subscriptionStatus } = useUser();

  return (
    <View>
      <Text>Status: {subscriptionStatus?.status}</Text>
      {subscriptionStatus?.status === "ACTIVE" && (
        <Text>
          Entitlements:{" "}
          {subscriptionStatus.entitlements?.map((e) => e.id).join(", ")}
        </Text>
      )}
    </View>
  );
}
```

### Monitor Events with useSuperwallEvents

For detailed debugging, subscribe to transaction events:

```tsx
import { useSuperwallEvents } from "expo-superwall";

function EventLogger() {
  useSuperwallEvents({
    onSuperwallEvent: (eventInfo) => {
      const { event } = eventInfo;

      switch (event.event) {
        case "transactionStart":
          console.log("Transaction started:", event.product);
          break;
        case "transactionComplete":
          console.log("Transaction complete:", event.transaction);
          break;
        case "transactionFail":
          console.log("Transaction failed:", event.message);
          break;
        case "transactionAbandon":
          console.log("Transaction abandoned");
          break;
        case "subscriptionStart":
          console.log("Subscription started:", event.product);
          break;
      }
    },
    onSubscriptionStatusChange: (status) => {
      console.log("Subscription status changed:", status);
    },
  });

  return null;
}
```

### Test Different Scenarios

Create a test screen to verify various purchase outcomes:

```tsx
function TestControls() {
  const { setSubscriptionStatus, subscriptionStatus } = useUser();

  return (
    <View style={{ gap: 12 }}>
      <Text>Current: {subscriptionStatus?.status}</Text>

      <Button
        title="Set ACTIVE"
        onPress={() =>
          setSubscriptionStatus({
            status: "ACTIVE",
            entitlements: [{ id: "pro", type: "SERVICE_LEVEL" }],
          })
        }
      />

      <Button
        title="Set INACTIVE"
        onPress={() => setSubscriptionStatus({ status: "INACTIVE" })}
      />

      <Button
        title="Set UNKNOWN"
        onPress={() => setSubscriptionStatus({ status: "UNKNOWN" })}
      />
    </View>
  );
}
```

---

## Common Issues and Troubleshooting

### iOS Issues

**"Cannot connect to iTunes Store" in Simulator**
- Ensure StoreKit Configuration file is enabled in scheme
- Verify product IDs match exactly
- Reset the simulator and try again

**Sandbox account not working**
- Sign out completely from Settings > App Store
- Delete and reinstall the app
- Verify the sandbox account is properly created in App Store Connect

**Purchases not completing**
- Check Xcode console for StoreKit errors
- Verify your app's bundle ID matches App Store Connect
- Ensure products are "Ready to Submit" or "Approved" status

### Android Issues

**"Item not available" or "Item already owned"**
- Ensure app is installed from Play Store test track, not via adb
- Verify you're signed in with a license tester account
- Clear Play Store cache and data
- Consume test purchases in Play Console under **Order management**

**Test card options not appearing**
- App must be installed from a test track
- User must be a configured license tester
- Wait a few minutes after adding license testers

**Purchase stuck in pending state**
- Check if the purchase requires 3D Secure or additional verification
- Review Google Play Console for pending transactions

### General Issues

**Subscription status not updating after purchase**
- Verify `onPurchase` returns `{ type: "purchased" }`
- Check that the purchase controller is properly configured
- Review console logs for errors in the purchase flow

**Paywall not appearing**
- Verify placement ID matches Superwall dashboard
- Check that campaign targeting includes your test user
- Use `getPresentationResult` to debug why paywall was skipped

---

## Testing Checklist

Before releasing to production, verify:

- [ ] Fresh install shows paywall for non-subscribers
- [ ] Purchase flow completes successfully
- [ ] Subscription status updates to ACTIVE after purchase
- [ ] Feature gating works (subscriber bypasses paywall)
- [ ] Restore purchases works for previous subscribers
- [ ] Cancel/decline properly dismisses paywall
- [ ] Error states are handled gracefully
- [ ] Paywall appears correctly on both iOS and Android

---

## Resources

- [Apple StoreKit Testing](https://developer.apple.com/documentation/storekit/in-app_purchase/testing_in-app_purchases_with_sandbox)
- [Google Play Billing Testing](https://developer.android.com/google/play/billing/test)
- [Superwall Documentation](https://superwall.com/docs/home)
- [Example App](./example)
