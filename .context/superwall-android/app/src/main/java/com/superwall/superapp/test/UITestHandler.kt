package com.superwall.superapp.test

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEvent.DeepLink
import com.superwall.sdk.config.models.ConfigurationStatus
import com.superwall.sdk.identity.identify
import com.superwall.sdk.identity.setUserAttributes
import com.superwall.sdk.misc.AlertControllerFactory
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.paywall.presentation.PaywallPresentationHandler
import com.superwall.sdk.paywall.presentation.dismiss
import com.superwall.sdk.paywall.presentation.get_paywall.getPaywall
import com.superwall.sdk.paywall.presentation.get_presentation_result.getPresentationResult
import com.superwall.sdk.paywall.presentation.register
import com.superwall.sdk.paywall.presentation.result.PresentationResult
import com.superwall.sdk.paywall.view.SuperwallPaywallActivity
import com.superwall.sdk.view.fatalAssert
import com.superwall.superapp.ComposeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

object UITestHandler {
    object WebTests {
        val tests =
            listOf(
                UITestInfo(
                    1000,
                    "Purchase an entitlement from the web and redeem it as anonymous user. Paywall wont show if redemption succeeded.",
                    test = { scope, events, _ ->
                        Log.e("Registering event", "show_if_web_failed")
                        Log.e(
                            "Entitlements are",
                            Superwall.instance.entitlements.active
                                .map {
                                    "${it.id}"
                                }.joinToString(separator = ", "),
                        )
                        Superwall.instance.register(placement = "show_if_web_failed")
                    },
                ),
                UITestInfo(
                    1001,
                    "Purchase an entitlement from the web and redeem it as anonymous user. Call identify and confirm entitlement has been migrated. Paywall wont show if redemption succeeded.",
                    test = { scope, events, _ ->
                        Superwall.instance.identify("MigratedWebUser")
                        Log.e("Registering event", "show_if_web_failed")
                        delay(5.seconds)
                        Log.e(
                            "Entitlements are",
                            Superwall.instance.entitlements.active
                                .map {
                                    "${it.id}}"
                                }.joinToString(separator = ", "),
                        )
                        Superwall.instance.register(placement = "show_if_web_failed")
                    },
                ),
                UITestInfo(
                    1001,
                    "Purchase an entitlement from the web and redeem it as anonymous user." +
                        "Call identify and confirm entitlement has been migrated (paywall shows)." +
                        "Call reset and confirm entitlement was removed.",
                    test = { scope, events, _ ->
                        Superwall.instance.identify("MigratedWebUser")
                        Log.e("Registering event", "show_if_web_failed")
                        delay(5.seconds)
                        Log.e(
                            "Entitlements are",
                            Superwall.instance.entitlements.active
                                .map {
                                    "${it.id}"
                                }.joinToString(separator = ", "),
                        )
                        delay(5.seconds)
                        Superwall.instance.reset()
                        Log.e(
                            "Entitlements are",
                            Superwall.instance.entitlements.active
                                .map {
                                    "${it.id}"
                                }.joinToString(separator = ", "),
                        )
                        delay(5.seconds)
                        Superwall.instance.register(placement = "show_if_web_failed")
                    },
                ),
                UITestInfo(
                    1002,
                    "Reset",
                    test = { scope, events, _ ->
                        Superwall.instance.reset()
                    },
                ),
                UITestInfo(
                    1003,
                    "Identify",
                    test = { scope, events, _ ->
                        Log.e("Registering event", "identify before ${Superwall.instance.userId}")
                        Superwall.instance.identify("MigratedWebUser")
                        Log.e("Registering event", "identify after ${Superwall.instance.userId}")
                    },
                ),
                UITestInfo(
                    1004,
                    "Start paywall, will identify. Hit restore, should dismiss paywall",
                    test = { scope, events, _ ->
                        Superwall.instance.register(placement = "pro_only")
                        Superwall.instance.identify("MigratedWebUser")
                        Log.e("Registering event", "identify after ${Superwall.instance.userId}")
                    },
                ),
                UITestInfo(
                    1005,
                    "Start paywall anonymously and hit restore. Should ask to open on web.",
                    test = { scope, events, _ ->
                        Superwall.instance.register(placement = "pro_only")
                        Log.e("Registering event", "identify after ${Superwall.instance.userId}")
                    },
                ),
                UITestInfo(
                    1006,
                    "Try handling a VALID link with invalid code.",
                    test = { scope, events, _ ->
                        Superwall.handleDeepLink("superapp://superwall/redeem?code=redeem_testing123".toUri())?.let {
                            Log.e("Output", "IS HANDLED: ${it.getOrThrow()}")
                        }
                    },
                ),
                UITestInfo(
                    1006,
                    "Try handling an INVALID link with invalid code.",
                    test = { scope, events, _ ->
                        Superwall.handleDeepLink("superapp://superwall/dontredeem?code=abcd".toUri())?.let {
                            Log.e("Output", "IS HANDLED: ${it.getOrThrow()}")
                        }
                        Log.e("Registering event", "identify after ${Superwall.instance.userId}")
                    },
                ),
            )
    }

    var test0Info =
        UITestInfo(
            0,
            "Uses the identify function. Should see the name 'Jack' in the paywall.",
            test = { scope, events, _ ->
                Log.e("Registering event", "present_data")
                Superwall.instance.identify(userId = "test0")
                Superwall.instance.setUserAttributes(attributes = mapOf("first_name" to "Jack"))
                Superwall.instance.register(placement = "present_data")
                Log.e("Registering event", "done")
            },
        )

    var test1Info =
        UITestInfo(
            1,
            "Uses the identify function. Should see the name 'Kate' in the paywall.",
            test = { scope, events, _ ->
                // Set identity
                Superwall.instance.identify(userId = "test1a")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))
                scope.launch {

                    // Set new identity
                    Superwall.instance.identify(userId = "test1b")
                    Superwall.instance.setUserAttributes(mapOf("first_name" to "Kate"))
                    Superwall.instance.register(placement = "present_data")
                }
            },
        )
    var test2Info =
        UITestInfo(
            2,
            "Calls `reset()`. No first name should be displayed.",
            test = { scope, events, _ ->
                // TODO: The name doesn't get set to begin with so isn't an accurate test.
                // Set identity
                Superwall.instance.identify(userId = "test2")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))

                Superwall.instance.reset()
                Superwall.instance.register(placement = "present_data")
            },
        )
    var test3Info =
        UITestInfo(
            3,
            "Calls `reset()` multiple times. No first name should be displayed.",
            test = { scope, events, _ ->
                // Set identity
                Superwall.instance.identify(userId = "test3")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))

                Superwall.instance.reset()
                Superwall.instance.reset()
                Superwall.instance.register(placement = "present_data")
            },
        )
    var test4Info =
        UITestInfo(
            4,
            "This paywall will open with a video playing that shows a 0 in the video at" +
                "t0 and a 2 in the video at t2. It will close after 4 seconds. A new paywall " +
                "will be presented 1 second after close. This paywall should have a video " +
                "playing and should be started from the beginning with a 0 on the screen. ",
            test = { scope, events, _ ->
                // Present the paywall.
                Superwall.instance.register(placement = "present_video")
                // Dismiss after 4 seconds
                events.first { it is SuperwallEvent.PaywallWebviewLoadComplete }
                delay(4.seconds)
                Superwall.instance.dismiss()
                delay(4.seconds)
                Superwall.instance.register(placement = "present_video")
            },
        )
    var test5Info =
        UITestInfo(
            5,
            "Show paywall with override products. Paywall should appear with 2 products:" +
                "1 monthly at \$12.99 and 1 annual at \$99.99.",
            test = { scope, events, _ ->
                // TODO: Need to get some products from google play console and substitute in.
            },
        )
    var test6Info =
        UITestInfo(
            6,
            "Paywall should appear with 2 products: 1 monthly at \$4.99 and 1 annual at" +
                " \$29.99.",
            test = { scope, events, _ ->
                // TODO: This doesn't have the products that it should have - need to add to
                //  google play console
                Superwall.instance.register(placement = "present_products")
            },
        )
    var test7Info =
        UITestInfo(
            7,
            "Adds a user attribute to verify rule on `present_and_rule_user` presents: " +
                "user.should_display == true and user.some_value > 12. Then dismisses and removes " +
                "those attributes. Make sure it's not presented.",
            test = { scope, events, _ ->
                Superwall.instance.identify(userId = "test7")
                Superwall.instance.setUserAttributes(
                    mapOf(
                        "first_name" to "Charlie",
                        "should_display" to true,
                        "some_value" to 14,
                    ),
                )
                Superwall.instance.register(placement = "present_and_rule_user")

                delay(8000)
                Superwall.instance.dismiss()

                // Remove those attributes.
                Superwall.instance.setUserAttributes(
                    mapOf(
                        "should_display" to null,
                        "some_value" to null,
                    ),
                )
                Superwall.instance.register(placement = "present_and_rule_user")
            },
        )
    var test8Info =
        UITestInfo(
            8,
            "Adds a user attribute to verify rule on `present_and_rule_user`. Verify it" +
                " DOES NOT present: user.should_display == true and user.some_value > 12",
            test = { scope, events, _ ->
                // TODO: Crashes on no rule match
                Superwall.instance.identify(userId = "test7")
                Superwall.instance.setUserAttributes(
                    mapOf(
                        "first_name" to "Charlie",
                        "should_display" to true,
                        "some_value" to 12,
                    ),
                )
                Superwall.instance.register(placement = "present_and_rule_user")
            },
        )
    var test9Info =
        UITestInfo(
            9,
            "Sets subs status to active, paywall should present regardless of this," +
                " then it sets the status back to inactive.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement(
                                "test",
                            ),
                        ),
                    ),
                )
                Superwall.instance.register(placement = "present_always")
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
            },
        )
    var test10Info =
        UITestInfo(
            10,
            "Paywall should appear with 2 products: 1 monthly at \$4.99 and 1 annual at " +
                "\$29.99. After dismiss, paywall should be presented again with override " +
                "products: 1 monthly at \$12.99 and 1 annual at \$99.99. After dismiss, paywall " +
                "should be presented again with no override products. After dismiss, paywall " +
                "should be presented one last time with no override products.",
            test = { scope, events, _ ->
                // TODO: Product substitution
            },
        )
    var test11Info =
        UITestInfo(
            11,
            "Paywall should present with the name Claire. Then it should dismiss after" +
                "8 seconds and present again without any name. Then it should present again" +
                " with the name Sawyer.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Claire"))
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                Superwall.instance.register(placement = "present_data")
                events.first { it is SuperwallEvent.ShimmerViewComplete }
                // Dismiss any view controllers
                delay(4.seconds)

                // Dismiss any views
                Superwall.instance.dismiss()
                Superwall.instance.setUserAttributes(mapOf("first_name" to null))
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                Superwall.instance.register(placement = "present_data")
                events.first { it is SuperwallEvent.PaywallOpen }
                delay(10.seconds)
                // Dismiss any views
                Superwall.instance.dismiss()
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Sawyer"))
                Superwall.instance.register(placement = "present_data")
            },
        )
    var test12Info =
        UITestInfo(
            12,
            "Test trigger: off. Paywall shouldn't present. Should print eventNotFound.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "keep_this_trigger_off")
            },
        )
    var test13Info =
        UITestInfo(
            13,
            "Test trigger: not in the dashboard. Paywall shouldn't present.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "i_just_made_this_up_and_it_dne")
            },
        )
    var test14Info =
        UITestInfo(
            14,
            "Presents the paywall and then dismisses after 8 seconds. The paywall shouldn't " +
                "display based on a paywall_close event.",
            test = { scope, events, _ ->
                // Show a paywall
                Superwall.instance.register(placement = "present_always")
                events.first { it is SuperwallEvent.ShimmerViewComplete }

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()
            },
        )
    var test15Info =
        UITestInfo(
            15,
            "Clusterfucks by Jake™. One paywall should present, then it should disappear" +
                " then another paywall should present and disappear. Then a third should present. " +
                "Verify that the console output contains a non-null experimentId. After the third " +
                "presentation, manually close the paywall. During this test only 3 paywalls should " +
                "event present. If more than 3 present, this test has failed.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "present_always")
                Superwall.instance.register(
                    placement = "present_always",
                    params = mapOf("some_param_1" to "hello"),
                )
                Superwall.instance.register(placement = "present_always")

                delay(5000)

                // Dismiss any views
                Superwall.instance.dismiss()

                Superwall.instance.register(placement = "present_always")
                Superwall.instance.identify(userId = "1111")
                Superwall.instance.register(placement = "present_always")

                delay(5000)

                // Dismiss any views
                Superwall.instance.dismiss()

                var handler = PaywallPresentationHandler()
                handler.onPresent { info ->
                    val experimentId = info.experiment?.id ?: ""
                    println("!!! TEST 15 !!! experimentId: $experimentId")
                    Superwall.instance.register(placement = "present_always")
                }

                Superwall.instance.register(placement = "present_always", handler = handler)
            },
        )
    var test16Info =
        UITestInfo(
            16,
            "The paywall should present. In the console you should see a paywall_open event " +
                "from the delegate followed by !!! TEST 16 !!!.",
            test = { scope, events, _ ->
                var presentTriggered = false
                val paywallPresentationHandler = PaywallPresentationHandler()
                paywallPresentationHandler.onPresent { info ->
                    presentTriggered = true
                    println("!!! TEST 16 !!! DID PRESENT PAYWALL")
                }

                Superwall.instance.register(
                    "present_always",
                    null,
                    paywallPresentationHandler,
                )
                events.first { it is SuperwallEvent.PaywallWebviewLoadComplete }
                scope.launch {
                    delay(1.seconds)
                    assert(presentTriggered)
                }
            },
        )
    var test17Info =
        UITestInfo(
            17,
            "Clusterfucks by Jake™. This presents a paywall with the name Jack. Then it " +
                "dismisses after 8s. Then another paywall will present with no name. Then" +
                " the paywall will dismiss after 8s and one more paywall will display. After " +
                "the third presentation, manually close the paywall. During this test only 3 " +
                "paywalls should event present. If more than 3 present, this test has failed.",
            test = { scope, events, _ ->
                Superwall.instance.identify(userId = "test0")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))
                Superwall.instance.register(placement = "present_data")

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()

                // Set identity
                Superwall.instance.identify(userId = "test2")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))

                // Reset the user identity
                Superwall.instance.reset()

                Superwall.instance.register(placement = "present_data")

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()

                // Present paywall
                Superwall.instance.register(placement = "present_always")
                Superwall.instance.register(
                    placement = "present_always",
                    params = mapOf("some_param_1" to "hello"),
                )
                Superwall.instance.register(placement = "present_always")
            },
        )
    var test18Info =
        UITestInfo(
            18,
            "Open In-App browser from a manually presented paywall. Once the in-app " +
                "browser opens, close it, and verify that the paywall is still showing.",
            test = { scope, events, _ ->

                // Create a mock paywall view
                val delegate = MockPaywallViewDelegate()

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(placement = "present_urls", delegate = delegate)

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(
                    context = this@UITestInfo,
                    view = view.getOrThrow(),
                )
            },
        )
    var test19Info =
        UITestInfo(
            19,
            "Clusterfucks by Jake™. Ths presents a paywall with no name. Then it dismisses" +
                " after 8s. Then it presents again with no name, dismisses, and finally presents " +
                "with the name Kate.",
            test = { scope, events, _ ->
                // Set identity
                Superwall.instance.identify(userId = "test19a")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))

                Superwall.instance.reset()
                Superwall.instance.reset()
                Superwall.instance.register(placement = "present_data")

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()

                Superwall.instance.getPresentationResult(placement = "present_and_rule_user")

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()

                // Show a paywall
                Superwall.instance.register(placement = "present_always")

                delay(8000)

                // Dismiss any views
                Superwall.instance.dismiss()

                // Set identity
                Superwall.instance.identify(userId = "test19b")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Jack"))

                // Set new identity
                Superwall.instance.identify(userId = "test19c")
                Superwall.instance.setUserAttributes(mapOf("first_name" to "Kate"))
                Superwall.instance.register(placement = "present_data")
            },
        )
    var test20Info =
        UITestInfo(
            20,
            "Verify that external URLs can be opened in native browser from paywall. When" +
                " the paywall opens, tap \"Perform\" for \"Open in Safari\". Afterwards, go back " +
                "and verify the paywall is still displayed.",
            test = { scope, events, _ ->
                // Present paywall with URLs
                Superwall.instance.register("present_urls")
            },
        )
    var test21Info =
        UITestInfo(
            21,
            "Present the paywall and make a purchase. After purchasing completes and " +
                "the paywall dismisses, attempt to launch again. The paywall should NOT appear.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "present_data")
            },
        )
    var test22Info =
        UITestInfo(
            22,
            "This is skipped! (Track an event shortly after another one is beginning to present. The " +
                "session should not be cancelled out.)",
            test = { scope, events, _ ->
                // TODO: This is skipped in the iOS SDK for now
            },
        )
    var test23Info =
        UITestInfo(
            23,
            "Case: Unsubscribed user, register event without a gating handler\n" +
                "Result: paywall should display",
            test = { scope, events, _ ->
                // Register event
                Superwall.instance.register(placement = "register_nongated_paywall")
            },
        )
    var test24Info =
        UITestInfo(
            24,
            "Case: Subscribed user, register event without a gating handler\n" +
                "Result: paywall should NOT display. Resets subscription status to inactive " +
                "4s later.",
            test = { scope, events, _ ->
                // Set user as subscribed
                Superwall.instance.setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement(
                                "pro",
                            ),
                        ),
                    ),
                )
                // Register event - paywall shouldn't appear.
                Superwall.instance.register(placement = "register_nongated_paywall")
                scope.launch {
                    events.first { it is SuperwallEvent.SubscriptionStatusDidChange }
                    delay(4000)
                    Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                }
            },
        )
    var test25Info =
        UITestInfo(
            25,
            "Tapping the button shouldn't present a paywall. These register calls don't " +
                "have a feature gate. Differs from iOS in that there is no purchase taking place.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement(
                                "pro",
                            ),
                        ),
                    ),
                )
                // Try to present paywall again
                Superwall.instance.register(placement = "register_nongated_paywall")
                scope.launch {

                    delay(4000)
                }
            },
        )
    var test26Info =
        UITestInfo(
            26,
            "Registers an event with a gating handler. The paywall should display, you should " +
                "NOT see an alert when you close the paywall.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                Superwall.instance.register(placement = "register_gated_paywall") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )
    var test27Info =
        UITestInfo(
            27,
            "Tapping the button shouldn't present the paywall but should launch the " +
                "feature block - an alert should present.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus("pro")
                Superwall.instance.register(placement = "register_gated_paywall") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
                delay(1000)
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
            },
        )
    var test28Info =
        UITestInfo(
            28,
            "Should print out \"Paywall(experiment...)\".",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                scope.launch {
                    val result = Superwall.instance.getPresentationResult("present_data")
                    val resOrNull = result.getOrNull()
                    fatalAssert(
                        result.isSuccess && (resOrNull is PresentationResult.Paywall && resOrNull.experiment.id == "12859"),
                        "Expected Paywall(experiment...), got $result",
                    )
                    println("!!! TEST 28 !!! $result")
                }
            },
        )
    var test29Info =
        UITestInfo(
            29,
            "Should print out \"noRuleMatch\".",
            test = { scope, events, _ ->
                Superwall.instance.setUserAttributes(
                    mapOf(
                        "should_display" to null,
                        "some_value" to null,
                    ),
                )

                val result = Superwall.instance.getPresentationResult("present_and_rule_user")
                println("!!! TEST 29 !!! $result")
                fatalAssert(
                    result.getOrNull() is PresentationResult.NoAudienceMatch,
                    "NoRuleMatch expected, received $result",
                )
            },
        )
    var test30Info =
        UITestInfo(
            30,
            "Should print out \"eventNotFound\".",
            test = { scope, events, _ ->
                scope.launch {

                    val result =
                        Superwall.instance.getPresentationResult("some_random_not_found_event")
                    fatalAssert(
                        result.getOrNull() is PresentationResult.PlacementNotFound,
                        "NoRuleMatch expected, received $result",
                    )
                    println("!!! TEST 30 !!! $result")
                }
            },
        )
    var test31Info =
        UITestInfo(
            31,
            "Should print out \"holdout\".",
            test = { scope, events, _ ->
                scope.launch {

                    val result = Superwall.instance.getPresentationResult("holdout")
                    fatalAssert(
                        result.getOrNull() is PresentationResult.Holdout,
                        "NoRuleMatch expected, received $result",
                    )
                    println("!!! TEST 31 !!! $result")
                }
            },
        )
    var test32Info =
        UITestInfo(
            32,
            "This sets the subscription status active, prints out \"userIsSubscribed\" " +
                "and then returns subscription status to inactive.",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement(
                                "test",
                            ),
                        ),
                    ),
                )
                scope.launch {
                    val result = Superwall.instance.getPresentationResult("present_data")
                    fatalAssert(
                        result.getOrNull() is PresentationResult.NoAudienceMatch,
                        "UserIsSubscribed expected, received $result",
                    )
                    println("!!! TEST 32 !!! $result")
                    Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                }
            },
        )
    var test33Info =
        UITestInfo(
            33,
            "Calls identify twice with the same ID before presenting a paywall",
            test = { scope, events, _ ->
                // Set identity
                Superwall.instance.identify(userId = "test33")
                Superwall.instance.identify(userId = "test33")

                Superwall.instance.register(placement = "present_data")
            },
        )
    var test34Info =
        UITestInfo(
            34,
            "Call reset 8s after a paywall is presented – should not cause a crash.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "present_data")

                scope.launch {
                    events.first { it is SuperwallEvent.PaywallWebviewLoadComplete }
                    delay(8000)
                    // Call reset while it is still on screen
                    Superwall.instance.reset()
                }
            },
        )
    var test35Info =
        UITestInfo(
            35,
            "Purchase from the paywall and then check that after the purchase has finished " +
                "the result type `purchased` is printed to the console. The paywall should dismiss." +
                " After doing this, try test 37",
            test = { scope, events, _ ->
                // Create a mock paywall view
                val delegate = MockPaywallViewDelegate()
                delegate.paywallViewFinished { paywallView, paywallResult, shouldDismiss ->
                    println("!!! TEST 35 !!! Result: $paywallResult, shouldDismiss: $shouldDismiss, paywallVc: $paywallView")
                }

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(placement = "present_data", delegate = delegate)

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(context = this, view = view.getOrThrow())
            },
        )
    var test36Info =
        UITestInfo(
            36,
            "Close the paywall and check that after the purchase has finished \" " +
                "\"the result type \"declined\" is printed to the console. The paywall should close.",
            test = { scope, events, _ ->
                // Create a mock paywall view
                val delegate = MockPaywallViewDelegate()
                delegate.paywallViewFinished { paywallView, paywallResult, shouldDismiss ->
                    println("!!! TEST 36 !!! Result: $paywallResult, shouldDismiss: $shouldDismiss, paywallVc: $paywallView")
                }

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(placement = "present_data", delegate = delegate)

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(
                    context = this,
                    view = view.getOrThrow(),
                )
            },
        )
    var test37Info =
        UITestInfo(
            37,
            "Need to have purchased a product before calling this test, then present the " +
                "paywall and tap \"restore\". The paywall should dismiss and the the console should" +
                "print the paywallResult as \"restored\".",
            test = { scope, events, _ ->
                // Create a mock paywall view
                val delegate = MockPaywallViewDelegate()
                delegate.paywallViewFinished { paywallView, paywallResult, shouldDismiss ->
                    println("!!! TEST 37 !!! Result: $paywallResult, shouldDismiss: $shouldDismiss, paywallVc: $paywallView")
                }

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(placement = "restore", delegate = delegate)

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(context = this, view = view.getOrThrow())
            },
        )

    // TODO: Test 38 & 39, & 40 need to be able to present modally and swipe to dismiss implemented.

    // Warning: Change `subscribed` param to product id
    private suspend fun Context.executeRegisterFeatureClosureTest(
        subscribed: Boolean,
        gated: Boolean,
        messagePipe: MutableSharedFlow<Any?>,
    ) {
        val currentSubscriptionStatus = Superwall.instance.entitlements.status.value

        if (subscribed) {
            // Set user subscribed
            Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Active(setOf(Entitlement("test"))))
        }

        // Determine gating event
        val placement =
            if (gated) {
                "register_gated_paywall"
            } else {
                "register_nongated_paywall"
            }

        val paywallPresentationHandler = PaywallPresentationHandler()
        paywallPresentationHandler.onError { error ->
            CoroutineScope(Dispatchers.IO).launch {
                messagePipe.emit(false)
            }
            println("!!! ERROR HANDLER !!! $error")
        }

        Superwall.instance.register(placement, null, paywallPresentationHandler) {
            val alertController =
                AlertControllerFactory.make(
                    context = this,
                    title = "Feature Launched",
                    message = "The feature block was called",
                    actionTitle = "Ok",
                )
            alertController.show()
        }

        delay(4000)

        if (subscribed) {
            // Reset status
            Superwall.instance.setSubscriptionStatus(currentSubscriptionStatus)
        }
    }

    var test41Info =
        UITestInfo(
            41,
            "Unable to fetch config, not subscribed, and not gated. First disable " +
                "internet on device. You should not be subscribed. You SHOULD " +
                "see !!! ERROR HANDLER !!! in the console and the alert should NOT show.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = false,
                    gated = false,
                    messagePipe = message,
                )
            },
        )
    var test42Info =
        UITestInfo(
            42,
            "Unable to fetch config, not subscribed, and gated. First disable internet " +
                "on device. You should not be subscribed.  You SHOULD " +
                "see !!! ERROR HANDLER !!! in the console and the alert should NOT show.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = false,
                    gated = true,
                    messagePipe = message,
                )
            },
        )
    var test43Info =
        UITestInfo(
            43,
            "Unable to fetch config, subscribed, and not gated. First disable internet on " +
                "device. You should NOT see !!! ERROR HANDLER !!! in the console and the alert " +
                "SHOULD show.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = true,
                    gated = false,
                    messagePipe = message,
                )
            },
        )
    var test44Info =
        UITestInfo(
            44,
            "Unable to fetch config, subscribed, and gated. First disable internet on " +
                "device. You should NOT see !!! ERROR HANDLER !!! in the console and the alert " +
                "SHOULD show.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = true,
                    gated = true,
                    messagePipe = message,
                )
            },
        )
    var test45Info =
        UITestInfo(
            45,
            "Fetched config, not subscribed, and not gated. The paywall should show. On " +
                "paywall dismiss you should NOT see !!! ERROR HANDLER !!! in the console and the " +
                "alert should show when you dismiss the paywall.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = false,
                    gated = false,
                    messagePipe = message,
                )
                delay(4000)
            },
        )
    var test46Info =
        UITestInfo(
            46,
            "Fetched config, not subscribed, and gated. The paywall should show. You should " +
                "NOT see !!! ERROR HANDLER !!! in the console and the alert should NOT show on " +
                "paywall dismiss.",
            test = { scope, events, message ->
                executeRegisterFeatureClosureTest(
                    subscribed = false,
                    gated = true,
                    messagePipe = message,
                )
                delay(4000)
            },
        )
    var test47Info =
        UITestInfo(
            47,
            "Fetched config, subscribed, and not gated. The paywall should NOT show. You " +
                "should NOT see !!! ERROR HANDLER !!! in the console and the alert SHOULD show.",
            test = { scope, events, message ->
                Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
                delay(1000)
                executeRegisterFeatureClosureTest(
                    subscribed = true,
                    gated = false,
                    messagePipe = message,
                )
            },
        )
    var test48Info =
        UITestInfo(
            48,
            "Fetched config, subscribed, and gated. The paywall should NOT show. You should" +
                " NOT see !!! ERROR HANDLER !!! in the console and the alert SHOULD show.",
            test = { scope, events, message ->
                Superwall.instance.configurationStateListener.first { it is ConfigurationStatus.Configured }
                delay(1000)
                executeRegisterFeatureClosureTest(
                    subscribed = true,
                    gated = true,
                    messagePipe = message,
                )
                delay(4000)
            },
        )

    // Test 49 is combined with test 53.

    var test50Info =
        UITestInfo(
            50,
            "Change the API Key to the SessionStart UITest app. Clean install app and the " +
                "paywall should show. The button does nothing.",
            test =
                { scope, events, _ -> },
        )
    var test52Info =
        UITestInfo(
            52,
            "Change the API Key to the AppInstall UITest app. Then restart the app and " +
                "a paywall should show when the app is launched from a cold start. The button " +
                "does nothing.",
            test = { scope, events, _ -> },
        )
    var test53Info =
        UITestInfo(
            53,
            "This covers test 49 too. Change the API Key to the AppLaunch UITest app. " +
                "Then restart the app and a paywall should show when the app is launched from a " +
                "cold start. Also should happen from a clean install. The button does nothing.",
            test = { scope, events, _ -> },
        )
    var test56Info =
        UITestInfo(
            56,
            "The debugger should open when tapping on link. Currently the debugger isn't " +
                "implemented.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is DeepLink -> {
                            println("!!! TEST 56 !!! Result: Deep link event received successfully.")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                val url =
                    Uri.parse(
                        "superapp://?superwall_debug=true&paywall_id=7872&token=sat_eyJhbGciOiJIUzI1NiJ9.eyJzY29wZXMiOlt7InNjb3BlIjoicGF5d2FsbF9wcmV2aWV3IiwiYXBwbGljYXRpb25JZCI6MTI3MH1dLCJpYXQiOjE2ODg2MjgxNTIsImV4cCI6NTA2NTI4Nzg3MiwiYXVkIjoicHduIiwiaXNzIjoicHduIiwic3ViIjoiNzAifQ.J0QNaycFlGY8ZQGBUwrySxkX43iPH2iV646EvJ5TvCg",
                    )
                Superwall.handleDeepLink(url)
            },
        )
    var test57Info =
        UITestInfo(
            57,
            "Change API key to DeepLink. Present paywall from implicit trigger: " +
                "`deepLink_open`. Verify the `Deep link event received successfully.` in the" +
                " console.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is DeepLink -> {
                            println("!!! TEST 57 !!! Result: Deep link event received successfully.")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                // Handle the URL
                val url = Uri.parse("superapp://mydeepLink?isDeepLink=true")
                val handled = Superwall.handleDeepLink(url)
            },
        )
    var test58Info =
        UITestInfo(
            58,
            "Change API key to TransactionAbandon. Then, present paywall, try to purchase, " +
                "then abandon the transaction. Another paywall will present. In the console " +
                "you'll see !!! TEST 58 !!!",
            test = { scope, events, _ ->
                Superwall.instance.register("campaign_trigger")
            },
        )
    var test59Info =
        UITestInfo(
            59,
            "Delete and reinstall app. Change API key to PaywallDecline. Present paywall, " +
                "dismiss it and a survey will present. Choose Option 1. The survey will dismiss, " +
                "then survey_response will be printed in the console. The paywall will dismiss " +
                "and PaywallDecline will be printed in the console. Then a paywall should auto " +
                "present after the decline.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.PaywallDecline -> {
                            println("!!! TEST 59 !!! PaywallDecline")
                        }

                        is SuperwallEvent.SurveyResponse -> {
                            println("!!! TEST 59 !!! SurveyResponse")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("campaign_trigger")
            },
        )
    var test60Info =
        UITestInfo(
            60,
            "Change API key to TransactionFail. Inside RevenueCatPurchaseController, " +
                "comment out everything in the purchase function catch except " +
                "PurchaseResult.Failed(e). Then present paywall, go to purchase and cancel the " +
                "purchase. This will trigger a transaction_fail and it will dismiss the paywall " +
                "and present another paywall. In the console you'll see !!! TEST 60 !!!.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.TransactionFail -> {
                            println("!!! TEST 60 !!! TransactionFail.")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("campaign_trigger")
            },
        )
    var test62Info =
        UITestInfo(
            62,
            "Verify that an invalid URL like `#` doesn't crash the app. Manually tap on" +
                "the \"Open in-app #\" button.",
            test = { scope, events, _ ->
                // Present paywall with URLs
                Superwall.instance.register(placement = "present_urls")
            },
        )

    var test63Info =
        UITestInfo(
            63,
            "Don't have an active subscription, present paywall, tap restore. Check " +
                "the \"No Subscription Found\" alert pops up.",
            test = { scope, events, _ ->
                // Create a mock paywall view
                val delegate = MockPaywallViewDelegate()
                delegate.paywallViewFinished { paywallView, paywallResult, shouldDismiss ->
                    println("!!! TEST 37 !!! Result: $paywallResult, shouldDismiss: $shouldDismiss, paywallVc: $paywallView")
                }

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(placement = "restore", delegate = delegate)

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(context = this, view = view.getOrThrow())
            },
        )
    var test64Info =
        UITestInfo(
            64,
            "Delete and reinstall app. Present paywall then tap close button. A survey will " +
                "show. Choose non-other option. The paywall will close with !!! TEST 63 !!! and " +
                "PaywallClose and SurveyResponse printed to the console. The feature block will fire. " +
                "Then open and close the paywall again to make sure survey doesn't show again. " +
                "The console will just print out !!! TEST 63!!! with PaywallClose  and the feature " +
                "block will fire again.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.PaywallClose -> {
                            println("!!! TEST 64 !!! PaywallClose")
                        }

                        is SuperwallEvent.SurveyResponse -> {
                            println("!!! TEST 64 !!! SurveyResponse")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("show_survey_with_other") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )
    var test65Info =
        UITestInfo(
            65,
            "Delete and reinstall app. Present paywall then tap close button. A survey will " +
                "show. Choose the other option and type \"Test\" and tap Submit. The console " +
                "will print out !!! TEST 65!!! with PaywallClose and again with SurveyResponse. " +
                "The feature block will fire.",
            test = { scope, events, message ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.PaywallClose -> {
                            scope.launch {
                                message.emit(eventInfo.placement)
                            }
                        }

                        is SuperwallEvent.SurveyResponse -> {
                            scope.launch {
                                message.emit(eventInfo.placement)
                                println("!!! TEST 65 !!! SurveyResponse")
                            }
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("show_survey_with_other") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )
    var test66Info =
        UITestInfo(
            66,
            "Delete an reinstall app. Present paywall then tap close button. A survey will " +
                "NOT show. The console will print out !!! TEST 66 !!! with PaywallClose. " +
                "The feature block will fire.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.PaywallClose -> {
                            println("!!! TEST 66 !!! PaywallClose")
                        }

                        is SuperwallEvent.SurveyResponse -> {
                            println("!!! TEST 66 !!! SurveyResponse")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("zero_percent_survey") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )

    // TODO: Test 67 needs modal presentation and swipe to dismiss.

    var test68Info =
        UITestInfo(
            68,
            "Delete an reinstall app. Present paywall then tap close button. A survey will " +
                "show. Tap Option 1. The paywall will dismiss and a new paywall will auto show. " +
                "The console will print out !!! TEST 68 !!! with PaywallClose. Close the second " +
                "paywall. The feature block will fire on close of the second paywall.",
            test =
                { scope, events, _ ->
                    // Create a mock Superwall delegate
                    val delegate = MockSuperwallDelegate()
                    Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                    // Set delegate
                    Superwall.instance.delegate = delegate

                    // Respond to Superwall events
                    delegate.handleSuperwallEvent { eventInfo ->
                        when (eventInfo.placement) {
                            is SuperwallEvent.PaywallClose -> {
                                println("!!! TEST 68 !!! PaywallClose")
                            }

                            is SuperwallEvent.SurveyResponse -> {
                                println("!!! TEST 68 !!! SurveyResponse")
                            }

                            else -> return@handleSuperwallEvent
                        }
                    }

                    Superwall.instance.register("campaign_trigger") {
                        val alertController =
                            AlertControllerFactory.make(
                                context = this,
                                title = "Feature Launched",
                                message = "The feature block was called",
                                actionTitle = "Ok",
                            )
                        alertController.show()
                    }
                },
        )

    // TODO: Test 69 requires modal presentation and swipe to dismiss.

    var test70Info =
        UITestInfo(
            70,
            "Delete an reinstall app. Present paywall then tap close button. Make sure a " +
                "survey is displayed. Tap Option 1, make sure it dismisses and the console prints " +
                "!!! TEST 70 !!! twice with both PaywallClose and SurveyResponse.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.PaywallClose -> {
                            println("!!! TEST 70 !!! PaywallClose")
                        }

                        is SuperwallEvent.SurveyResponse -> {
                            println("!!! TEST 70 !!! SurveyResponse")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                // Create a mock paywall view
                val paywallDelegate = MockPaywallViewDelegate()
                paywallDelegate.paywallViewFinished { paywallView, paywallResult, shouldDismiss ->
                    println("!!! TEST 70 !!! Result: $paywallResult, shouldDismiss: $shouldDismiss, paywallVc: $paywallView")
                    if (shouldDismiss) {
                        CoroutineScope(Dispatchers.IO).launch {
                            Superwall.instance.dismiss()
                        }
                    }
                }

                // Get the paywall view instance
                val view =
                    Superwall.instance.getPaywall(
                        placement = "show_survey_with_other",
                        delegate = paywallDelegate,
                    )

                // Present using the convenience `SuperwallPaywallActivity` activity and verify test case.
                SuperwallPaywallActivity.startWithView(context = this, view = view.getOrThrow())
            },
        )
    var test71Info =
        UITestInfo(
            71,
            "Delete an reinstall app. Present paywall then purchase product. Make sure the " +
                "paywall closes and DOES NOT show a survey. The console should NOT print " +
                "!!! TEST 71 !!!.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.SurveyResponse -> {
                            println("!!! TEST 71 !!! SurveyResponse")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("survey_with_purchase_button") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )

    // TODO: Test 63 - 71 require getPaywall, feature block, delegate, and surveys.

    var test72Info =
        UITestInfo(
            72,
            "Check that calling identify restores the seed value to 2. This is async and " +
                "dependent on config so needs to sleep after calling identify. You should see " +
                "\"!!! seed - 1: 2\", where 2 is a seed number. Then \"!!! user ID: abc\", then " +
                " \"!!! seed - 2: 2\".",
            test =
                { scope, events, _ ->
                    Superwall.instance.identify(userId = "abc")

                    var seed = Superwall.instance.userAttributes["seed"]
                    println("!!! seed - 1: $seed")

                    Superwall.instance.reset()

                    Superwall.instance.identify(userId = "abc")

                    val appUserId = Superwall.instance.userAttributes["appUserId"]
                    println("!!! user ID: $appUserId")

                    seed = Superwall.instance.userAttributes["seed"]
                    println("!!! seed - 2: $seed")
                },
        )

    // TODO: Test 73 needs touches_began implemented.

    var test74Info =
        UITestInfo(
            74,
            "Delete an reinstall app. Present paywall then close the paywall. A survey will " +
                "show. Tap the close button. The paywall will close and the console will print " +
                "\"!!! TEST 74 !!! SurveyClose\".",
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.SurveyClose -> {
                            println("!!! TEST 74 !!! SurveyClose")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("survey_with_close_option")
            },
        )
    var test75Info =
        UITestInfo(
            75,
            "Present paywall then make a purchase. Make sure !!! TEST 75 !!! with  \"false\" " +
                "for the transaction being nil, a product id, and a paywall id is printed to the " +
                "console.",
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.TransactionComplete -> {
                            val transaction =
                                (eventInfo.placement as SuperwallEvent.TransactionComplete).transaction == null
                            val productId =
                                (eventInfo.placement as SuperwallEvent.TransactionComplete).product.fullIdentifier
                            val paywallId =
                                (eventInfo.placement as SuperwallEvent.TransactionComplete).paywallInfo.identifier

                            println("!!! TEST 75 !!! TransactionComplete. Transaction nil? $transaction, $productId, $paywallId")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register("present_data")
            },
        )
    var test82Info =
        UITestInfo(
            82,
            "Verify that our pricing gets templated in correctly.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "price_readout")
            },
        )
    var test83Info =
        UITestInfo(
            83,
            "The first time launch is tapped you will land in holdout. Check Skipped paywall " +
                "for holdout printed in console. Tapping launch again will present paywall. Will " +
                "need to delete app to be able to do it again as it uses limits.",
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "holdout_one_time_occurrence")
            },
        )
    var testAndroid4Info =
        UITestInfo(
            40,
            "NOTE: Must use `Android Main screen` API key. Launch compose debug screen: " +
                "Verify that paywall loads in Tab 0. Go to Tab 2 and press `Another Paywall` button. " +
                "Verify that paywall does not load (only 1 paywall can be displayed at once).",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                val intent = Intent(this, ComposeActivity::class.java)
                startActivity(intent)
            },
        )
    var testAndroid9Info =
        UITestInfo(
            90,
            "Tap launch button. It should display the paywall. Tap again and it should NOT " +
                "display again. You will need to delete and reinstall the app to test this again. " +
                "This tests the occurrence limit.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "one_time_occurrence")
            },
        )

    var testAndroid18Info =
        UITestInfo(
            180,
            "Tap launch button. It should display the paywall. Tap again and it should NOT " +
                "display again within a minute, printing a NoRuleMatch in the console. After a " +
                "minute, tap again, and it should show. You will need to delete and reinstall " +
                "the app to test this again.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "once_a_minute")
            },
        )
    var testAndroid19Info =
        UITestInfo(
            190,
            "Tap launch button. The paywall should not display until one day or longer has " +
                "passed since the last once_a_minute event. You'll get a NoRuleMatch in the " +
                "console.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "one_day_since_last_event")
            },
        )
    var testAndroid20Info =
        UITestInfo(
            200,
            "Non-recurring product purchase. Purchase the product and",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                // Create a mock Superwall delegate
                val delegate = MockSuperwallDelegate()

                // Set delegate
                Superwall.instance.delegate = delegate

                // Respond to Superwall events
                delegate.handleSuperwallEvent { eventInfo ->
                    when (eventInfo.placement) {
                        is SuperwallEvent.NonRecurringProductPurchase -> {
                            println("!!! Android TEST 20 !!! Non recurring product purchase")
                        }

                        else -> return@handleSuperwallEvent
                    }
                }

                Superwall.instance.register(placement = "non_recurring_product")
            },
        )
    var testAndroid21Info =
        UITestInfo(
            210,
            "Tap launch button. Paywall should display. Tests that paywalls with products " +
                "that have base plans and offerIds works.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "subs_baseplan_offer_with_free_trial")
            },
        )
    var testAndroid22Info =
        UITestInfo(
            220,
            "Tap launch button. Paywall should display. Purchase then quit app. Notification" +
                " should appear after a minute.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register(placement = "notifications")
            },
        )
    var testAndroid23Info =
        UITestInfo(
            230,
            "Tap button. You should see first_name: Jack printed out in user attributes " +
                "then the user attributes shouldn't contain first_name",
            testCaseType = TestCaseType.Android,
            test = { scope, events, msg ->
                Superwall.instance.setUserAttributes(attributes = mapOf("first_name" to "Jack"))
                val attributes = Superwall.instance.userAttributes
                println(attributes)
                scope.launch {
                    msg.emit(attributes)
                }
                Superwall.instance.setUserAttributes(attributes = mapOf("first_name" to null))
                val next = Superwall.instance.userAttributes
                println(next)
                scope.launch {
                    msg.emit(next)
                }
            },
        )

    var testAndroid100Info =
        UITestInfo(
            100,
            "Entitlements test: Tap launch button. Paywall should display when user has no entitlements.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(SubscriptionStatus.Inactive)
                Superwall.instance.register(placement = "entitlements_test_basic")
            },
        )

    var testAndroid101Info =
        UITestInfo(
            101,
            "Entitlements test: Tap launch button. Paywall should not display since user has the entitlement `basic`. Dialog should show.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus(
                    SubscriptionStatus.Active(
                        setOf(
                            Entitlement(
                                "basic",
                            ),
                        ),
                    ),
                )
                Superwall.instance.register(placement = "entitlements_test_basic") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )

    var testAndroid102Info =
        UITestInfo(
            102,
            "Entitlements test: Tap launch button. Paywall should display when user has no `pro` entitlements.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus("basic")
                Superwall.instance.register(placement = "entitlements_test_pro") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )

    var testAndroid103Info =
        UITestInfo(
            103,
            "Entitlements test: Tap launch button. Paywall should not display when user has `pro` entitlements. Dialog should show.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.setSubscriptionStatus("pro")
                Superwall.instance.register(placement = "entitlements_test_pro") {
                    val alertController =
                        AlertControllerFactory.make(
                            context = this,
                            title = "Feature Launched",
                            message = "The feature block was called",
                            actionTitle = "Ok",
                        )
                    alertController.show()
                }
            },
        )

    var testAndroid104Info =
        UITestInfo(
            103,
            "Test review with SuperwallOptions.useMockReview = true. When show review is tapped, a review prompt should show.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register("review_test")
            },
        )

    var testAndroid105Info =
        UITestInfo(
            103,
            "Test review with SuperwallOptions.useMockReview = true. When show external review is tapped, the play store should open.",
            testCaseType = TestCaseType.Android,
            test = { scope, events, _ ->
                Superwall.instance.register("review_test")
            },
        )

    val tests =
        listOf<UITestInfo>(
            UITestHandler.test0Info,
            UITestHandler.test1Info,
            UITestHandler.test2Info,
            UITestHandler.test3Info,
            UITestHandler.test4Info,
            UITestHandler.test5Info,
            UITestHandler.test6Info,
            UITestHandler.test7Info,
            UITestHandler.test8Info,
            UITestHandler.test9Info,
            UITestHandler.test10Info,
            UITestHandler.test11Info,
            UITestHandler.test12Info,
            UITestHandler.test13Info,
            UITestHandler.test14Info,
            UITestHandler.test15Info,
            UITestHandler.test16Info,
            UITestHandler.test17Info,
            UITestHandler.test18Info,
            UITestHandler.test19Info,
            UITestHandler.test20Info,
            UITestHandler.test21Info,
            UITestHandler.test22Info,
            UITestHandler.test23Info,
            UITestHandler.test24Info,
            UITestHandler.test25Info,
            UITestHandler.test26Info,
            UITestHandler.test27Info,
            UITestHandler.test28Info,
            UITestHandler.test29Info,
            UITestHandler.test30Info,
            UITestHandler.test31Info,
            UITestHandler.test32Info,
            UITestHandler.test33Info,
            UITestHandler.test34Info,
            UITestHandler.test35Info,
            UITestHandler.test36Info,
            UITestHandler.test37Info,
            UITestHandler.test41Info,
            UITestHandler.test42Info,
            UITestHandler.test43Info,
            UITestHandler.test44Info,
            UITestHandler.test45Info,
            UITestHandler.test46Info,
            UITestHandler.test47Info,
            UITestHandler.test48Info,
            UITestHandler.test50Info,
            UITestHandler.test52Info,
            UITestHandler.test53Info,
            UITestHandler.test56Info,
            UITestHandler.test57Info,
            UITestHandler.test58Info,
            UITestHandler.test59Info,
            UITestHandler.test60Info,
            UITestHandler.test62Info,
            UITestHandler.test63Info,
            UITestHandler.test64Info,
            UITestHandler.test65Info,
            UITestHandler.test66Info,
            UITestHandler.test68Info,
            UITestHandler.test70Info,
            UITestHandler.test71Info,
            UITestHandler.test72Info,
            UITestHandler.test74Info,
            UITestHandler.test75Info,
            UITestHandler.test82Info,
            UITestHandler.test83Info,
            UITestHandler.testAndroid4Info,
            UITestHandler.testAndroid9Info,
            UITestHandler.testAndroid18Info,
            UITestHandler.testAndroid19Info,
            UITestHandler.testAndroid20Info,
            UITestHandler.testAndroid21Info,
            UITestHandler.testAndroid22Info,
            UITestHandler.testAndroid23Info,
            UITestHandler.testAndroid100Info,
            UITestHandler.testAndroid101Info,
            UITestHandler.testAndroid102Info,
            UITestHandler.testAndroid103Info,
            UITestHandler.testAndroid104Info,
            UITestHandler.testAndroid105Info,
        )
}
