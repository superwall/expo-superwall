import {
  CustomPurchaseControllerProvider,
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useSuperwallEvents,
  useUser,
} from "expo-superwall"
import SuperwallExpoModule from "expo-superwall/SuperwallExpoModule"
import { useEffect } from "react"
import { ActivityIndicator, Alert, Button, Text, View } from "react-native"

const API_KEY = "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"

function ScreenContent() {
  const { identify, user, signOut, update, refresh, subscriptionStatus, setSubscriptionStatus } =
    useUser()

  useSuperwallEvents({
    onLog: (log) => console.log(log),
  })

  const { registerPlacement, state } = usePlacement({
    onError: (err) => console.error(err),
    onPresent: (info) => console.log("Paywall presented", info),
    onDismiss: (info, result) => console.log("Paywall dismissed", info, result),
  })

  const triggerPlacement = async () => {
    await registerPlacement({
      placement: "fishing",
      feature() {
        console.log("Feature called")
        Alert.alert("Feature Unlocked! 🎉", "Successfully accessed fishing feature")
      },
    })
  }

  useEffect(() => {
    SuperwallExpoModule.setLogLevel("debug")
  }, [])

  const updateUser = async () => {
    await update((old) => ({ ...old, counter: (old.counter || 0) + 1 }))
  }

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
      <Text style={{ fontSize: 18, fontWeight: "600" }}>Superwall SDK ready!</Text>
      <Text>Subscription status: {subscriptionStatus?.status ?? "unknown"}</Text>
      {user && <Text>User: {user.appUserId}</Text>}
      {user && <Text>User attributes: {JSON.stringify(user, null, 2)}</Text>}
      {state && <Text>Last paywall result: {JSON.stringify(state)}</Text>}

      <Button
        title={`Update user attributes with counter: ${user?.counter || 0}`}
        onPress={updateUser}
      />

      <Button title="Refresh User Attributes" onPress={refresh} />
      <Button
        title="Sign out"
        onPress={async () => {
          signOut()
        }}
      />
      <Button
        title="Login"
        onPress={async () => {
          await identify(`user_${Date.now()}`)
        }}
      />

      <Button
        title="Set Subscription Status Active"
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
          console.log("Subscription status set to active")
        }}
      />
      <Button
        title="Set Subscription Status inactive"
        onPress={async () => {
          await setSubscriptionStatus({
            status: "INACTIVE",
          })
          console.log("Subscription status set to inactive")
        }}
      />

      <Button title="Open paywall placement" onPress={triggerPlacement} />
    </View>
  )
}

export default function NewPage() {
  return (
    <CustomPurchaseControllerProvider
      controller={{
        onPurchase: async (params) => {
          if (params.platform === "ios") {
            console.log("onPurchase", params)
          } else {
            console.log("onPurchase", params.productId)
          }
          // Set ur system here
          return {
            type: "purchased",
          }
        },
        onPurchaseRestore: async () => {
          console.log("onPurchaseRestore")
          // Set ur system here
          return {
            type: "failed",
          }
        },
      }}
    >
      <SuperwallProvider
        apiKeys={{ ios: API_KEY, android: "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257" }}
      >
        <SuperwallLoading>
          <ActivityIndicator style={{ flex: 1 }} />
        </SuperwallLoading>
        <SuperwallLoaded>
          <ScreenContent />
        </SuperwallLoaded>
      </SuperwallProvider>
    </CustomPurchaseControllerProvider>
  )
}
