import * as Superwall from "expo-superwall"
import React from "react"
import { Alert, ScrollView, StyleSheet, Text, View } from "react-native"
import { useRouter } from "expo-router"
import { TestButton } from "./TestButton"
import { SubscriptionStatus, SubscriptionStatusActive, SubscriptionStatusInactive, SubscriptionStatusUnknown } from "expo-superwall"

export default function SubscriptionStatusTest() {
  const router = useRouter()

  const showSubscriptionStatusDialog = (status: SubscriptionStatus) => {
    let content = ''
    
    if (status.type === 'active') {
      const entitlementIds = status.entitlements.map(e => e.id).join(', ')
      content = `Subscription status: Active - Entitlements: ${entitlementIds}`
    } else if (status.type === 'inactive') {
      content = 'Subscription status: Inactive'
    } else if (status.type === 'unknown') {
      content = 'Subscription status: Unknown'
    } else {
      content = `Subscription status: ${status}`
    }

    Alert.alert(
      'Success',
      content,
      [
        {
          text: 'OK',
          onPress: () => console.log('Subscription status dialog dismissed')
        }
      ]
    )
  }

  const setSubscriptionStatusActive = async () => {
    try {
      // Create active subscription status with pro and test_entitlement
      const activeStatus: SubscriptionStatusActive = {
        type: 'active',
        entitlements: [
          { id: 'pro' },
          { id: 'test_entitlement' }
        ]
      }

      // Set the subscription status
      await Superwall.setSubscriptionStatus(activeStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(activeStatus)
    } catch (error) {
      console.error('Failed to set active subscription status:', error)
      Alert.alert('Error', 'Failed to set subscription status')
    }
  }

  const setSubscriptionStatusInactive = async () => {
    try {
      // Create inactive subscription status
      const inactiveStatus: SubscriptionStatusInactive = {
        type: 'inactive'
      }

      // Set the subscription status
      await Superwall.setSubscriptionStatus(inactiveStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(inactiveStatus)
    } catch (error) {
      console.error('Failed to set inactive subscription status:', error)
      Alert.alert('Error', 'Failed to set subscription status')
    }
  }

  const setSubscriptionStatusUnknown = async () => {
    try {
      // Create unknown subscription status
      const unknownStatus: SubscriptionStatusUnknown = {
        type: 'unknown'
      }

      // Set the subscription status
      await Superwall.setSubscriptionStatus(unknownStatus)

      // Show dialog with the status
      showSubscriptionStatusDialog(unknownStatus)
    } catch (error) {
      console.error('Failed to set unknown subscription status:', error)
      Alert.alert('Error', 'Failed to set subscription status')
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
      <Text onPress={() => router.back()}>← Back</Text>
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