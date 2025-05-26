import {
  AndroidConfig,
  type ConfigPlugin,
  withAndroidManifest,
  withInfoPlist,
} from "expo/config-plugins"

const withApiKey: ConfigPlugin<{ apiKey: string }> = (config, { apiKey }) => {
  config = withInfoPlist(config, (config) => {
    config.modResults.SUPERWALL_API_KEY = apiKey
    return config
  })

  config = withAndroidManifest(config, (config) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults)

    AndroidConfig.Manifest.addMetaDataItemToMainApplication(
      mainApplication,
      "SUPERWALL_API_KEY",
      apiKey,
    )
    return config
  })

  return config
}

export default withApiKey
