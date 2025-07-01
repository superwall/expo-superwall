#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

function getExpoVersion() {
  try {
    // Try to find expo package.json in node_modules
    const possiblePaths = [
      path.join(process.cwd(), 'node_modules', 'expo', 'package.json'),
      path.join(process.cwd(), '..', '..', 'expo', 'package.json'), // When installed as a dependency
      path.join(__dirname, '..', '..', 'expo', 'package.json'), // Alternative path
    ];

    for (const expoPackagePath of possiblePaths) {
      if (fs.existsSync(expoPackagePath)) {
        const expoPackage = JSON.parse(fs.readFileSync(expoPackagePath, 'utf8'));
        return expoPackage.version;
      }
    }

    // If expo is not found in node_modules, try to get it from the project's package.json
    const projectPackagePath = path.join(process.cwd(), 'package.json');
    if (fs.existsSync(projectPackagePath)) {
      const projectPackage = JSON.parse(fs.readFileSync(projectPackagePath, 'utf8'));
      const expoVersion = projectPackage.dependencies?.expo || projectPackage.devDependencies?.expo;
      if (expoVersion && expoVersion !== '*' && !expoVersion.includes('workspace:')) {
        // Extract version number from version string (e.g., "^52.0.0" -> "52.0.0")
        const versionMatch = expoVersion.match(/\d+\.\d+\.\d+/);
        return versionMatch ? versionMatch[0] : null;
      }
    }

    return null;
  } catch (error) {
    return null;
  }
}

function checkExpoVersion() {
  const expoVersion = getExpoVersion();
  
  if (!expoVersion) {
    console.warn('\n⚠️  Warning: Could not determine Expo SDK version.');
    console.warn('Make sure you have Expo SDK 53 or later installed.\n');
    return;
  }

  const majorVersion = parseInt(expoVersion.split('.')[0], 10);
  
  if (majorVersion < 53) {
    console.error('\n❌ Error: expo-superwall requires Expo SDK 53 or later.');
    console.error(`You are currently using Expo SDK ${majorVersion}.`);
    console.error('\nTo fix this issue:');
    console.error('1. Upgrade to Expo SDK 53 or later:');
    console.error('   npx expo install expo@latest');
    console.error('\n2. Or use the legacy React Native Superwall SDK for older Expo versions:');
    console.error('   npm uninstall expo-superwall');
    console.error('   npm install @superwall/react-native-superwall');
    console.error('\nFor more information, visit: https://github.com/superwall/expo-superwall\n');
    process.exit(1);
  }
}

// Only run the check if this script is executed directly (not during development)
if (process.env.NODE_ENV !== 'development' && !process.env.SKIP_EXPO_VERSION_CHECK) {
  checkExpoVersion();
}