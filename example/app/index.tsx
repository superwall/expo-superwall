import { Link } from "expo-router"
import { View } from "react-native"

export default function OuterApp() {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Link href="/compat">Compat</Link>
      <Link href="/new">New</Link>
    </View>
  )
}
