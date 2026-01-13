package com.superwall.sdk.paywall.view

import android.graphics.Color
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import com.superwall.sdk.paywall.view.delegate.PaywallLoadingState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadingViewTest {
    @Test
    fun init_createsLoadingViewWithCorrectProperties() {
        Given("a context") {
            val context = RuntimeEnvironment.getApplication()

            When("LoadingView is created") {
                val loadingView = LoadingView(context)

                Then("it has correct properties") {
                    assertEquals(LoadingView.TAG, loadingView.tag)
                    assertEquals(Color.TRANSPARENT, loadingView.solidColor)
                    assertEquals(1, loadingView.childCount)
                }
            }
        }
    }

    @Test
    fun init_addsProgressBarAsChild() {
        Given("a context") {
            val context = RuntimeEnvironment.getApplication()

            When("LoadingView is created") {
                val loadingView = LoadingView(context)

                Then("it contains a ProgressBar child") {
                    val child = loadingView.getChildAt(0)
                    assertTrue(child is ProgressBar)
                }
            }
        }
    }

    @Test
    fun init_progressBarIsCentered() {
        Given("a context") {
            val context = RuntimeEnvironment.getApplication()

            When("LoadingView is created") {
                val loadingView = LoadingView(context)
                val progressBar = loadingView.getChildAt(0) as ProgressBar

                Then("ProgressBar is centered") {
                    val layoutParams = progressBar.layoutParams as FrameLayout.LayoutParams
                    assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.width)
                    assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.height)
                    assertEquals(android.view.Gravity.CENTER, layoutParams.gravity)
                }
            }
        }
    }

    @Test
    fun init_setsTag() {
        Given("a context") {
            val context = RuntimeEnvironment.getApplication()

            When("LoadingView is created") {
                val loadingView = LoadingView(context)

                Then("it has the correct tag") {
                    assertEquals(LoadingView.TAG, loadingView.tag)
                }
            }
        }
    }

    @Test
    fun showLoading_setsVisibilityToVisible() {
        Given("a LoadingView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            loadingView.visibility = GONE

            When("showLoading is called") {
                loadingView.showLoading()

                Then("visibility is set to VISIBLE") {
                    assertEquals(VISIBLE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun hideLoading_setsVisibilityToGone() {
        Given("a LoadingView that is visible") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            loadingView.visibility = VISIBLE

            When("hideLoading is called") {
                loadingView.hideLoading()

                Then("visibility is set to GONE") {
                    assertEquals(GONE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_loadingPurchase_showsView() {
        Given("a LoadingView and PaywallView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called with LoadingPurchase state") {
                loadingView.setupFor(paywallView, PaywallLoadingState.LoadingPurchase)

                Then("view is added to paywall and made visible") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(VISIBLE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_manualLoading_showsView() {
        Given("a LoadingView and PaywallView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called with ManualLoading state") {
                loadingView.setupFor(paywallView, PaywallLoadingState.ManualLoading)

                Then("view is added to paywall and made visible") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(VISIBLE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_ready_hidesView() {
        Given("a LoadingView and PaywallView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called with Ready state") {
                loadingView.setupFor(paywallView, PaywallLoadingState.Ready)

                Then("view is added to paywall but hidden") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(GONE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_handlesNullParent() {
        Given("a LoadingView with no parent") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called") {
                loadingView.setupFor(paywallView, PaywallLoadingState.Ready)

                Then("view is added to paywall successfully") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(GONE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_loadingURL_hidesView() {
        Given("a LoadingView and PaywallView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called with LoadingURL state") {
                loadingView.setupFor(paywallView, PaywallLoadingState.LoadingURL)

                Then("view is added to paywall but hidden") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(GONE, loadingView.visibility)
                }
            }
        }
    }

    @Test
    fun setupFor_unknown_hidesView() {
        Given("a LoadingView and PaywallView") {
            val context = RuntimeEnvironment.getApplication()
            val loadingView = LoadingView(context)
            val paywallView =
                mockk<PaywallView>(relaxed = true) {
                    every { addView(any<View>()) } returns Unit
                }

            When("setupFor is called with Unknown state") {
                loadingView.setupFor(paywallView, PaywallLoadingState.Unknown)

                Then("view is added to paywall but hidden") {
                    verify { paywallView.addView(loadingView) }
                    assertEquals(GONE, loadingView.visibility)
                }
            }
        }
    }
}
