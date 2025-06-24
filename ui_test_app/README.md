# Expo Superwall UI Test App 👋

This is an [Expo](https://expo.dev) application specifically for running UI tests for the `expo-superwall` SDK, likely using [Maestro](https://maestro.mobile.dev/). It is created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).

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

2. **Configure API Keys (if needed for tests)**

   Some UI tests might require displaying actual paywalls. If so, you may need to configure Superwall API keys.
   - Check the test setup or relevant files (e.g., `app/index.tsx` or test-specific configurations) for API key placeholders.
   - Replace them with your actual Superwall API keys if necessary.

3. **Start the app (for manual testing or inspection)**

   ```bash
   npx expo start
   # or
   yarn expo start
   # or
   bun expo start
   ```
   This will allow you to open the app in a development build, Android emulator, or iOS simulator.

## Running UI Tests (Maestro)

This app includes Maestro flows for UI testing, located in the `maestro/` directory.

1. **Install Maestro:** Follow the [Maestro installation guide](https://maestro.mobile.dev/getting-started/installing-maestro).

2. **Run tests:**
   Navigate to the `ui_test_app` directory and execute Maestro flows. For example:
   ```bash
   cd ui_test_app
   maestro test maestro/flow.yaml
   # Or specific test flows like:
   # maestro test maestro/delegate/flow.yaml
   ```
   (Consult the `maestro` directory and specific test files for exact commands and available flows.)

## Exploring the App Structure

This project uses [file-based routing](https://docs.expo.dev/router/introduction) via Expo Router.
- **`app/index.tsx`**: Main entry point, likely initializes Superwall for testing various scenarios.
- **`app/...` (other files)**: Contain specific screens or configurations for different test cases (e.g., `delegateTest.tsx`, `handlerTest.tsx`).
- **`maestro/`**: Contains Maestro UI test flow definitions.

## Learn more about Expo

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with our [guides](https://docs.expo.dev/guides).
- [Expo Router Documentation](https://docs.expo.dev/router/introduction/)

## Join the community

- [Superwall Discord Community](https://discord.gg/superwall): Chat with Superwall users and ask questions.
- [Expo Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.
