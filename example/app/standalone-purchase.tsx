import { SuperwallLoaded, SuperwallLoading, SuperwallProvider, useSuperwall } from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Button, ScrollView, Text, TextInput } from "react-native"

const API_KEY = "pk_e361c8a9662281f4249f2fa11d1a63854615fa80e15e7a4d"

function ScreenContent() {
  const { purchase, products } = useSuperwall()
  const [productId, setProductId] = useState("superwall_pro_3999")
  const [result, setResult] = useState<string | null>(null)
  const [pendingAction, setPendingAction] = useState<"products" | "purchase" | null>(null)

  const handleProducts = async () => {
    setPendingAction("products")
    setResult(null)
    try {
      const res = await products([productId.trim()])
      setResult(JSON.stringify(res, null, 2))
    } catch (error) {
      setResult(`Fetch products failed: ${String(error)}`)
    } finally {
      setPendingAction(null)
    }
  }

  const handlePurchase = async () => {
    setPendingAction("purchase")
    setResult(null)
    try {
      const res = await purchase(productId.trim())
      setResult(JSON.stringify(res, null, 2))
    } catch (error) {
      setResult(`Purchase failed: ${String(error)}`)
    } finally {
      setPendingAction(null)
    }
  }

  return (
    <ScrollView
      contentContainerStyle={{ padding: 20, gap: 12 }}
      keyboardShouldPersistTaps="handled"
    >
      <Text style={{ fontSize: 16, fontWeight: "600" }}>Standalone Purchase Test</Text>
      <Text>
        Enter a product ID configured for this app. Fetch Products reads its store details; Purchase
        opens the store purchase flow without presenting a paywall.
      </Text>

      <TextInput
        value={productId}
        accessibilityLabel="Product ID"
        testID="purchase-product-id"
        onChangeText={setProductId}
        autoCapitalize="none"
        autoCorrect={false}
        editable={pendingAction === null}
        placeholder="Product ID"
        style={{ borderWidth: 1, borderColor: "#ccc", padding: 12, borderRadius: 8 }}
      />

      <Button
        title={pendingAction === "products" ? "Fetching Products…" : "Fetch Products"}
        testID="fetch-products"
        disabled={pendingAction !== null || !productId.trim()}
        onPress={handleProducts}
      />
      <Button
        title={pendingAction === "purchase" ? "Purchasing…" : "Purchase"}
        testID="purchase-product"
        disabled={pendingAction !== null || !productId.trim()}
        onPress={handlePurchase}
      />

      {result && (
        <Text selectable testID="purchase-test-result" style={{ fontSize: 12 }}>
          {result}
        </Text>
      )}
    </ScrollView>
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
