import * as Superwall from "expo-superwall/compat"
import React, { useEffect } from "react"
import { Text, View } from "react-native"
import { Link, useRouter } from "expo-router"
import { TestButton } from "./TestButton"


export default function App() {
  const router = useRouter()

  useEffect(() => {}, [])

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>Superwall Tests</Text>
      
      <TestButton
        title="Configuration Test"
        onPress={() => {
          router.push("/configureTest")
        }}
      />

      <TestButton
        title="Subscription Status Test" 
        onPress={() => {
          router.push("/subscriptionStatusTest")
        }}
      />

      <TestButton
        title="Purchase Controller Test"
        onPress={() => {
          router.push("/purchaseControllerTest")
        }}
      />

      <TestButton
        title="Delegate Test"
        onPress={() => {
          router.push("/delegateTest")
        }}
      />

      <TestButton
        title="Handler Test"
        onPress={() => {
          router.push("/handlerTest")
        }}
      />
    </View>
  )
}
