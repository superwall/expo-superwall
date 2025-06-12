import { useRouter } from "expo-router"
import Superwall, { PaywallOptions, SuperwallOptions } from "expo-superwall/compat"
import { Alert, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native"
import { TestButton } from "./TestButton"
import { TestingPurchaseController } from "./TestingPurchaseController"

export default function ConfigureTest() {
  const router = useRouter()

  const apiKey =
    Platform.OS === "ios"
      ? "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"
      : "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257"

  const showConfigurationCompletedDialog = () => {
    Alert.alert("Success", "Configuration completed", [
      {
        text: "OK",
        onPress: () => console.log("Configuration dialog dismissed"),
      },
    ])
  }

  const configureWithDialogAndPC = async () => {
    try {
      const purchaseController = new TestingPurchaseController()

      const options = new SuperwallOptions({
        paywalls: new PaywallOptions({
          shouldPreload: false,
        }),
      })

      await Superwall.configure({
        apiKey: apiKey,
        purchaseController: purchaseController,
        options: options,
        completion: () => {
          console.log("Configuration completed with dialog shown and PC")
          showConfigurationCompletedDialog()
        },
      })
    } catch (error) {
      console.error("Configuration failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  const configureWithDialogNoPc = async () => {
    try {
      await Superwall.configure({
        apiKey,
        completion: () => {
          console.log("Configuration completed with dialog shown and no PC")
          showConfigurationCompletedDialog()
        },
      })
    } catch (error) {
      console.error("Configuration failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  const configureWithDialogAndRC = async () => {
    try {
      await Superwall.configure({
        apiKey,
        completion: () => {
          console.log("CALLED COMPLETION")
          console.log("Configuration completed with dialog shown and RC")
          showConfigurationCompletedDialog()
        },
      })

      console.log("Configuration completed with dialog shown and RC")
    } catch (error) {
      console.error("Configuration failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  const justConfigure = async () => {
    try {
      await Superwall.configure({
        apiKey,
        completion: () => {
          console.log("Configuration completed without dialog")
        },
      })
    } catch (error) {
      console.error("Configuration failed:", error)
      Alert.alert("Error", "Configuration failed")
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} testID="navigation-back-button">
          <Text>Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>ConfigureTest</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <TestButton title="Configure with dialog shown + PC" onPress={configureWithDialogAndPC} />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Configure with dialog shown + no PC"
            onPress={configureWithDialogNoPc}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Configure with dialog shown and RC"
            onPress={configureWithDialogAndRC}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton title="Just configure" onPress={justConfigure} />
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
