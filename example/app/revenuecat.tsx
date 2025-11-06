import {
  CustomPurchaseControllerProvider,
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useUser,
} from "expo-superwall"
import { useEffect, useState } from "react"
import { ActivityIndicator, Alert, Button, Platform, ScrollView, Text, View } from "react-native"
import Purchases from "react-native-purchases"

const API_KEY = "pk_a5959912ceb3087d55c7ea63001202335adfbb54e0f70475"

// RevenueCat configuration keys
const REVENUECAT_API_KEYS = {
  ios: "appl_YOUR_IOS_KEY_HERE",
  android: "goog_YOUR_ANDROID_KEY_HERE",
}

function ScreenContent() {
  const [purchaseStatus, setPurchaseStatus] = useState<string>("")
  const [rcConfigured, setRcConfigured] = useState<boolean>(false)

  const { identify, user, signOut, subscriptionStatus, setSubscriptionStatus } = useUser()

  // Configure RevenueCat on mount
  useEffect(() => {
    const configureRevenueCat = async () => {
      try {
        Purchases.setLogLevel(Purchases.LOG_LEVEL.DEBUG)
        const apiKey = Platform.OS === "ios" ? REVENUECAT_API_KEYS.ios : REVENUECAT_API_KEYS.android
        await Purchases.configure({ apiKey })
        setRcConfigured(true)
        console.log("✓ RevenueCat configured successfully")
      } catch (error) {
        console.error("Failed to configure RevenueCat:", error)
        Alert.alert("Configuration Error", "Failed to initialize RevenueCat. Check your API keys.")
      }
    }

    configureRevenueCat()
  }, [])

  const { registerPlacement } = usePlacement({
    onError: (err) => {
      console.error("Placement error:", err)
      Alert.alert("Error", err)
    },
    onPresent: (info) => {
      console.log("Paywall presented", info)
      setPurchaseStatus("Paywall presented - waiting for user action...")
    },
    onDismiss: (info, result) => {
      console.log("Paywall dismissed", info, result)
      if (result?.type === "purchased") {
        setPurchaseStatus("Purchase completed successfully! ✓")
        // Sync subscription status with RevenueCat
        syncSubscriptionStatus()
      } else if (result?.type === "declined") {
        setPurchaseStatus("Purchase was declined")
      }
    },
  })

  const syncSubscriptionStatus = async () => {
    try {
      const customerInfo = await Purchases.getCustomerInfo()
      const hasActiveSubscription = Object.keys(customerInfo.entitlements.active).length > 0

      if (hasActiveSubscription) {
        await setSubscriptionStatus({
          status: "ACTIVE",
          entitlements: Object.keys(customerInfo.entitlements.active).map((id) => ({
            id,
            type: "SERVICE_LEVEL",
          })),
        })
        console.log("✓ Subscription status synced: ACTIVE")
      } else {
        await setSubscriptionStatus({
          status: "INACTIVE",
        })
        console.log("✓ Subscription status synced: INACTIVE")
      }
    } catch (error) {
      console.error("Failed to sync subscription status:", error)
    }
  }

  const triggerPaywall = async () => {
    setPurchaseStatus("")
    await registerPlacement({
      placement: "upgrade_button",
      feature() {
        console.log("Feature unlocked!")
        Alert.alert("Feature Unlocked! 🎉", "Successfully accessed premium feature")
      },
    })
  }

  if (!rcConfigured) {
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 16 }}>
        <ActivityIndicator size="large" color="#667eea" />
        <Text style={{ color: "#666" }}>Configuring RevenueCat...</Text>
      </View>
    )
  }

  return (
    <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 20, gap: 20 }}>
      <View style={{ gap: 8 }}>
        <Text style={{ fontSize: 24, fontWeight: "bold" }}>RevenueCat Integration</Text>
        <Text style={{ fontSize: 14, color: "#666" }}>
          Complete example of integrating Superwall with RevenueCat for purchases and restores
        </Text>
      </View>

      {/* User Identity Section */}
      <View
        style={{
          backgroundColor: "#e8eaf6",
          padding: 16,
          borderRadius: 8,
          gap: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>User Identity</Text>
        <View style={{ gap: 8 }}>
          <Text style={{ fontSize: 14 }}>User ID: {user?.appUserId || "Not logged in"}</Text>
          <Text style={{ fontSize: 14 }}>
            Subscription: {subscriptionStatus?.status || "UNKNOWN"}
          </Text>
        </View>
        <View style={{ gap: 8 }}>
          <Button
            title="Login as New User"
            onPress={async () => {
              const newUserId = `user_${Date.now()}`
              await identify(newUserId)
              // Also identify in RevenueCat
              await Purchases.logIn(newUserId)
              console.log("Logged in as new user:", newUserId)
            }}
            color="#5e35b1"
          />
          <Button
            title="Sign Out"
            onPress={async () => {
              signOut()
              await Purchases.logOut()
              console.log("Signed out")
            }}
            color="#7e57c2"
          />
          <Button
            title="Sync Subscription Status"
            onPress={syncSubscriptionStatus}
            color="#9575cd"
          />
        </View>
      </View>

      {/* Trigger Paywall */}
      <View style={{ gap: 12 }}>
        <Button title="Open Paywall" onPress={triggerPaywall} />
        {purchaseStatus ? (
          <View
            style={{
              backgroundColor: purchaseStatus.includes("✓") ? "#e8f5e9" : "#fff3e0",
              padding: 12,
              borderRadius: 8,
            }}
          >
            <Text style={{ fontSize: 14 }}>{purchaseStatus}</Text>
          </View>
        ) : null}
      </View>

      {/* How It Works */}
      <View
        style={{
          backgroundColor: "#f9fafb",
          padding: 16,
          borderRadius: 8,
          gap: 8,
          marginTop: 20,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>How It Works:</Text>
        <Text style={{ fontSize: 14, lineHeight: 20 }}>
          1. RevenueCat is configured on app mount with your API keys
          {"\n"}2. CustomPurchaseControllerProvider wraps SuperwallProvider
          {"\n"}3. When user taps purchase → onPurchase() calls RevenueCat's purchaseStoreProduct()
          {"\n"}4. When user taps restore → onPurchaseRestore() calls RevenueCat's
          restorePurchases()
          {"\n"}5. Success/failure is communicated back to Superwall automatically
          {"\n"}6. Throwing errors indicates failure; resolving indicates success
        </Text>
      </View>

      {/* Implementation Code */}
      <View
        style={{
          backgroundColor: "#f5f5f5",
          padding: 16,
          borderRadius: 8,
          gap: 8,
          marginTop: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Key Implementation Points:</Text>
        <Text style={{ fontSize: 13, lineHeight: 20, fontFamily: "monospace" }}>
          • Configure RevenueCat in useEffect
          {"\n"}• Wrap app with CustomPurchaseControllerProvider
          {"\n"}• onPurchase: Get product → purchaseStoreProduct()
          {"\n"}• onPurchaseRestore: Call restorePurchases()
          {"\n"}• Throw errors for failures
          {"\n"}• No return value needed for success (undefined is valid)
        </Text>
      </View>

      {/* Setup Instructions */}
      <View
        style={{
          backgroundColor: "#fff3e0",
          padding: 16,
          borderRadius: 8,
          gap: 8,
          marginTop: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Setup Instructions:</Text>
        <Text style={{ fontSize: 14, lineHeight: 20 }}>
          1. Install: npm install react-native-purchases
          {"\n"}2. Configure RevenueCat in your dashboard
          {"\n"}3. Replace REVENUECAT_API_KEYS with your actual keys
          {"\n"}4. Replace Superwall API keys with your keys
          {"\n"}5. Configure products in both RevenueCat and Superwall
          {"\n"}6. Ensure product IDs match across both platforms
        </Text>
      </View>
    </ScrollView>
  )
}

export default function RevenueCatPage() {
  return (
    <CustomPurchaseControllerProvider
      controller={{
        onPurchase: async (params) => {
          console.log("🛒 onPurchase called with params:", params)

          try {
            // Get products from RevenueCat
            const products = await Purchases.getProducts([params.productId])
            const product = products[0]

            if (!product) {
              console.error("❌ Product not found:", params.productId)
              return { type: "failed", error: `Product not found: ${params.productId}` }
            }

            console.log("Found product:", product.identifier)

            // Make the purchase through RevenueCat
            const { customerInfo } = await Purchases.purchaseStoreProduct(product)

            console.log("✅ Purchase successful!", customerInfo)

            // Return undefined to indicate success (defaults to "purchased")
            return undefined
          } catch (error: any) {
            console.error("❌ Purchase failed:", error)

            // Check if user cancelled
            if (error.userCancelled) {
              console.log("User cancelled the purchase")
              return { type: "cancelled" }
            }

            // Return failure result
            return { type: "failed", error: error.message || "Purchase failed" }
          }
        },

        onPurchaseRestore: async () => {
          console.log("🔄 onPurchaseRestore called")

          try {
            // Restore purchases through RevenueCat
            const customerInfo = await Purchases.restorePurchases()

            console.log("✅ Restore successful!", customerInfo)

            // Check if any entitlements were restored
            const hasActiveEntitlements = Object.keys(customerInfo.entitlements.active).length > 0

            if (hasActiveEntitlements) {
              console.log(
                "Active entitlements found:",
                Object.keys(customerInfo.entitlements.active),
              )
            } else {
              console.log("No active entitlements found")
            }

            // Return undefined to indicate success (defaults to "restored")
            return undefined
          } catch (error: any) {
            console.error("❌ Restore failed:", error)

            // Return failure result
            return { type: "failed", error: error.message || "Restore failed" }
          }
        },
      }}
    >
      <SuperwallProvider
        apiKeys={{
          ios: API_KEY,
          android: "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257",
        }}
      >
        <SuperwallLoading>
          <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 16 }}>
            <ActivityIndicator size="large" color="#667eea" />
            <Text style={{ color: "#666" }}>Loading Superwall...</Text>
          </View>
        </SuperwallLoading>
        <SuperwallLoaded>
          <ScreenContent />
        </SuperwallLoaded>
      </SuperwallProvider>
    </CustomPurchaseControllerProvider>
  )
}
