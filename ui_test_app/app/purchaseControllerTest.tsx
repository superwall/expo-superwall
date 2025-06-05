import * as Superwall from "expo-superwall"
import React, { useState, useEffect } from "react"
import { Alert, Button, Platform, ScrollView, StyleSheet, Text, View } from "react-native"
import { useRouter } from "expo-router"
import { TestingPurchaseController } from "./TestingPurchaseController"

interface SubscriptionStatusInactive {
  type: 'inactive'
}

export default function PurchaseControllerTest() {
  const router = useRouter()
  const [isConfigured, setIsConfigured] = useState(false)
  const [purchaseController, setPurchaseController] = useState<TestingPurchaseController | null>(null)
  const [forceUpdate, setForceUpdate] = useState(0) // To trigger re-renders when controller state changes

  const apiKey = Platform.OS === 'ios'
    ? 'pk_25605698906751f5383385f9976e21f840d44aa11cd4639c'
    : 'pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257'

  useEffect(() => {
    if (!purchaseController) {
      setPurchaseController(new TestingPurchaseController())
    }
  }, [])

  const showFeatureDialog = () => {
    Alert.alert(
      'Feature',
      'Feature triggered',
      [
        {
          text: 'OK',
          onPress: () => console.log('Feature dialog dismissed')
        }
      ]
    )
  }

  const configureWithPC = async () => {
    try {
      if (!purchaseController) return

      const options = {
        paywalls: {
          shouldPreload: false
        }
      }

      await Superwall.configure(apiKey, {
        purchaseController: purchaseController,
        options: options,
        completion: async () => {
          const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
          await Superwall.setSubscriptionStatus(inactiveStatus)
          setIsConfigured(true)
        }
      })
    } catch (error) {
      console.error('Configuration with PC failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  const configureWithoutPC = async () => {
    try {
      const options = {
        paywalls: {
          shouldPreload: false
        }
      }

      await Superwall.configure(apiKey, {
        options: options,
        completion: async () => {
          const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
          await Superwall.setSubscriptionStatus(inactiveStatus)
          setIsConfigured(true)
        }
      })
    } catch (error) {
      console.error('Configuration without PC failed:', error)
      Alert.alert('Error', 'Configuration failed')
    }
  }

  const triggerPaywall = async () => {
    try {
      await Superwall.registerPlacement('campaign_trigger', {
        feature: () => {
          console.log("feature triggered")
          showFeatureDialog()
        }
      })
    } catch (error) {
      console.error('Failed to trigger paywall:', error)
      Alert.alert('Error', 'Failed to trigger paywall')
    }
  }

  const togglePurchases = () => {
    if (purchaseController) {
      purchaseController.toggleRejectPurchase()
      setForceUpdate(prev => prev + 1) // Force re-render
    }
  }

  const toggleRestore = () => {
    if (purchaseController) {
      purchaseController.toggleRestorePurchase()
      setForceUpdate(prev => prev + 1) // Force re-render
    }
  }

  const resetStatus = async () => {
    try {
      const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
      await Superwall.setSubscriptionStatus(inactiveStatus)
      console.log('Status reset to inactive')
    } catch (error) {
      console.error('Failed to reset status:', error)
      Alert.alert('Error', 'Failed to reset status')
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Button title="← Back" onPress={() => router.back()} />
        <Text style={styles.title}>Mock PC Test</Text>
      </View>
      
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <Button
            title="Configure with PC"
            onPress={configureWithPC}
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Configure without PC"
            onPress={configureWithoutPC}
          />
        </View>

        {isConfigured && (
          <>
            <View style={styles.buttonContainer}>
              <Button
                title="Trigger Paywall"
                onPress={triggerPaywall}
              />
            </View>

            <View style={styles.buttonContainer}>
              <Button
                title={purchaseController?.rejectPurchase ? 'Enable purchases' : 'Disable purchases'}
                onPress={togglePurchases}
              />
            </View>

            <View style={styles.buttonContainer}>
              <Button
                title={purchaseController?.restorePurchase ? 'Disable restore' : 'Enable restore'}
                onPress={toggleRestore}
              />
            </View>

            <View style={styles.buttonContainer}>
              <Button
                title="Reset status"
                onPress={resetStatus}
              />
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