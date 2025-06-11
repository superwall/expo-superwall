import {
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  useSuperwall,
  useUser,
} from "expo-superwall"
import { useEffect, useState } from "react"
import { ActivityIndicator, Button, Text, View } from "react-native"

const API_KEY = "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"

function ScreenContent() {
  const { registerPlacement, subscriptionStatus, lastPaywallResult, reset } = useSuperwall(
    (state) => ({
      registerPlacement: state.registerPlacement,
      subscriptionStatus: state.subscriptionStatus,
      lastPaywallResult: state.lastPaywallResult,
      reset: state.reset,
    }),
  )

  const { identify, user, signOut } = useUser()

  useEffect(() => {
    console.log("user", user)
  }, [user])

  const [placementLoading, setPlacementLoading] = useState(false)

  const triggerPlacement = async () => {
    setPlacementLoading(true)
    try {
      await registerPlacement("fishing")
    } finally {
      setPlacementLoading(false)
    }
  }

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
      <Text style={{ fontSize: 18, fontWeight: "600" }}>Superwall SDK ready!</Text>
      <Text>Subscription status: {subscriptionStatus?.status ?? "unknown"}</Text>
      {user && <Text>User: {user.appUserId}</Text>}
      {lastPaywallResult && <Text>Last paywall result: {JSON.stringify(lastPaywallResult)}</Text>}

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
        title="Open paywall placement"
        disabled={placementLoading}
        onPress={triggerPlacement}
      />
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
