import { SuperwallProvider } from "expo-superwall"
import { ActivityIndicator, Text, View } from "react-native"

const API_KEY = "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c" // replace with your key

function ScreenContent() {
  // const { configured, loading, registerPlacement, subscriptionStatus, lastPaywallResult } =
  //   useSuperwall((state) => ({
  //     configured: state.configured,
  //     loading: state.loading,
  //     registerPlacement: state.registerPlacement,
  //     subscriptionStatus: state.subscriptionStatus,
  //     lastPaywallResult: state.lastPaywallResult,
  //   }))

  // const [placementLoading, setPlacementLoading] = useState(false)

  // if (!configured || loading) {
  //   return (
  //     <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
  //       <ActivityIndicator />
  //       <Text style={{ marginTop: 8 }}>Initialising Superwall…</Text>
  //     </View>
  //   )
  // }

  // const triggerPlacement = async () => {
  //   setPlacementLoading(true)
  //   try {
  //     await registerPlacement("fishing")
  //   } finally {
  //     setPlacementLoading(false)
  //   }
  // }

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
      <Text style={{ fontSize: 18, fontWeight: "600" }}>Superwall SDK ready!</Text>
      {/* <Text>Subscription status: {subscriptionStatus?.status ?? "unknown"}</Text>
      {lastPaywallResult && <Text>Last paywall result: {JSON.stringify(lastPaywallResult)}</Text>}
      <Button
        title="Open paywall placement"
        disabled={placementLoading}
        onPress={triggerPlacement}
      /> */}
    </View>
  )
}

export default function NewPage() {
  return (
    <SuperwallProvider apiKey={API_KEY} fallback={<ActivityIndicator style={{ flex: 1 }} />}>
      <ScreenContent />
    </SuperwallProvider>
  )
}
