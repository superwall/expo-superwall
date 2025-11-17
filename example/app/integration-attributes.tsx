import { SuperwallLoaded, SuperwallLoading, SuperwallProvider, useUser } from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Button, ScrollView, Text, TextInput, View } from "react-native"

const API_KEY = "pk_a5959912ceb3087d55c7ea63001202335adfbb54e0f70475"

function ScreenContent() {
  const { setIntegrationAttributes, getIntegrationAttributes, user } = useUser()

  const [adjustId, setAdjustId] = useState("")
  const [amplitudeUserId, setAmplitudeUserId] = useState("")
  const [appsflyerId, setAppsflyerId] = useState("")
  const [mixpanelDistinctId, setMixpanelDistinctId] = useState("")
  const [onesignalId, setOnesignalId] = useState("")

  const [currentAttributes, setCurrentAttributes] = useState<Record<string, string>>({})
  const [statusMessage, setStatusMessage] = useState("")

  const handleSetAttributes = async () => {
    try {
      const attributes: Record<string, string> = {}

      if (adjustId) attributes.adjustId = adjustId
      if (amplitudeUserId) attributes.amplitudeUserId = amplitudeUserId
      if (appsflyerId) attributes.appsflyerId = appsflyerId
      if (mixpanelDistinctId) attributes.mixpanelDistinctId = mixpanelDistinctId
      if (onesignalId) attributes.onesignalId = onesignalId

      await setIntegrationAttributes(attributes)
      setStatusMessage("✓ Integration attributes set successfully!")
      console.log("Set integration attributes:", attributes)

      // Refresh displayed attributes
      await handleGetAttributes()
    } catch (error: any) {
      setStatusMessage(`✗ Error: ${error.message}`)
      console.error("Error setting integration attributes:", error)
    }
  }

  const handleGetAttributes = async () => {
    try {
      const attributes = await getIntegrationAttributes()
      setCurrentAttributes(attributes)
      setStatusMessage("✓ Retrieved current integration attributes")
      console.log("Current integration attributes:", attributes)
    } catch (error: any) {
      setStatusMessage(`✗ Error: ${error.message}`)
      console.error("Error getting integration attributes:", error)
    }
  }

  const handleClearAttributes = async () => {
    try {
      await setIntegrationAttributes({})
      setCurrentAttributes({})
      setAdjustId("")
      setAmplitudeUserId("")
      setAppsflyerId("")
      setMixpanelDistinctId("")
      setOnesignalId("")
      setStatusMessage("✓ Integration attributes cleared")
      console.log("Cleared all integration attributes")
    } catch (error: any) {
      setStatusMessage(`✗ Error: ${error.message}`)
      console.error("Error clearing integration attributes:", error)
    }
  }

  return (
    <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 20, gap: 20 }}>
      <View style={{ gap: 8 }}>
        <Text style={{ fontSize: 24, fontWeight: "bold" }}>Integration Attributes</Text>
        <Text style={{ fontSize: 14, color: "#666" }}>
          Link user IDs from third-party attribution and analytics platforms to Superwall
        </Text>
      </View>

      {/* User Info */}
      <View
        style={{
          backgroundColor: "#e8eaf6",
          padding: 16,
          borderRadius: 8,
          gap: 8,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Current User</Text>
        <Text style={{ fontSize: 14 }}>User ID: {user?.appUserId || "Not logged in"}</Text>
      </View>

      {/* Set Integration Attributes */}
      <View
        style={{
          backgroundColor: "#f5f5f5",
          padding: 16,
          borderRadius: 8,
          gap: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Set Integration Attributes</Text>
        <Text style={{ fontSize: 12, color: "#666" }}>
          Enter IDs from your attribution/analytics platforms
        </Text>

        <View style={{ gap: 12 }}>
          <View>
            <Text style={{ fontSize: 14, marginBottom: 4 }}>Adjust ID</Text>
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: "#ddd",
                padding: 10,
                borderRadius: 4,
                backgroundColor: "white",
              }}
              placeholder="e.g., abc123def456"
              value={adjustId}
              onChangeText={setAdjustId}
            />
          </View>

          <View>
            <Text style={{ fontSize: 14, marginBottom: 4 }}>Amplitude User ID</Text>
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: "#ddd",
                padding: 10,
                borderRadius: 4,
                backgroundColor: "white",
              }}
              placeholder="e.g., user_12345"
              value={amplitudeUserId}
              onChangeText={setAmplitudeUserId}
            />
          </View>

          <View>
            <Text style={{ fontSize: 14, marginBottom: 4 }}>AppsFlyer ID</Text>
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: "#ddd",
                padding: 10,
                borderRadius: 4,
                backgroundColor: "white",
              }}
              placeholder="e.g., 1234567890-1234567"
              value={appsflyerId}
              onChangeText={setAppsflyerId}
            />
          </View>

          <View>
            <Text style={{ fontSize: 14, marginBottom: 4 }}>Mixpanel Distinct ID</Text>
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: "#ddd",
                padding: 10,
                borderRadius: 4,
                backgroundColor: "white",
              }}
              placeholder="e.g., 18f2e123-abc4-def5-gh67-890abc123def"
              value={mixpanelDistinctId}
              onChangeText={setMixpanelDistinctId}
            />
          </View>

          <View>
            <Text style={{ fontSize: 14, marginBottom: 4 }}>OneSignal ID</Text>
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: "#ddd",
                padding: 10,
                borderRadius: 4,
                backgroundColor: "white",
              }}
              placeholder="e.g., 12345678-abcd-1234-efgh-567890abcdef"
              value={onesignalId}
              onChangeText={setOnesignalId}
            />
          </View>
        </View>

        <Button title="Set Attributes" onPress={handleSetAttributes} />
      </View>

      {/* View Current Attributes */}
      <View
        style={{
          backgroundColor: "#e8f5e9",
          padding: 16,
          borderRadius: 8,
          gap: 12,
        }}
      >
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Current Integration Attributes</Text>
        <Button title="Refresh Attributes" onPress={handleGetAttributes} color="#388e3c" />

        {Object.keys(currentAttributes).length > 0 ? (
          <View
            style={{
              backgroundColor: "white",
              padding: 12,
              borderRadius: 4,
              gap: 6,
            }}
          >
            {Object.entries(currentAttributes).map(([key, value]) => (
              <Text key={key} style={{ fontSize: 14, fontFamily: "monospace" }}>
                <Text style={{ fontWeight: "600" }}>{key}:</Text> {value}
              </Text>
            ))}
          </View>
        ) : (
          <Text style={{ fontSize: 14, color: "#666", fontStyle: "italic" }}>
            No integration attributes set. Click "Refresh Attributes" to check.
          </Text>
        )}
      </View>

      {/* Clear Attributes */}
      <View style={{ gap: 8 }}>
        <Button title="Clear All Attributes" onPress={handleClearAttributes} color="#d32f2f" />
      </View>

      {/* Status Message */}
      {statusMessage ? (
        <View
          style={{
            backgroundColor: statusMessage.includes("✓") ? "#e8f5e9" : "#ffebee",
            padding: 12,
            borderRadius: 8,
          }}
        >
          <Text style={{ fontSize: 14 }}>{statusMessage}</Text>
        </View>
      ) : null}

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
          Integration attributes allow you to link user IDs from third-party platforms to Superwall
          for better analytics and attribution.
          {"\n\n"}
          Supported platforms include:
          {"\n"}• Attribution: Adjust, AppsFlyer, Kochava, Tenjin
          {"\n"}• Analytics: Amplitude, Mixpanel, mParticle, PostHog, CleverTap, Firebase
          {"\n"}• Messaging: Braze, OneSignal, Iterable, Airship, Customer.io
          {"\n"}• Other: Facebook Anonymous ID
          {"\n\n"}
          Call setIntegrationAttributes() after you've initialized your third-party SDKs and
          obtained their user IDs.
        </Text>
      </View>
    </ScrollView>
  )
}

export default function IntegrationAttributesPage() {
  return (
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
        <ScreenContent />
      </SuperwallLoaded>
    </SuperwallProvider>
  )
}
