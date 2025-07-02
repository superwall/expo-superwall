import { useRouter } from "expo-router"
import type { SubscriptionStatus } from "expo-superwall/compat"
import Superwall from "expo-superwall/compat"
import { useEffect, useState } from "react"
import { Alert, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native"
import { TestButton } from "./TestButton"
import { TestingPurchaseController } from "./TestingPurchaseController"

export default function PurchaseControllerTest() {
  const router = useRouter()
  const [isConfigured, setIsConfigured] = useState(false)
  const [purchaseController, setPurchaseController] = useState<TestingPurchaseController | null>(
    null,
  )
  const [forceUpdate, setForceUpdate] = useState(0) // To trigger re-renders when controller state changes

  const apiKey =
    Platform.OS === "ios"
      ? "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"
      : "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257"

  useEffect(() => {
    if (!purchaseController) {
      setPurchaseController(new TestingPurchaseController())
    }
  }, [purchaseController])

  const showFeatureDialog = () => {
    Alert.alert("Feature", "Feature triggered", [
      {
        text: "OK",
        onPress: () => console.log("Feature dialog dismissed"),
      },
    ])
  }

  const configureWithPC = async () => {
    try {
      if (!purchaseController) return

      await Superwall.configure({
        apiKey: apiKey,
        completion: async () => {
          const inactiveStatus: SubscriptionStatus = { status: "INACTIVE" }
          await Superwall.shared.setSubscriptionStatus(inactiveStatus)
          setIsConfigured(true)
        },
      })
    } catch (error) {
      console.error("Configuration with PC failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  const configureWithoutPC = async () => {
    try {
      await Superwall.configure({
        apiKey,
        completion: async () => {
          const inactiveStatus: SubscriptionStatus = { status: "INACTIVE" }
          await Superwall.shared.setSubscriptionStatus(inactiveStatus)
          setIsConfigured(true)
        },
      })
    } catch (error) {
      console.error("Configuration without PC failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  const triggerPaywall = async () => {
    try {
      await Superwall.shared.register({
        placement: "campaign_trigger",
        feature: () => {
          console.log("feature triggered")
          showFeatureDialog()
        },
      })
    } catch (error) {
      console.error("Failed to trigger paywall:", error)
      Alert.alert("Error", "Failed to trigger paywall")
    }
  }

  const togglePurchases = () => {
    if (purchaseController) {
      purchaseController.toggleRejectPurchase()
      setForceUpdate((prev) => prev + 1) // Force re-render
    }
  }

  const toggleRestore = () => {
    if (purchaseController) {
      purchaseController.toggleRestorePurchase()
      setForceUpdate((prev) => prev + 1) // Force re-render
    }
  }

  const resetStatus = async () => {
    try {
      const inactiveStatus: SubscriptionStatus = { status: "INACTIVE" }
      await Superwall.shared.setSubscriptionStatus(inactiveStatus)
      console.log("Status reset to inactive")
    } catch (error) {
      console.error("Failed to reset status:", error)
      Alert.alert("Error", "Failed to reset status")
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} testID="navigation-back-button">
          <Text>Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Mock PC Test</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <TestButton title="Configure with PC" onPress={configureWithPC} />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton title="Configure without PC" onPress={configureWithoutPC} />
        </View>

        {isConfigured && (
          <>
            <View style={styles.buttonContainer}>
              <TestButton title="Trigger Paywall" onPress={triggerPaywall} />
            </View>

            <View style={styles.buttonContainer}>
              <TestButton
                title={
                  purchaseController?.rejectPurchase ? "Enable purchases" : "Disable purchases"
                }
                onPress={togglePurchases}
              />
            </View>

            <View style={styles.buttonContainer}>
              <TestButton
                title={purchaseController?.restorePurchase ? "Disable restore" : "Enable restore"}
                onPress={toggleRestore}
              />
            </View>

            <View style={styles.buttonContainer}>
              <TestButton title="Reset status" onPress={resetStatus} />
            </View>
          </>
        )}
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingTop: 50,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#e0e0e0",
  },
  title: {
    fontSize: 18,
    fontWeight: "bold",
    marginLeft: 16,
  },
  content: {
    flexGrow: 1,
    justifyContent: "center",
    paddingHorizontal: 16,
  },
  buttonContainer: {
    marginVertical: 8,
  },
})
