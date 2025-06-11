import {
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePaywall,
  useSuperwall,
  useUser,
} from "expo-superwall"
import { useEffect } from "react"
import { ActivityIndicator, Alert, Button, Text, View } from "react-native"

const API_KEY = "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"

function ScreenContent() {
  const { subscriptionStatus, lastPaywallResult } = useSuperwall((state) => ({
    subscriptionStatus: state.subscriptionStatus,
    lastPaywallResult: state.lastPaywallResult,
  }))

  const { identify, user, signOut, refresh } = useUser()
  const { registerPlacement, error, state } = usePaywall({
    onError: (err) => console.error(err),
    onPresent: (info) => console.log("Paywall presented", info),
  })

  useEffect(() => {
    console.log(error, state)
  }, [error, state])

  const triggerPlacement = async () => {
    await registerPlacement({
      placement: "fishing",
      feature() {
        console.log("Feature called")
        Alert.alert("Feature Unlocked! 🎉", "Successfully accessed fishing feature")
      },
    })
  }

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
      <Text style={{ fontSize: 18, fontWeight: "600" }}>Superwall SDK ready!</Text>
      <Text>Subscription status: {subscriptionStatus?.status ?? "unknown"}</Text>
      {user && <Text>User: {user.appUserId}</Text>}
      {lastPaywallResult && <Text>Last paywall result: {JSON.stringify(lastPaywallResult)}</Text>}

      <Button title="Refresh" onPress={refresh} />
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

      <Button title="Open paywall placement" onPress={triggerPlacement} />
    </View>
  )
}

export default function NewPage() {
  return (
    <SuperwallProvider apiKey={API_KEY}>
      <SuperwallLoading>
        <ActivityIndicator style={{ flex: 1 }} />
      </SuperwallLoading>
      <SuperwallLoaded>
        <ScreenContent />
      </SuperwallLoaded>
    </SuperwallProvider>
  )
}
