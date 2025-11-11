package expo.modules.superwallexpo.json

import com.superwall.sdk.models.attribution.AttributionProvider

fun attributionProviderFromString(provider: String): AttributionProvider? {
  return when (provider) {
    "adjustId" -> AttributionProvider.ADJUST_ID
    "amplitudeDeviceId" -> AttributionProvider.AMPLITUDE_DEVICE_ID
    "amplitudeUserId" -> AttributionProvider.AMPLITUDE_USER_ID
    "appsflyerId" -> AttributionProvider.APPSFLYER_ID
    "brazeAliasName" -> AttributionProvider.BRAZE_ALIAS_NAME
    "brazeAliasLabel" -> AttributionProvider.BRAZE_ALIAS_LABEL
    "onesignalId" -> AttributionProvider.ONESIGNAL_ID
    "fbAnonId" -> AttributionProvider.FB_ANON_ID
    "firebaseAppInstanceId" -> AttributionProvider.FIREBASE_APP_INSTANCE_ID
    "iterableUserId" -> AttributionProvider.ITERABLE_USER_ID
    "iterableCampaignId" -> AttributionProvider.ITERABLE_CAMPAIGN_ID
    "iterableTemplateId" -> AttributionProvider.ITERABLE_TEMPLATE_ID
    "mixpanelDistinctId" -> AttributionProvider.MIXPANEL_DISTINCT_ID
    "mparticleId" -> AttributionProvider.MPARTICLE_ID
    "clevertapId" -> AttributionProvider.CLEVERTAP_ID
    "airshipChannelId" -> AttributionProvider.AIRSHIP_CHANNEL_ID
    "kochavaDeviceId" -> AttributionProvider.KOCHAVA_DEVICE_ID
    "tenjinId" -> AttributionProvider.TENJIN_ID
    "posthogUserId" -> AttributionProvider.POSTHOG_USER_ID
    "customerioId" -> AttributionProvider.CUSTOMERIO_ID
    else -> null
  }
}
