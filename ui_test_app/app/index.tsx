import * as Superwall from "expo-superwall"
import React, { useEffect } from "react"
import { Button, Text, View } from "react-native"
import { Link, useRouter } from "expo-router"

export default function App() {
  const router = useRouter()

  useEffect(() => {}, [])

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>Superwall Tests</Text>
      
      <Button
        title="Configuration Test"
        onPress={() => {
          router.push("/configureTest")
        }}
      />

      <Button
        title="Subscription Status Test" 
        onPress={() => {
          router.push("/subscriptionStatusTest")
        }}
      />

      <Button
        title="Purchase Controller Test"
        onPress={() => {
          router.push("/purchaseControllerTest")
        }}
      />

      <Button
        title="Delegate Test"
        onPress={() => {
          router.push("/delegateTest")
        }}
      />

      <Button
        title="Handler Test"
        onPress={() => {
          router.push("/handlerTest")
        }}
      />
    </View>
  )
}
