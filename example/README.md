# Expo Superwall Example App 👋

This is an [Expo](https://expo.dev) project demonstrating the features of the `expo-superwall` SDK. It is created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).

## Get started

1. **Install dependencies**

   Use your preferred package manager:
   ```bash
   npm install
   # or
   yarn install
   # or
   bun install
   ```

2. **Configure API Keys**

   Before running the app, you need to add your Superwall API keys.
   - Open `app/index.tsx`.
   - Replace `"YOUR_SUPERWALL_API_KEY"` with your actual iOS Superwall API key.
   - If you are testing on Android, add your Android API key as well.

   ```tsx
   // Inside app/index.tsx
   export default function App() {
     return (
       <SuperwallProvider apiKeys={{ ios: "YOUR_SUPERWALL_API_KEY" /* android: "YOUR_ANDROID_API_KEY" */ }}>
         {/* ... app content ... */}
       </SuperwallProvider>
     );
   }
   ```

3. **Start the app**

   ```bash
   npx expo start
   # or
   yarn expo start
   # or
   bun expo start
   ```

   In the output, you'll find options to open the app in a development build, Android emulator, or iOS simulator. [Expo Go](https://expo.dev/go) can also be used, but development builds are recommended for full native module compatibility.

## Exploring the Example

This project uses [file-based routing](https://docs.expo.dev/router/introduction) via Expo Router.
- **`app/index.tsx`**: Initializes `SuperwallProvider` and shows basic loading states.
- **`app/new.tsx`**: Demonstrates the new Hooks-based SDK (`useUser`, `usePlacement`).
- **`app/compat.tsx`**: Demonstrates the compatibility layer SDK (legacy `Superwall.shared` usage).

You can start developing by editing these files or adding new routes within the `app` directory.

## Learn more about Expo

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with our [guides](https://docs.expo.dev/guides).
- [Learn Expo tutorial](https://docs.expo.dev/tutorial/introduction/): Follow a step-by-step tutorial where you'll create a project that runs on Android, iOS, and the web.
- [Expo Router Documentation](https://docs.expo.dev/router/introduction/)

## Join the community

Join our community of developers creating universal apps.

- [Expo on GitHub](https://github.com/expo/expo): View our open source platform and contribute.
- [Superwall Discord Community](https://discord.gg/superwall): Chat with Superwall users and ask questions.
- [Expo Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.
