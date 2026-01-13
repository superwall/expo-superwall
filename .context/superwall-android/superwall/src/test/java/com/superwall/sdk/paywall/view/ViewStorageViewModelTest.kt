package com.superwall.sdk.paywall.view

import android.view.View
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class ViewStorageViewModelTest {
    @Test
    fun storeView_addsViewToMap() {
        Given("a ViewStorageViewModel and a mock view") {
            val viewModel = ViewStorageViewModel()
            val mockView = mockk<View>(relaxed = true)
            val key = "test_key"

            When("storeView is called") {
                viewModel.storeView(key, mockView)

                Then("the view is stored in the map") {
                    assertEquals(mockView, viewModel.views[key])
                    assertEquals(mockView, viewModel.retrieveView(key))
                }
            }
        }
    }

    @Test
    fun removeView_removesViewFromMap() {
        Given("a ViewStorageViewModel with a stored view") {
            val viewModel = ViewStorageViewModel()
            val mockView = mockk<View>(relaxed = true)
            val key = "test_key"
            viewModel.storeView(key, mockView)

            When("removeView is called") {
                viewModel.removeView(key)

                Then("the view is removed from the map") {
                    assertNull(viewModel.views[key])
                    assertNull(viewModel.retrieveView(key))
                }
            }
        }
    }

    @Test
    fun retrieveView_returnsNullForNonExistentKey() {
        Given("a ViewStorageViewModel with no views") {
            val viewModel = ViewStorageViewModel()

            When("retrieveView is called with a non-existent key") {
                val result = viewModel.retrieveView("non_existent")

                Then("it returns null") {
                    assertNull(result)
                }
            }
        }
    }

    @Test
    fun all_returnsAllStoredViews() {
        Given("a ViewStorageViewModel with multiple views") {
            val viewModel = ViewStorageViewModel()
            val view1 = mockk<View>(relaxed = true)
            val view2 = mockk<View>(relaxed = true)
            val view3 = mockk<View>(relaxed = true)

            viewModel.storeView("key1", view1)
            viewModel.storeView("key2", view2)
            viewModel.storeView("key3", view3)

            When("all() is called") {
                val allViews = viewModel.all()

                Then("it returns all stored views") {
                    assertEquals(3, allViews.size)
                    assertTrue(allViews.contains(view1))
                    assertTrue(allViews.contains(view2))
                    assertTrue(allViews.contains(view3))
                }
            }
        }
    }

    @Test
    fun keys_returnsAllStoredKeys() {
        Given("a ViewStorageViewModel with multiple views") {
            val viewModel = ViewStorageViewModel()
            val view1 = mockk<View>(relaxed = true)
            val view2 = mockk<View>(relaxed = true)

            viewModel.storeView("alpha", view1)
            viewModel.storeView("beta", view2)

            When("keys() is called") {
                val keys = viewModel.keys()

                Then("it returns all stored keys") {
                    assertEquals(2, keys.size)
                    assertTrue(keys.contains("alpha"))
                    assertTrue(keys.contains("beta"))
                }
            }
        }
    }

    @Test
    fun storeView_overwritesExistingKey() {
        Given("a ViewStorageViewModel with an existing view") {
            val viewModel = ViewStorageViewModel()
            val oldView = mockk<View>(relaxed = true)
            val newView = mockk<View>(relaxed = true)
            val key = "same_key"

            viewModel.storeView(key, oldView)

            When("storeView is called with the same key") {
                viewModel.storeView(key, newView)

                Then("the old view is replaced with the new one") {
                    assertEquals(newView, viewModel.retrieveView(key))
                    assertEquals(1, viewModel.views.size)
                }
            }
        }
    }

    @Test
    fun viewStorage_isThreadSafe() {
        Given("a ViewStorageViewModel") {
            val viewModel = ViewStorageViewModel()

            Then("views map is a ConcurrentHashMap") {
                assertTrue(viewModel.views is java.util.concurrent.ConcurrentHashMap)
            }
        }
    }
}
