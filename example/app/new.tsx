import {
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useSuperwall,
  useUser,
} from "expo-superwall"
import { ActivityIndicator, Alert, Button, Text, View } from "react-native"

const API_KEY = "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"

function ScreenContent() {
  const { subscriptionStatus } = useSuperwall((state) => ({
    subscriptionStatus: state.subscriptionStatus,
  }))

  const { identify, user, signOut, update, refresh } = useUser()
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
