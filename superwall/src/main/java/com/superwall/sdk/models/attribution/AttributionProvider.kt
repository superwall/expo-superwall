package com.superwall.sdk.models.attribution

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An enum that represents the available third-party attribution providers that Superwall can integrate with.
 * These providers are used to track user attribution data and link it to paywall events and purchases.
 * */
@Serializable
enum class AttributionProvider(
    val rawName: String,
) {
    /** The unique Adjust identifier for the user. */
    @SerialName("adjustId")
    ADJUST_ID("adjustId"),

    /** The Amplitude device identifier. */
    @SerialName("amplitudeDeviceId")
    AMPLITUDE_DEVICE_ID("amplitudeDeviceId"),

    /** The Amplitude user identifier. */
    @SerialName("amplitudeUserId")
    AMPLITUDE_USER_ID("amplitudeUserId"),

    /** The unique Appsflyer identifier for the user. */
    @SerialName("appsflyerId")
    APPSFLYER_ID("appsflyerId"),

    /** The Braze `alias_name` in User Alias Object. */
    @SerialName("brazeAliasName")
    BRAZE_ALIAS_NAME("brazeAliasName"),

    /** The Braze `alias_label` in User Alias Object. */
    @SerialName("brazeAliasLabel")
    BRAZE_ALIAS_LABEL("brazeAliasLabel"),

    /** The OneSignal Player identifier for the user. */
    @SerialName("onesignalId")
    ONESIGNAL_ID("onesignalId"),

    /** The Facebook Anonymous identifier for the user. */
    @SerialName("fbAnonId")
    FB_ANON_ID("fbAnonId"),

    /** The Firebase instance identifier. */
    @SerialName("firebaseAppInstanceId")
    FIREBASE_APP_INSTANCE_ID("firebaseAppInstanceId"),

    /** The Iterable identifier for the user. */
    @SerialName("iterableUserId")
    ITERABLE_USER_ID("iterableUserId"),

    /** The Iterable campaign identifier. */
    @SerialName("iterableCampaignId")
    ITERABLE_CAMPAIGN_ID("iterableCampaignId"),

    /** The Iterable template identifier. */
    @SerialName("iterableTemplateId")
    ITERABLE_TEMPLATE_ID("iterableTemplateId"),

    /** The Mixpanel user identifier. */
    @SerialName("mixpanelDistinctId")
    MIXPANEL_DISTINCT_ID("mixpanelDistinctId"),

    /** The unique mParticle user identifier (mpid). */
    @SerialName("mparticleId")
    MPARTICLE_ID("mparticleId"),

    /** The CleverTap user identifier. */
    @SerialName("clevertapId")
    CLEVERTAP_ID("clevertapId"),

    /** The Airship channel identifier for the user. */
    @SerialName("airshipChannelId")
    AIRSHIP_CHANNEL_ID("airshipChannelId"),

    /** The unique Kochava device identifier. */
    @SerialName("kochavaDeviceId")
    KOCHAVA_DEVICE_ID("kochavaDeviceId"),

    /** The Tenjin identifier. */
    @SerialName("tenjinId")
    TENJIN_ID("tenjinId"),

    /** The PostHog User identifer. */
    @SerialName("posthogUserId")
    POSTHOG_USER_ID("posthogUserId"),

    /** The Customer.io person's identifier (`id`). */
    @SerialName("customerioId")
    CUSTOMERIO_ID("customerioId"),

    @SerialName("meta")
    META("meta"),

    @SerialName("amplitude")
    AMPLITUDE("amplitude"),

    @SerialName("mixpanel")
    MIXPANEL("mixpanel"),

    @SerialName("google_ads")
    GOOGLE_ADS("google_ads"),

    @SerialName("google_app_set_id")
    GOOGLE_APP_SET("google_app_set_id"),

    @SerialName("custom")
    CUSTOM("custom"),
}
