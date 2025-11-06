import { Link } from "expo-router"
import { View } from "react-native"

export default function OuterApp() {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 16 }}>
      <Link href="/compat">Compat SDK Demo</Link>
      <Link href="/new">Hooks SDK Demo</Link>
      <Link href="/custom-purchase">Custom Purchase Controller</Link>
    </View>
  )
}
