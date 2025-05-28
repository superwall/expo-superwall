import { useEffect } from "react"
import { Text, View } from "react-native"

import { Link } from "expo-router"

export default function App() {
  useEffect(() => {}, [])

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text>Superwall Expo Example</Text>
      <Link href="/compat">Compat</Link>
      <Link href="/new">New</Link>
    </View>
  )
}
