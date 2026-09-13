import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: {
          backgroundColor: '#667eea',
        },
        headerTintColor: '#fff',
        headerTitleStyle: {
          fontWeight: 'bold',
        },
      }}
    >
      <Stack.Screen
        name="index"
        options={{
          title: 'Superwall Demo Home',
          headerShown: false
        }}
      />
      <Stack.Screen
        name="compat"
        options={{
          title: 'Compat SDK Demo',
          headerShown: false
        }}
      />
      <Stack.Screen
        name="new"
        options={{
          title: 'Hooks SDK Demo',
          headerShown: false
        }}
      />
      <Stack.Screen
        name="custom-purchase"
        options={{
          title: 'Custom Purchase Controller',
          headerShown: true
        }}
      />
      <Stack.Screen
        name="integration-attributes"
        options={{
          title: 'Integration Attributes',
          headerShown: true
        }}
      />
      <Stack.Screen
        name="article-paywall"
        options={{
          title: 'Inline Article Paywall',
          headerShown: true
        }}
      />
      <Stack.Screen
        name="customer-info"
        options={{
          title: 'Customer Info',
          headerShown: true,
          headerBackButtonDisplayMode: 'minimal'
        }}
      />
    </Stack>
  );
}
