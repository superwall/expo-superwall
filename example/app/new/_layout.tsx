import { Stack } from "expo-router"
import { SuperwallProvider } from "expo-superwall"

export default function StackLayout() {
  return (
    <SuperwallProvider
      config={{
        ios: {
          apiKey: "pk_25605698906751f5383385f9976e21f840d44aa11cd4639c",
        },
      }}
    >
      <Stack />
    </SuperwallProvider>
  )
}
