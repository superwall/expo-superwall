import { useRouter } from "expo-router"
import Superwall, { type SubscriptionStatus } from "expo-superwall/compat"
import { Entitlement } from "expo-superwall/compat/lib/Entitlement"
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native"
import { TestButton } from "./TestButton"

export default function SubscriptionStatusTest() {
  const router = useRouter()

  const showSubscriptionStatusDialog = (status: SubscriptionStatus) => {
    let content = ""

    if (status.status === "ACTIVE") {
      const entitlementIds = status.entitlements.map((e) => e.id).join(", ")
      content = `Subscription status: Active - Entitlements: ${entitlementIds}`
    } else if (status.status === "INACTIVE") {
      content = "Subscription status: Inactive"
    } else if (status.status === "UNKNOWN") {
      content = "Subscription status: Unknown"
    } else {
      content = `Subscription status: ${status}`
    }

    Alert.alert("Success", content, [
      {
        text: "OK",
        onPress: () => console.log("Subscription status dialog dismissed"),
      },
    ])
  }

  const setSubscriptionStatusActive = async () => {
    try {
      // Create active subscription status with pro and test_entitlement
      const activeStatus: SubscriptionStatus.Active = {
        status: "ACTIVE",
        entitlements: [new Entitlement("pro"), new Entitlement("test_entitlement")],
      }

      // Set the subscription status
      await Superwall.shared.setSubscriptionStatus(activeStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(activeStatus)
    } catch (error) {
      console.error("Failed to set active subscription status:", error)
      Alert.alert("Error", "Failed to set subscription status")
    }
  }

  const setSubscriptionStatusInactive = async () => {
    try {
      // Create inactive subscription status
      const inactiveStatus: SubscriptionStatus = {
        status: "INACTIVE",
      }

      // Set the subscription status
      await Superwall.shared.setSubscriptionStatus(inactiveStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(inactiveStatus)
    } catch (error) {
      console.error("Failed to set inactive subscription status:", error)
      Alert.alert("Error", "Failed to set subscription status")
    }
  }

  const setSubscriptionStatusUnknown = async () => {
    try {
      // Create unknown subscription status
      const unknownStatus: SubscriptionStatus = {
        status: "UNKNOWN",
      }

      // Set the subscription status
      await Superwall.shared.setSubscriptionStatus(unknownStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(unknownStatus)
    } catch (error) {
      console.error("Failed to set unknown subscription status:", error)
      Alert.alert("Error", "Failed to set subscription status")
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} testID="navigation-back-button">
          <Text>Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Subscription Status Test</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <TestButton
            title="Set Subscription Status Active"
            onPress={setSubscriptionStatusActive}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Set Subscription Status Inactive"
            onPress={setSubscriptionStatusInactive}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Set Subscription Status Unknown"
            onPress={setSubscriptionStatusUnknown}
          />
        </View>
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
