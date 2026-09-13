import { Link, useRouter } from "expo-router"
import { Button, ScrollView, Text, View } from "react-native"

export default function OuterApp() {
  const router = useRouter()

  return (
    <ScrollView
      contentContainerStyle={{ flexGrow: 1, padding: 24, justifyContent: "center", gap: 16 }}
    >
      <Text style={{ fontSize: 22, fontWeight: "600" }}>Feature Tests</Text>
      <View style={{ gap: 12 }}>
        <Button
          title="Test Paywall Spinner"
          testID="open-spinner-test"
          onPress={() => router.push("/paywall-spinner")}
        />
        <Button
          title="Test Products & Purchase"
          testID="open-purchase-test"
          onPress={() => router.push("/standalone-purchase")}
        />
        <Button
          title="Test Customer Info"
          testID="open-customer-info-test"
          onPress={() => router.push("/customer-info")}
        />
      </View>
      <Text style={{ fontSize: 22, fontWeight: "600", marginTop: 16 }}>SDK Demos</Text>
      <Link href="/compat">Compat SDK Demo</Link>
      <Link href="/new">Hooks SDK Demo</Link>
      <Link href="/custom-purchase">Custom Purchase Controller</Link>
      <Link href="/revenuecat">RevenueCat Integration</Link>
      <Link href="/integration-attributes">Integration Attributes</Link>
      <Link href="/article-paywall">Inline Article Paywall</Link>
    </ScrollView>
  )
}
