import { useEffect } from "react"
import { Button, Text, View } from "react-native"
import * as Superwall from "superwall-expo"

export default function App() {
  useEffect(() => {
    Superwall.addPaywallListener((event) => {
      console.log(event)
    })
  }, [])

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text>Superwall Expo Example XD</Text>
      <Text>Api Key: {Superwall.getApiKey()}</Text>
      <Button
        title="Register"
        onPress={async () => {
          const test = await Superwall.registerPlacement("test")
          console.log(test)
        }}
      />
    </View>
  )
}
