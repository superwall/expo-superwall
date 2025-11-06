import {
  CustomPurchaseControllerProvider,
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useUser,
} from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Alert, Button, ScrollView, Text, View } from "react-native"

const API_KEY = "pk_a5959912ceb3087d55c7ea63001202335adfbb54e0f70475"

function ScreenContent({
  selectedResult,
  setSelectedResult,
}: {
  selectedResult: "purchased" | "cancelled" | "failed" | "pending"
  setSelectedResult: (result: "purchased" | "cancelled" | "failed" | "pending") => void
}) {
  const [purchaseStatus, setPurchaseStatus] = useState<string>("")
  const [restoreStatus, setRestoreStatus] = useState<string>("")

  const { identify, user, signOut, subscriptionStatus, setSubscriptionStatus } = useUser()

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
      } else if (result?.type === "declined") {
        setPurchaseStatus("Purchase was declined")
      }
    },
  })

  const triggerPaywall = async () => {
    setPurchaseStatus("")
    await registerPlacement({
      placement: "upgrade_button",
      feature() {
        console.log("Feature unlocked!")
        Alert.alert("Feature Unlocked! 🎉", "Successfully accessed fishing feature")
      },
    })
  }

  return (
    <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 20, gap: 20 }}>
      <View style={{ gap: 8 }}>
        <Text style={{ fontSize: 24, fontWeight: "bold" }}>Custom Purchase Controller</Text>
        <Text style={{ fontSize: 14, color: "#666" }}>
          This example demonstrates how to handle purchases and restores with custom logic
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
              await identify(`user_${Date.now()}`)
              console.log("Logged in as new user")
            }}
            color="#5e35b1"
          />
          <Button
            title="Sign Out"
            onPress={() => {
              signOut()
              console.log("Signed out")
            }}
            color="#7e57c2"
          />
        </View>
      </View>

      {/* Subscription Status Controls */}
      <View
        style={{
          backgroundColor: "#e8f5e9",
          padding: 16,
          borderRadius: 8,
          gap: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Subscription Status</Text>
        <Text style={{ fontSize: 12, color: "#666" }}>
          Change subscription status to test paywall behavior for subscribed vs unsubscribed users
        </Text>
        <View style={{ gap: 8 }}>
          <Button
            title="Set Active Subscription"
            onPress={async () => {
              await setSubscriptionStatus({
                status: "ACTIVE",
                entitlements: [
                  {
                    id: "pro",
                    type: "SERVICE_LEVEL",
                  },
                ],
              })
              console.log("Subscription status set to ACTIVE")
            }}
            color="#388e3c"
          />
          <Button
            title="Set Inactive Subscription (Unsubscribed)"
            onPress={async () => {
              await setSubscriptionStatus({
                status: "INACTIVE",
              })
              console.log("Subscription status set to INACTIVE")
            }}
            color="#d32f2f"
          />
        </View>
      </View>

      {/* Purchase Result Selector */}
      <View
        style={{
          backgroundColor: "#f5f5f5",
          padding: 16,
          borderRadius: 8,
          gap: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Purchase Result Type:</Text>
        <View style={{ gap: 8 }}>
          {(["purchased", "cancelled", "failed", "pending"] as const).map((type) => (
            <Button
              key={type}
              title={`${type.toUpperCase()} ${selectedResult === type ? "✓" : ""}`}
              onPress={() => setSelectedResult(type)}
              color={selectedResult === type ? "#667eea" : "#999"}
            />
          ))}
        </View>
        <Text style={{ fontSize: 12, color: "#666", marginTop: 4 }}>
          Select which result the controller will return when a purchase is triggered
        </Text>
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

      {/* Restore Purchases */}
      <View style={{ gap: 12, marginTop: 20 }}>
        <Text style={{ fontSize: 18, fontWeight: "600" }}>Restore Purchases (Test)</Text>
        <Text style={{ fontSize: 12, color: "#666" }}>
          Note: In real usage, restore is triggered by the paywall's restore button. This is for
          testing the controller logic.
        </Text>
        <Button
          title="Test Restore Flow"
          onPress={async () => {
            setRestoreStatus("Testing restore flow...")
            console.log("Restore button pressed - calling controller.onPurchaseRestore()")

            try {
              // Simulate what happens when Superwall's paywall restore button is tapped
              // In production, CustomPurchaseControllerProvider handles this automatically
              const controller = {
                onPurchaseRestore: async () => {
                  console.log("🔄 onPurchaseRestore called")
                  await new Promise((resolve) => setTimeout(resolve, 1500))

                  // In real implementation: check backend, restore entitlements
                  const hasEntitlements = Math.random() > 0.5

                  if (hasEntitlements) {
                    return { type: "restored" as const }
                  } else {
                    return { type: "failed" as const, error: "No purchases found to restore" }
                  }
                },
              }

              const result = await controller.onPurchaseRestore()

              if (result.type === "restored") {
                setRestoreStatus("✓ Restore successful! Entitlements restored.")
                await setSubscriptionStatus({
                  status: "ACTIVE",
                  entitlements: [{ id: "pro", type: "SERVICE_LEVEL" }],
                })
              } else {
                setRestoreStatus(`✗ Restore failed: ${result.error}`)
              }
            } catch (error: any) {
              setRestoreStatus(`✗ Error: ${error.message}`)
              console.error("Restore error:", error)
            }
          }}
        />
        {restoreStatus ? (
          <View
            style={{
              backgroundColor: restoreStatus.includes("✓") ? "#e8f5e9" : "#ffebee",
              padding: 12,
              borderRadius: 8,
            }}
          >
            <Text style={{ fontSize: 14 }}>{restoreStatus}</Text>
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
          1. CustomPurchaseControllerProvider wraps your app and listens for purchase events
          {"\n"}2. When user taps purchase button in paywall → onPurchase() is called
          {"\n"}3. When user taps restore button in paywall → onPurchaseRestore() is called
          {"\n"}4. Your custom logic processes the purchase/restore (e.g., RevenueCat, backend API)
          {"\n"}5. Return PurchaseResult or RestoreResult to inform Superwall of the outcome
          {"\n"}6. Superwall dismisses the paywall or shows error based on your result
        </Text>
        <Text style={{ fontSize: 12, color: "#666", marginTop: 8 }}>
          Important: Return undefined or {`{type: "purchased"}`} for success,{" "}
          {`{type: "failed", error: "..."}`} for failures.
        </Text>
      </View>
    </ScrollView>
  )
}

export default function CustomPurchasePage() {
  const [selectedResult, setSelectedResult] = useState<
    "purchased" | "cancelled" | "failed" | "pending"
  >("purchased")

  return (
    <CustomPurchaseControllerProvider
      controller={{
        onPurchase: async (params) => {
          console.log("🛒 onPurchase called with params:", params)

          // Log platform-specific details
          if (params.platform === "ios") {
            console.log("iOS Purchase - Product ID:", params.productId)
          } else {
            console.log(
              "Android Purchase - Product ID:",
              params.productId,
              "Base Plan:",
              params.basePlanId,
              "Offer:",
              params.offerId,
            )
          }

          // Simulate processing time
          await new Promise((resolve) => setTimeout(resolve, 1000))

          // In a real implementation, you would:
          // 1. Call your backend to process the purchase
          // 2. Validate the purchase with the store (App Store/Play Store)
          // 3. Grant entitlements to the user
          // 4. Return the appropriate result

          const result = {
            type: selectedResult,
            error: selectedResult === "failed" ? "Payment method declined" : undefined,
          }

          console.log("✅ onPurchase returning:", result)
          return result
        },

        onPurchaseRestore: async () => {
          console.log("🔄 onPurchaseRestore called")

          // Simulate processing time
          await new Promise((resolve) => setTimeout(resolve, 1500))

          // In a real implementation, you would:
          // 1. Call your backend to fetch user's purchase history
          // 2. Restore entitlements if found
          // 3. Return success or failure

          const result = {
            type: "restored" as const,
          }

          console.log("✅ onPurchaseRestore returning:", result)
          return result
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
          <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
            <ActivityIndicator size="large" color="#667eea" />
            <Text style={{ marginTop: 16, color: "#666" }}>Loading Superwall...</Text>
          </View>
        </SuperwallLoading>
        <SuperwallLoaded>
          <ScreenContent selectedResult={selectedResult} setSelectedResult={setSelectedResult} />
        </SuperwallLoaded>
      </SuperwallProvider>
    </CustomPurchaseControllerProvider>
  )
}
