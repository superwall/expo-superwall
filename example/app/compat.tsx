import { Ionicons } from "@expo/vector-icons"
import { LinearGradient } from "expo-linear-gradient"
import { Link } from "expo-router"
import { useEffect, useState } from "react"
import {
  Alert,
  Linking,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native"
import Superwall from "superwall-expo/compat"
import { MySuperwallDelegate } from "../lib/compat/SuperwallDelegate"

const delegate = new MySuperwallDelegate()

export default function Compat() {
  const [isConfigured, setIsConfigured] = useState(false)
  const [userId, setUserId] = useState("some_user_id")
  const [lastEvent, setLastEvent] = useState<string | null>(null)

  useEffect(() => {
    let eventListener: any
    const apiKey =
      Platform.OS === "ios"
        ? "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"
        : "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257"

    const configurePlatform = async () => {
      await Superwall.configure({
        apiKey: apiKey,
      })

      console.log("Configured")
      await Superwall.shared.identify({ userId })
      console.log("Identified")
      await Superwall.shared.setDelegate(delegate)
      console.log("Set delegate")
      await Superwall.shared.setUserAttributes({
        test: "abc",
        platform: Platform.OS,
        timestamp: new Date().toISOString(),
      })
      console.log("Set user attributes")

      setIsConfigured(true)

      await Linking.getInitialURL().then((url) => {
        if (url) {
          Superwall.shared.handleDeepLink(url)
          setLastEvent(`Deep link handled: ${url}`)
        }
      })

      eventListener = Linking.addEventListener("url", (event) => {
        Superwall.shared.handleDeepLink(event.url)
        setLastEvent(`Deep link handled: ${event.url}`)
      })
    }

    configurePlatform()

    return () => {
      eventListener?.remove()
    }
  }, [userId])

  const handleOpenPaywall = (placement: string) => {
    Superwall.shared.register({
      placement,
      feature() {
        console.log(`Feature called for placement: ${placement}`)
        setLastEvent(`Feature executed for ${placement}`)
        Alert.alert("Feature Unlocked! 🎉", `Successfully accessed ${placement} feature`)
      },
    })
    setLastEvent(`Paywall opened for ${placement}`)
  }

  const paywallPlacements = [
    { name: "fishing", icon: "fish", color: "#4A90E2" },
    { name: "premium", icon: "star", color: "#F5A623" },
    { name: "pro_features", icon: "rocket", color: "#7ED321" },
    { name: "unlimited", icon: "infinite", color: "#9013FE" },
  ]

  return (
    <LinearGradient colors={["#667eea", "#764ba2"]} style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Link href="/" style={styles.homeLink}>
            <Ionicons name="home" size={24} color="#fff" />
            <Text style={styles.homeLinkText}>Home</Text>
          </Link>

          <Text style={styles.title}>Superwall SDK Demo</Text>
          <Text style={styles.subtitle}>Expo Compatibility Layer</Text>

          <View style={styles.statusCard}>
            <View style={styles.statusRow}>
              <Ionicons
                name={isConfigured ? "checkmark-circle" : "time"}
                size={20}
                color={isConfigured ? "#4CAF50" : "#FF9800"}
              />
              <Text style={styles.statusText}>
                SDK {isConfigured ? "Configured" : "Configuring..."}
              </Text>
            </View>
            <Text style={styles.userIdText}>User ID: {userId}</Text>
            <Text style={styles.platformText}>Platform: {Platform.OS}</Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🎯 Paywall Placements</Text>
          <View style={styles.grid}>
            {paywallPlacements.map((placement) => (
              <TouchableOpacity
                key={placement.name}
                style={[styles.card, { borderLeftColor: placement.color }]}
                onPress={() => handleOpenPaywall(placement.name)}
              >
                <Ionicons name={placement.icon as any} size={24} color={placement.color} />
                <Text style={styles.cardTitle}>{placement.name}</Text>
                <Text style={styles.cardSubtitle}>Tap to open</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🔧 SDK Actions</Text>
          <View style={styles.actionButtons}>
            <TouchableOpacity
              style={[styles.actionButton, styles.primaryButton]}
              onPress={async () => {
                console.log(await Superwall.shared.getUserAttributes())

                Superwall.shared.identify({ userId: `user_${Date.now()}` })
                setUserId(`user_${Date.now()}`)
                setLastEvent("User re-identified")
              }}
            >
              <Ionicons name="person" size={20} color="#fff" />
              <Text style={styles.actionButtonText}>Re-identify User</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.actionButton, styles.secondaryButton]}
              onPress={async () => {
                await Superwall.shared.reset()
                Superwall.shared.setUserAttributes({
                  premium: true,
                  lastAction: new Date().toISOString(),
                  randomValue: Math.floor(Math.random() * 1000),
                })
                setLastEvent("User attributes updated")
              }}
            >
              <Ionicons name="person-add" size={20} color="#667eea" />
              <Text style={[styles.actionButtonText, { color: "#667eea" }]}>Update Attributes</Text>
            </TouchableOpacity>
          </View>
        </View>

        {lastEvent && (
          <View style={styles.eventLog}>
            <Text style={styles.eventLogTitle}>📝 Last Event</Text>
            <Text style={styles.eventLogText}>{lastEvent}</Text>
            <Text style={styles.eventLogTime}>{new Date().toLocaleTimeString()}</Text>
          </View>
        )}
      </ScrollView>
    </LinearGradient>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    padding: 20,
  },
  header: {
    alignItems: "center",
    marginBottom: 30,
  },
  homeLink: {
    flexDirection: "row",
    alignItems: "center",
    alignSelf: "flex-start",
    marginBottom: 20,
    padding: 10,
    backgroundColor: "rgba(255, 255, 255, 0.2)",
    borderRadius: 20,
  },
  homeLinkText: {
    color: "#fff",
    marginLeft: 8,
    fontSize: 16,
    fontWeight: "600",
  },
  title: {
    fontSize: 28,
    fontWeight: "bold",
    color: "#fff",
    textAlign: "center",
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: "rgba(255, 255, 255, 0.8)",
    textAlign: "center",
    marginBottom: 20,
  },
  statusCard: {
    backgroundColor: "rgba(255, 255, 255, 0.95)",
    padding: 16,
    borderRadius: 12,
    width: "100%",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statusRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
  },
  statusText: {
    marginLeft: 8,
    fontSize: 16,
    fontWeight: "600",
    color: "#333",
  },
  userIdText: {
    fontSize: 14,
    color: "#666",
    marginBottom: 4,
  },
  platformText: {
    fontSize: 14,
    color: "#666",
  },
  section: {
    marginBottom: 30,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#fff",
    marginBottom: 16,
    textAlign: "center",
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
  },
  card: {
    backgroundColor: "rgba(255, 255, 255, 0.95)",
    padding: 16,
    borderRadius: 12,
    width: "48%",
    marginBottom: 12,
    alignItems: "center",
    borderLeftWidth: 4,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333",
    marginTop: 8,
    textAlign: "center",
  },
  cardSubtitle: {
    fontSize: 12,
    color: "#666",
    marginTop: 4,
    textAlign: "center",
  },
  actionButtons: {
    gap: 12,
  },
  actionButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    padding: 16,
    borderRadius: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  primaryButton: {
    backgroundColor: "#4CAF50",
  },
  secondaryButton: {
    backgroundColor: "#fff",
  },
  actionButtonText: {
    marginLeft: 8,
    fontSize: 16,
    fontWeight: "600",
    color: "#fff",
  },
  eventLog: {
    backgroundColor: "rgba(255, 255, 255, 0.95)",
    padding: 16,
    borderRadius: 12,
    marginTop: 20,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  eventLogTitle: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#333",
    marginBottom: 8,
  },
  eventLogText: {
    fontSize: 14,
    color: "#666",
    marginBottom: 4,
  },
  eventLogTime: {
    fontSize: 12,
    color: "#999",
    fontStyle: "italic",
  },
})
