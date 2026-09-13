import {
  type CustomerInfo,
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  usePlacement,
  useSuperwall,
  useSuperwallEvents,
  useUser,
} from "expo-superwall"
import { useState } from "react"
import { ActivityIndicator, Button, ScrollView, Text, View } from "react-native"

const API_KEY = "pk_a5959912ceb3087d55c7ea63001202335adfbb54e0f70475"

function ScreenContent() {
  const { customerInfo, getCustomerInfo, user } = useUser()
  const restorePurchases = useSuperwall((state) => state.restorePurchases)

  const [statusMessage, setStatusMessage] = useState("")
  const [changeLog, setChangeLog] = useState<string[]>([])

  const { registerPlacement } = usePlacement({
    onError: (err) => setStatusMessage(`✗ Paywall error: ${err}`),
  })

  useSuperwallEvents({
    onCustomerInfoChange: (from, to) => {
      const line = `subs ${from.subscriptions.length} → ${to.subscriptions.length}, one-time ${from.nonSubscriptions.length} → ${to.nonSubscriptions.length}, entitlements ${from.entitlements.length} → ${to.entitlements.length}`
      setChangeLog((log) => [`${new Date().toLocaleTimeString()}: ${line}`, ...log])
      console.log("customerInfoDidChange", { from, to })
    },
  })

  const handleRefresh = async () => {
    try {
      const info = await getCustomerInfo()
      setStatusMessage(`✓ Fetched customer info for "${info.userId || "anonymous"}"`)
      console.log("getCustomerInfo:", info)
    } catch (error: any) {
      setStatusMessage(`✗ Error: ${error.message}`)
    }
  }

  const handleRestore = async () => {
    try {
      const result = await restorePurchases()
      setStatusMessage(
        result.result === "restored"
          ? "✓ Purchases restored"
          : `✗ Restore failed: ${result.errorMessage}`,
      )
    } catch (error: any) {
      setStatusMessage(`✗ Error: ${error.message}`)
    }
  }

  return (
    <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 20, gap: 20 }}>
      <View style={{ gap: 8 }}>
        <Text style={{ fontSize: 24, fontWeight: "bold" }}>Customer Info</Text>
        <Text style={{ fontSize: 14, color: "#666" }}>
          The customer's full purchase history and entitlements, seeded on launch and kept in sync
          via the customerInfoDidChange event.
        </Text>
      </View>

      {/* Snapshot */}
      <View style={{ backgroundColor: "#e8eaf6", padding: 16, borderRadius: 8, gap: 8 }}>
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Current Snapshot</Text>
        <Text style={{ fontSize: 14 }}>User ID: {user?.appUserId || "Not logged in"}</Text>
        {customerInfo ? (
          <CustomerInfoView info={customerInfo} />
        ) : (
          <Text style={{ fontSize: 14, color: "#666", fontStyle: "italic" }}>
            No snapshot yet — waiting for the SDK to load customer data.
          </Text>
        )}
      </View>

      <View style={{ gap: 8 }}>
        <Button
          title="Refresh Customer Info"
          testID="refresh-customer-info"
          onPress={handleRefresh}
        />
        <Button
          title="Show Paywall (test)"
          testID="customer-info-show-paywall"
          onPress={() => registerPlacement({ placement: "test" })}
        />
        <Button
          title="Restore Purchases"
          testID="customer-info-restore"
          onPress={handleRestore}
          color="#388e3c"
        />
      </View>

      {/* Change log */}
      <View style={{ backgroundColor: "#e8f5e9", padding: 16, borderRadius: 8, gap: 8 }}>
        <Text style={{ fontSize: 16, fontWeight: "600" }}>Change Events</Text>
        {changeLog.length > 0 ? (
          changeLog.map((line) => (
            <Text key={line} style={{ fontSize: 12, fontFamily: "monospace" }}>
              {line}
            </Text>
          ))
        ) : (
          <Text style={{ fontSize: 14, color: "#666", fontStyle: "italic" }}>
            No changes yet. Purchase or restore to see customerInfoDidChange fire.
          </Text>
        )}
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
    </ScrollView>
  )
}

function CustomerInfoView({ info }: { info: CustomerInfo }) {
  return (
    <View style={{ backgroundColor: "white", padding: 12, borderRadius: 4, gap: 10 }}>
      <View style={{ gap: 4 }}>
        <Text style={{ fontSize: 14, fontWeight: "600" }}>
          Subscriptions ({info.subscriptions.length})
        </Text>
        {info.subscriptions.map((sub) => (
          <Text key={sub.transactionId} style={{ fontSize: 12, fontFamily: "monospace" }}>
            {sub.productId} · {sub.isActive ? "active" : "inactive"} ·{" "}
            {sub.willRenew ? "renews" : "won't renew"}
            {sub.expirationDate ? ` · expires ${sub.expirationDate}` : ""} · {sub.store}
          </Text>
        ))}
      </View>
      <View style={{ gap: 4 }}>
        <Text style={{ fontSize: 14, fontWeight: "600" }}>
          One-time Purchases ({info.nonSubscriptions.length})
        </Text>
        {info.nonSubscriptions.map((txn) => (
          <Text key={txn.transactionId} style={{ fontSize: 12, fontFamily: "monospace" }}>
            {txn.productId} · {txn.isConsumable ? "consumable" : "non-consumable"} ·{" "}
            {txn.purchaseDate} · {txn.store}
          </Text>
        ))}
      </View>
      <View style={{ gap: 4 }}>
        <Text style={{ fontSize: 14, fontWeight: "600" }}>
          Entitlements ({info.entitlements.length})
        </Text>
        {info.entitlements.map((entitlement) => (
          <Text key={entitlement.id} style={{ fontSize: 12, fontFamily: "monospace" }}>
            {entitlement.id} · {entitlement.type}
          </Text>
        ))}
      </View>
    </View>
  )
}

export default function CustomerInfoPage() {
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
