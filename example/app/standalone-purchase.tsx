import { SuperwallLoaded, SuperwallLoading, SuperwallProvider, useSuperwall } from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Alert, Button, Text, TextInput, View } from "react-native"

const API_KEY = "pk_e361c8a9662281f4249f2fa11d1a63854615fa80e15e7a4d"

function ScreenContent() {
  const { purchase, products } = useSuperwall()
  const [productId, setProductId] = useState("com.example.monthly")
  const [result, setResult] = useState<string | null>(null)

  const handleProducts = async () => {
    try {
      const res = await products([productId])
      setResult(JSON.stringify(res, null, 2))
    } catch (e: any) {
      Alert.alert("Error", e.message)
    }
  }

  const handlePurchase = async () => {
    try {
      const res = await purchase(productId)
      setResult(JSON.stringify(res, null, 2))
    } catch (e: any) {
      Alert.alert("Error", e.message)
    }
  }

  return (
    <View style={{ flex: 1, padding: 20, gap: 12 }}>
      <Text style={{ fontSize: 16, fontWeight: "600" }}>Standalone Purchase Test</Text>

      <TextInput
        value={productId}
        onChangeText={setProductId}
        placeholder="Product ID"
        style={{ borderWidth: 1, borderColor: "#ccc", padding: 12, borderRadius: 8 }}
      />

      <Button title="Fetch Products" onPress={handleProducts} />
      <Button title="Purchase" onPress={handlePurchase} />

      {result && (
        <Text selectable style={{ fontSize: 12 }}>
          {result}
        </Text>
      )}
    </View>
  )
}

export default function StandalonePurchasePage() {
  return (
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
  )
}
