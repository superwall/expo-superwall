import {
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useSuperwall,
} from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Button, ScrollView, Text, TextInput } from "react-native"

function ScreenContent() {
  const togglePaywallSpinner = useSuperwall((state) => state.togglePaywallSpinner)
  const [placement, setPlacement] = useState("test")
  const [isRunning, setIsRunning] = useState(false)
  const [message, setMessage] = useState("Ready to test.")
  const { registerPlacement, state } = usePlacement({
    onPresent: () => {
      setIsRunning(false)
      setMessage("Paywall presented. Tap its custom callback button to test the spinner.")
    },
    onDismiss: () => setIsRunning(false),
    onSkip: (reason) => {
      setIsRunning(false)
      setMessage(`Paywall skipped (${reason.type}). Use a placement that presents a paywall.`)
    },
    onError: (error) => {
      setIsRunning(false)
      setMessage(`Paywall error: ${error}`)
    },
    onCustomCallback: async ({ name }) => {
      setIsRunning(true)
      setMessage(`Custom callback "${name}": showing spinner during two seconds of async work.`)
      try {
        await togglePaywallSpinner(false)
        await new Promise((resolve) => setTimeout(resolve, 2000))
        setMessage(
          `Custom callback "${name}" completed. Confirm the spinner appeared, then disappeared.`,
        )
        return { status: "success", data: { message: "Spinner test completed" } }
      } catch (error) {
        setMessage(`Spinner test failed: ${String(error)}`)
        return { status: "failure", data: { message: String(error) } }
      } finally {
        try {
          await togglePaywallSpinner(true)
        } finally {
          setIsRunning(false)
        }
      }
    },
  })

  const runTest = async () => {
    setIsRunning(true)
    setMessage(`Opening placement "${placement.trim()}"…`)
    try {
      await registerPlacement({ placement: placement.trim() })
    } catch (error) {
      setIsRunning(false)
      setMessage(`Could not open paywall: ${String(error)}`)
    }
  }

  return (
    <ScrollView contentContainerStyle={{ padding: 20, gap: 16 }}>
      <Text style={{ fontSize: 24, fontWeight: "bold" }}>Paywall Spinner</Text>
      <Text>
        Use a placement whose paywall has a custom callback button. Open the paywall, then tap that
        button. The callback shows the spinner during two seconds of async work and hides it when
        the work finishes. Close the paywall to see the callback result.
      </Text>
      <TextInput
        accessibilityLabel="Placement name"
        testID="spinner-placement"
        value={placement}
        onChangeText={setPlacement}
        editable={!isRunning}
        autoCapitalize="none"
        autoCorrect={false}
        placeholder="Placement name"
        style={{ borderWidth: 1, borderColor: "#ccc", padding: 12, borderRadius: 8 }}
      />
      <Button
        title={isRunning ? "Working…" : "Open Paywall for Callback Test"}
        testID="run-spinner-test"
        disabled={isRunning || !placement.trim()}
        onPress={runTest}
      />
      <Text testID="spinner-test-result" accessibilityLiveRegion="polite">
        {message}
      </Text>
      <Text>Paywall status: {state.status}</Text>
    </ScrollView>
  )
}

export default function PaywallSpinnerPage() {
  return (
    <SuperwallProvider
      apiKeys={{
        ios: "pk_e361c8a9662281f4249f2fa11d1a63854615fa80e15e7a4d",
        android: "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257",
      }}
    >
      <SuperwallLoading>
        <ActivityIndicator style={{ flex: 1 }} />
      </SuperwallLoading>
      <SuperwallLoaded>
        <ScreenContent />
      </SuperwallLoaded>
    </SuperwallProvider>
  )
}
