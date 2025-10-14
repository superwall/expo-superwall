package com.superwall.sdk.paywall.presentation.internal.request

import com.superwall.sdk.models.events.EventData

// EventData should be defined in Kotlin before this.
// Assuming it is defined, and it contains a property 'name'

sealed class PresentationInfo {
    data class ImplicitTrigger(
        override val eventData: EventData,
    ) : PresentationInfo()

    data class ExplicitTrigger(
        override val eventData: EventData,
    ) : PresentationInfo()

    data class FromIdentifier(
        override val identifier: String,
        override val freeTrialOverride: Boolean,
    ) : PresentationInfo()

    open val freeTrialOverride: Boolean?
        get() =
            when (this) {
                is FromIdentifier -> freeTrialOverride
                else -> null
            }

    open val eventData: EventData?
        get() =
            when (this) {
                is ImplicitTrigger -> eventData
                is ExplicitTrigger -> eventData
                else -> null
            }

    val eventName: String?
        get() =
            when (this) {
                is ImplicitTrigger -> eventData.name
                is ExplicitTrigger -> eventData.name
                else -> null
            }

    open val identifier: String?
        get() =
            when (this) {
                is FromIdentifier -> identifier
                else -> null
            }
}
