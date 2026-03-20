module.exports = {
  ...require("expo-module-scripts/jest-preset-plugin"),
  modulePathIgnorePatterns: ["<rootDir>/build/"],
  moduleNameMapper: {
    "^react$": "<rootDir>/example/node_modules/react",
    "^react/jsx-runtime$": "<rootDir>/example/node_modules/react/jsx-runtime",
    "^react/jsx-dev-runtime$": "<rootDir>/example/node_modules/react/jsx-dev-runtime",
  },
  testPathIgnorePatterns: ["<rootDir>/build/", "<rootDir>/example/", "<rootDir>/ui_test_app/"],
}
