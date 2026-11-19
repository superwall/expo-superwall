// Jest can't load the real react-native entry point (it's Flow), and the SDK only needs these
// few exports to be importable. Suites that render components mock what they use themselves.
module.exports = {
  Platform: { OS: "ios", select: (options) => options.ios ?? options.default },
  StyleSheet: {
    absoluteFill: { bottom: 0, left: 0, position: "absolute", right: 0, top: 0 },
    create: (styles) => styles,
  },
  View: "View",
}
