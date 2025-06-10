import * as Superwall from "expo-superwall"
import React, { useState, useRef } from "react"
import { 
  Alert, 
  FlatList, 
  Modal, 
  ScrollView, 
  StyleSheet, 
  Text, 
  View,
  TouchableOpacity
} from "react-native"
import { useRouter } from "expo-router"
import { TestDelegate } from "./TestDelegate"
import { TestButton } from "./TestButton"
import { SubscriptionStatusActive, SubscriptionStatusInactive, 
  TestDelegateEvent,
  DidDismissPaywallEvent,
  DidPresentPaywallEvent,
  HandleCustomPaywallActionEvent,
  HandleLogEvent,
  HandleSuperwallEventEvent,
  PaywallWillOpenDeepLinkEvent,
  PaywallWillOpenURLEvent,
  SubscriptionStatusDidChangeEvent,
  WillDismissPaywallEvent,
  WillPresentPaywallEvent
} from "expo-superwall"



export default function DelegateTest() {
  const router = useRouter()
  const [showEventsModal, setShowEventsModal] = useState(false)
  const [currentEventsList, setCurrentEventsList] = useState<TestDelegateEvent[]>([])
  const [modalTitle, setModalTitle] = useState('')
  const [forceUpdate, setForceUpdate] = useState(0)
  
  const testDelegateRef = useRef(new TestDelegate())
  
  const showSnackBar = (message: string) => {
    Alert.alert('Info', message)
  }

  const getEventName = (event: TestDelegateEvent): string => {
    switch (event.type) {
      case 'didDismissPaywall':
        return 'DidDismissPaywall'
      case 'didPresentPaywall':
        return 'DidPresentPaywall'
      case 'handleCustomPaywallAction':
        return 'HandleCustomPaywallAction'
      case 'handleLog':
        return 'HandleLog'
      case 'handleSuperwallEvent':
        return 'HandleSuperwallEvent'
      case 'paywallWillOpenDeepLink':
        return 'PaywallWillOpenDeepLink'
      case 'paywallWillOpenURL':
        return 'PaywallWillOpenURL'
      case 'subscriptionStatusDidChange':
        return 'SubscriptionStatusDidChange'
      case 'willDismissPaywall':
        return 'WillDismissPaywall'
      case 'willPresentPaywall':
        return 'WillPresentPaywall'
      default:
        return 'Unknown'
    }
  }

  const showDelegateEventsListDialog = (eventsList: TestDelegateEvent[], title: string) => {
    setCurrentEventsList(eventsList)
    setModalTitle(title)
    setShowEventsModal(true)
  }

  const setTestDelegate = async () => {
    try {
      await Superwall.setDelegate(testDelegateRef.current)
      showSnackBar('Test delegate set')
    } catch (error) {
      console.error('Failed to set delegate:', error)
      Alert.alert('Error', 'Failed to set delegate')
    }
  }

  const showPaywall = async () => {
    try {
      await Superwall.registerPlacement('campaign_trigger')
    } catch (error) {
      console.error('Failed to show paywall:', error)
      Alert.alert('Error', 'Failed to show paywall')
    }
  }

  const changeSubscriptionStatus = async () => {
    try {
      const activeStatus: SubscriptionStatusActive = {
        type: 'active',
        entitlements: [
          { id: 'pro' },
          { id: 'test_entitlement' }
        ]
      }
      await Superwall.setSubscriptionStatus(activeStatus)
    } catch (error) {
      console.error('Failed to change subscription status:', error)
      Alert.alert('Error', 'Failed to change subscription status')
    }
  }

  const clearDelegateAndChangeStatus = async () => {
    try {
      testDelegateRef.current.clearEvents()
      await Superwall.setDelegate(null)
      
      const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
      await Superwall.setSubscriptionStatus(inactiveStatus)
      
      setForceUpdate(prev => prev + 1)
      showSnackBar('Delegate cleared')
    } catch (error) {
      console.error('Failed to clear delegate:', error)
      Alert.alert('Error', 'Failed to clear delegate')
    }
  }

  const clearDelegateEvents = () => {
    testDelegateRef.current.clearEvents()
    setForceUpdate(prev => prev + 1)
    showSnackBar('Delegate events cleared')
  }

  const showDelegateEventsWithoutLog = () => {
    showDelegateEventsListDialog(
      testDelegateRef.current.eventsWithoutLog,
      'Delegate Events (without log)'
    )
  }

  const showDelegateEventsWithLog = () => {
    const logEvents = testDelegateRef.current.events.filter(
      event => event.type === 'handleLog'
    )
    showDelegateEventsListDialog(logEvents, 'Delegate Events (log only)')
  }

  const showEventsWithoutLogAndPresentation = () => {
    const filteredEvents = testDelegateRef.current.eventsWithoutLog.filter(
      event => 
        event.type !== 'willPresentPaywall' &&
        event.type !== 'didPresentPaywall' &&
        event.type !== 'willDismissPaywall' &&
        event.type !== 'didDismissPaywall' &&
        event.type !== 'handleSuperwallEvent'
    )
    showDelegateEventsListDialog(
      filteredEvents, 
      'Events (no log/presentation)'
    )
  }

  const renderEventItem = ({ item, index }: { item: TestDelegateEvent; index: number }) => {
    const eventName = getEventName(item)
    return (
      <View style={styles.eventItem}>
        <Text style={styles.eventText}>Event {index + 1}: {eventName}</Text>
      </View>
    )
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
      <Text onPress={() => router.back()}>← Back</Text>
      <Text style={styles.title}>Delegate Test</Text>
      </View>
      
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <TestButton
            title="Set Test Delegate"
            onPress={setTestDelegate}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Show Paywall"
            onPress={showPaywall}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Change Subscription Status"
            onPress={changeSubscriptionStatus}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Clear Delegate and Change Status"
            onPress={clearDelegateAndChangeStatus}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Clear Delegate Events"
            onPress={clearDelegateEvents}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Show Delegate Events without log"
            onPress={showDelegateEventsWithoutLog}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Show Delegate Events with log"
            onPress={showDelegateEventsWithLog}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Events without log and presentation"
            onPress={showEventsWithoutLogAndPresentation}
          />
        </View>

        {/* Event Count Display */}
        <View style={styles.statsContainer}>
          <Text style={styles.statsTitle}>Event Statistics</Text>
          <Text style={styles.statText}>
            Total Events: {testDelegateRef.current.getEventCount()}
          </Text>
          <Text style={styles.statText}>
            Events (no log): {testDelegateRef.current.getEventsWithoutLogCount()}
          </Text>
        </View>
      </ScrollView>

      {/* Events Modal */}
      <Modal
        visible={showEventsModal}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowEventsModal(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{modalTitle}</Text>
            <TouchableOpacity 
              style={styles.closeButton}
              onPress={() => setShowEventsModal(false)}
            >
              <Text style={styles.closeButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
          
          <FlatList
            data={currentEventsList}
            renderItem={renderEventItem}
            keyExtractor={(_, index) => index.toString()}
            style={styles.eventsList}
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyText}>No events recorded yet</Text>
              </View>
            }
          />
        </View>
      </Modal>
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
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  buttonContainer: {
    marginVertical: 8,
  },
  statsContainer: {
    marginTop: 20,
    padding: 16,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    backgroundColor: '#f9f9f9',
  },
  statsTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    textAlign: 'center',
  },
  statText: {
    fontSize: 14,
    marginVertical: 4,
  },
  modalContainer: {
    flex: 1,
    backgroundColor: '#fff',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 50,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  closeButton: {
    padding: 8,
  },
  closeButtonText: {
    color: '#007AFF',
    fontSize: 16,
  },
  eventsList: {
    flex: 1,
    paddingHorizontal: 16,
  },
  eventItem: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  eventText: {
    fontSize: 16,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  emptyText: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
}) 