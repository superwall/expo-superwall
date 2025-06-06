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
import { TestHandler, HandlerEvent } from "./TestHandler"
import { TestButton } from "./TestButton"

export default function HandlerTest() {
  const router = useRouter()
  const [featureBlockExecuted, setFeatureBlockExecuted] = useState(false)
  const [showEventsModal, setShowEventsModal] = useState(false)
  const [forceUpdate, setForceUpdate] = useState(0)
  
  const testHandlerRef = useRef(new TestHandler())
  
  const showSnackBar = (message: string) => {
    Alert.alert('Info', message)
  }

  const featureBlock = () => {
    setFeatureBlockExecuted(true)
    showSnackBar('Feature block executed!')
  }

  const testNonGatedPaywall = async () => {
    try {
      await Superwall.registerPlacement('non_gated_paywall', {
        handler: testHandlerRef.current.getHandler(),
        feature: featureBlock
      })
    } catch (error) {
      console.error('Failed to register non-gated paywall:', error)
      Alert.alert('Error', 'Failed to register non-gated paywall')
    }
  }

  const testGatedPaywall = async () => {
    try {
      await Superwall.registerPlacement('gated_paywall', {
        handler: testHandlerRef.current.getHandler(),
        feature: featureBlock
      })
    } catch (error) {
      console.error('Failed to register gated paywall:', error)
      Alert.alert('Error', 'Failed to register gated paywall')
    }
  }

  const testSkipAudience = async () => {
    try {
      await Superwall.registerPlacement('skip_audience', {
        handler: testHandlerRef.current.getHandler(),
        feature: featureBlock
      })
    } catch (error) {
      console.error('Failed to register skip audience:', error)
      Alert.alert('Error', 'Failed to register skip audience')
    }
  }

  const testErrorPlacement = async () => {
    try {
      await Superwall.registerPlacement('error_placement', {
        handler: testHandlerRef.current.getHandler(),
        feature: featureBlock
      })
    } catch (error) {
      console.error('Failed to register error placement:', error)
      Alert.alert('Error', 'Failed to register error placement')
    }
  }

  const dismissPaywall = async () => {
    try {
      await Superwall.dismiss()
    } catch (error) {
      console.error('Failed to dismiss paywall:', error)
      Alert.alert('Error', 'Failed to dismiss paywall')
    }
  }

  const clearHandlerEvents = () => {
    testHandlerRef.current.clearEvents()
    setFeatureBlockExecuted(false)
    setForceUpdate(prev => prev + 1) // Force re-render
    showSnackBar('Handler events cleared')
  }

  const showHandlerEventsDialog = () => {
    setShowEventsModal(true)
  }

  const renderEventItem = ({ item, index }: { item: HandlerEvent; index: number }) => {
    const eventName = testHandlerRef.current.getEventName(item)
    return (
      <View style={styles.eventItem}>
        <Text style={styles.eventText}>Event {index + 1}: {eventName}</Text>
      </View>
    )
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TestButton title="← Back" onPress={() => router.back()} />
        <Text style={styles.title}>Handler Test</Text>
      </View>
      
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.buttonContainer}>
          <TestButton
            title="Test Non-Gated Paywall"
            onPress={testNonGatedPaywall}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Test Gated Paywall"
            onPress={testGatedPaywall}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Test Skip Audience"
            onPress={testSkipAudience}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Test Error Placement"
            onPress={testErrorPlacement}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Dismiss Paywall"
            onPress={dismissPaywall}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Clear Handler Events"
            onPress={clearHandlerEvents}
          />
        </View>

        <View style={styles.buttonContainer}>
          <TestButton
            title="Show Handler Events"
            onPress={showHandlerEventsDialog}
          />
        </View>

        <View style={styles.resultsContainer}>
          <Text style={styles.resultsTitle}>Test Results</Text>
          <Text style={styles.resultText}>
            Feature Block Executed: {featureBlockExecuted ? "Yes" : "No"}
          </Text>
          <Text style={styles.resultText}>
            Event Count: {testHandlerRef.current.getEventCount()}
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
            <Text style={styles.modalTitle}>Handler Events</Text>
            <TouchableOpacity 
              style={styles.closeButton}
              onPress={() => setShowEventsModal(false)}
            >
              <Text style={styles.closeButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
          
          <FlatList
            data={testHandlerRef.current.events}
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
  resultsContainer: {
    marginTop: 20,
    padding: 16,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    backgroundColor: '#f9f9f9',
  },
  resultsTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    textAlign: 'center',
  },
  resultText: {
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