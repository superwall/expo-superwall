package com.superwall.sdk

import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.events.EventData

class RuleAttributeFactoryBuilder : RuleAttributesFactory {
    override suspend fun makeRuleAttributes(
        event: EventData?,
        computedPropertyRequests: List<ComputedPropertyRequest>,
    ): Map<String, Any> =
        mapOf(
            "user" to
                mapOf(
                    "id" to "123",
                    "email" to "test@gmail.com",
                ),
        )
}
