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
  /** iOS only. The Firebase Installation ID. Ignored on Android. */
  | "firebaseInstallationId"
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
  | "singularDeviceId"
  /**
   * The Airbridge Device ID, from `Airbridge.fetchDeviceUUID`.
   *
   * On iOS, it is kept when you reset or identify a different user.
   */
  | "airbridgeId"
  /** Android only. The Meta (Facebook) ID. Ignored on iOS. */
  | "meta"
  /** Android only. The Amplitude ID. Ignored on iOS. */
  | "amplitude"
  /** Android only. The Mixpanel ID. Ignored on iOS. */
  | "mixpanel"
  /** Android only. The Google Ads ID. Ignored on iOS. */
  | "googleAds"
  /** Android only. The Google App Set ID. Ignored on iOS. */
  | "googleAppSetId"
  /** Android only. A custom attribution ID. Ignored on iOS. */
  | "custom"

/**
 * Object mapping integration attribute keys to their values.
 * Used with `setIntegrationAttributes()` to set third-party provider IDs.
 * Attributes you leave out keep their current value. Set one to `null` to remove it.
 */
export type IntegrationAttributes = {
  [K in IntegrationAttribute]?: string | null
}
