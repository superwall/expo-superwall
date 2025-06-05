import * as Superwall from "expo-superwall"
import React from "react"
import { Alert, Button, Platform, ScrollView, StyleSheet, Text, View } from "react-native"
import { useRouter } from "expo-router"
import { TestingPurchaseController } from "./TestingPurchaseController"
import { RCPurchaseController } from "./RCPurchaseController"

export default function ConfigureTest() {
  const router = useRouter()
  
  const apiKey = Platform.OS === 'ios'
    ? 'pk_25605698906751f5383385f9976e21f840d44aa11cd4639c'
    : 'pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257'

  const showConfigurationCompletedDialog = () => {
    Alert.alert(
      'Success',
      'Configuration completed',
      [
        {
          text: 'OK',
          onPress: () => console.log('Configuration dialog dismissed')
        }
      ]
    )
  }

  const configureWithDialogAndPC = async () => {
    try {
      const options = {
        paywalls: {
          shouldPreload: false
        }
      }

      // Note: TestingPurchaseController would need to be implemented
      // For now, we'll configure without a purchase controller
      await Superwall.configure(apiKey, {
        options: options,
        completion: () => {
          showConfigurationCompletedDialog()
        }
      })
    } catch (error) {
      console.error('Configuration failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  const configureWithDialogNoPc = async () => {
    try {
      const options = {}
      
      await Superwall.configure(apiKey, {
        options: options,
        completion: () => {
          showConfigurationCompletedDialog()
        }
      })
    } catch (error) {
      console.error('Configuration failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  const configureWithDialogAndRC = async () => {
    try {
      const options = {}
      
      const purchaseController = new RCPurchaseController()
      
      await Superwall.configure(apiKey, {
        purchaseController: purchaseController,
        options: options,
        completion: () => {
          showConfigurationCompletedDialog()
        }
      })
    } catch (error) {
      console.error('Configuration failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  const justConfigure = async () => {
    try {
      const options = {}
      
      await Superwall.configure(apiKey, {
        options: options,
        completion: () => {
          console.log('Configuration completed without dialog')
        }
      })
    } catch (error) {
      console.error('Configuration failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Button title="← Back" onPress={() => router.back()} />
        <Text style={styles.title}>ConfigureTest</Text>
      </View>
      
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <Button
            title="Configure with dialog shown + PC"
            onPress={configureWithDialogAndPC}
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Configure with dialog shown + no PC"
            onPress={configureWithDialogNoPc}
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Configure with dialog shown and RC"
            onPress={configureWithDialogAndRC}
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Just configure"
            onPress={justConfigure}
          />
        </View>
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 50,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    marginLeft: 16,
  },
  content: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  buttonContainer: {
    marginVertical: 8,
  }
}) 