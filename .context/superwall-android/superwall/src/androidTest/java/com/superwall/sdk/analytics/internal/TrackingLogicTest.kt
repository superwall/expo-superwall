package com.superwall.sdk.analytics.internal

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.Purchase
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.identity.IdentityInfo
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.models.enrichment.Enrichment
import com.superwall.sdk.network.Network
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.storage.LastPaywallView
import com.superwall.sdk.storage.LatestEnrichment
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.ReviewCount
import com.superwall.sdk.storage.ReviewData
import com.superwall.sdk.storage.TotalPaywallViews
import com.superwall.sdk.store.abstractions.transactions.StoreTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class TrackingLogicTest {
    val store =
        mockk<LocalStorage> {
            every { apiKey } returns "pk_test_1234"
            every { didTrackFirstSession } returns true
            every { didTrackFirstSeen } returns true
            every { read(LastPaywallView) } returns null
            every { read(TotalPaywallViews) } returns 0
            every { read(LatestEnrichment) } returns Enrichment.stub()
            every { read(ReviewData) } returns ReviewCount(0)
        }
    val network = mockk<Network>()

    @Test
    fun should_clean_up_attributes() =
        runTest(timeout = 5.minutes) {
            val ctx = InstrumentationRegistry.getInstrumentation().context
            Superwall.configure(ctx.applicationContext as Application, "pk_test_1234", null, null, null, null)
            val deviceHelper =
                spyk(
                    DeviceHelper(
                        ctx,
                        storage = store,
                        network = network,
                        factory =
                            object : DeviceHelper.Factory {
                                override suspend fun makeIdentityInfo(): IdentityInfo = IdentityInfo("aliasId", "appUserId")

                                override fun makeLocaleIdentifier(): String? = "en-US"

                                override suspend fun makeStoreTransaction(transaction: Purchase): StoreTransaction = mockk()

                                override suspend fun activeProductIds(): List<String> = emptyList()

                                override suspend fun makeIdentityManager(): IdentityManager = mockk()

                                override fun experimentalProperties(): Map<String, Any> = emptyMap()

                                override fun makeSuperwallOptions(): SuperwallOptions = SuperwallOptions()
                            },
                    ),
                ) {
                    every { appVersion } returns "123"
                }
            val attributes = deviceHelper.getTemplateDevice()
            val event = InternalSuperwallEvent.DeviceAttributes(HashMap(attributes))
            val res = TrackingLogic.processParameters(event, "appSessionId")

            assert(
                lazyMessage = { "Lists should be cleaned" },
                value = res.audienceFilterParams.none { it.value is List<*> },
            )
            assert(
                lazyMessage = { "Booleans should be serialized as booleans" },
                value = res.audienceFilterParams["\$is_standard_event"] == true,
            )
            assert(
                lazyMessage = { "Double should be serialized as double" },
                value = res.audienceFilterParams["\$totalPaywallViews"] == 0.0,
            )
        }
}
