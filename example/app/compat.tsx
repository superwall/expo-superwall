import { useEffect } from "react"
import { Button, Linking, Platform, Text, View } from "react-native"
import Superwall from "superwall-expo/compat"
import { MySuperwallDelegate } from "../lib/compat/SuperwallDelegate"

import { Link } from "expo-router"

const delegate = new MySuperwallDelegate()

export default function Compat() {
  useEffect(() => {
    const apiKey =
      Platform.OS === "ios"
        ? "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c"
        : "MY_ANDROID_API_KEY"
    Superwall.configure({
      apiKey: apiKey,
    })

    Superwall.shared.identify({ userId: "abc" })
    Superwall.shared.setDelegate(delegate)
    Superwall.shared.setUserAttributes({ test: "abc" })

    // Get the initial URL if the app was launched from a link
    Linking.getInitialURL().then((url) => {
      if (url) {
        Superwall.shared.handleDeepLink(url)
      }
    })

    // Listen for URL events
    const linkingListener = Linking.addEventListener("url", (event) => {
      Superwall.shared.handleDeepLink(event.url)
    })

    // Clean up the event listener
    return () => {
      linkingListener.remove()
    }
  }, [])

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Link href="/">Home</Link>
      <Text>Superwall Expo Compat</Text>

      <Button
        title="Open Paywall"
        onPress={() =>
          Superwall.shared.register({
            placement: "fishing",
            feature() {
              console.log("Feature called")
            },
          })
        }
      />
    </View>
  )
}
