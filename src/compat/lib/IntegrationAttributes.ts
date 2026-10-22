/**
 * Third-party integration providers supported by Superwall.
 * Use these values with `setIntegrationAttributes()` to link user IDs from
 * attribution and analytics platforms.
 */
export type IntegrationAttribute =
  | "adjustId"
  | "amplitudeDeviceId"
  | "amplitudeUserId"
  | "appsflyerId"
  | "brazeAliasName"
  | "brazeAliasLabel"
  | "onesignalId"
  | "fbAnonId"
  | "firebaseAppInstanceId"
  | "iterableUserId"
  | "iterableCampaignId"
  | "iterableTemplateId"
  | "mixpanelDistinctId"
  | "mparticleId"
  | "clevertapId"
  | "airshipChannelId"
  | "kochavaDeviceId"
  | "tenjinId"
  | "posthogUserId"
  | "customerioId"
  | "appstackId"
  /**
   * The Airbridge Device ID, from `Airbridge.fetchDeviceUUID`.
   *
   * **Android only.** On iOS this key is currently ignored by `setIntegrationAttributes()`.
   */
  | "airbridgeId"

/**
 * Object mapping integration attribute keys to their values.
 * Used with `setIntegrationAttributes()` to set third-party provider IDs.
 */
export type IntegrationAttributes = {
  [K in IntegrationAttribute]?: string
}
