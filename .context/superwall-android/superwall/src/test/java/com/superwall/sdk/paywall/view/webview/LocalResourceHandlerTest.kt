package com.superwall.sdk.paywall.view.webview

import android.content.Context
import android.net.Uri
import com.superwall.sdk.Given
import com.superwall.sdk.Then
import com.superwall.sdk.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocalResourceHandlerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun test_isLocalResourceUrl_returnsTrueForSwlocalScheme() {
        Given("a LocalResourceHandler") {
            val handler = LocalResourceHandler(context) { emptyMap() }

            When("checking a swlocal:// URL") {
                val result = handler.isLocalResourceUrl(Uri.parse("swlocal://hero-image"))

                Then("it returns true") {
                    assertTrue(result)
                }
            }
        }
    }

    @Test
    fun test_isLocalResourceUrl_returnsFalseForOtherSchemes() {
        Given("a LocalResourceHandler") {
            val handler = LocalResourceHandler(context) { emptyMap() }

            When("checking an https:// URL") {
                val result = handler.isLocalResourceUrl(Uri.parse("https://example.com/image.png"))

                Then("it returns false") {
                    assertFalse(result)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_emptyHost_returns400() {
        Given("a LocalResourceHandler") {
            val handler = LocalResourceHandler(context) { emptyMap() }

            When("handling a swlocal URL with no host") {
                val response = handler.handleRequest(Uri.parse("swlocal:///"))

                Then("it returns a 400 error response") {
                    assertEquals(400, response.statusCode)
                    assertEquals("text/plain", response.mimeType)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_missingResource_returns404() {
        Given("a LocalResourceHandler with no mapped resources") {
            val handler = LocalResourceHandler(context) { emptyMap() }

            When("handling a swlocal URL for an unmapped resource") {
                val response = handler.handleRequest(Uri.parse("swlocal://hero-image"))

                Then("it returns a 404 error response") {
                    assertEquals(404, response.statusCode)
                    assertEquals("text/plain", response.mimeType)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_validUriResource_returns200() {
        Given("a LocalResourceHandler with a mapped PNG file via FromUri") {
            val file = File(context.filesDir, "test-image.png")
            file.parentFile?.mkdirs()
            file.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG magic bytes

            val resource = PaywallResource.FromUri(Uri.fromFile(file))
            val handler = LocalResourceHandler(context) { mapOf("hero" to resource) }

            When("handling a swlocal://hero request") {
                val response = handler.handleRequest(Uri.parse("swlocal://hero"))

                Then("it returns a 200 response with content") {
                    assertEquals(200, response.statusCode)
                    assertEquals("OK", response.reasonPhrase)
                    assertNotNull(response.data)
                }
            }

            file.delete()
        }
    }

    @Test
    fun test_handleRequest_fromResources_returns200() {
        Given("a LocalResourceHandler with a FromResources resource") {
            // android.R.drawable.ic_delete is a built-in Android resource available in Robolectric
            val resource = PaywallResource.FromResources(android.R.drawable.ic_delete)
            val handler = LocalResourceHandler(context) { mapOf("icon" to resource) }

            When("handling a swlocal://icon request") {
                val response = handler.handleRequest(Uri.parse("swlocal://icon"))

                Then("it returns a 200 response with content") {
                    assertEquals(200, response.statusCode)
                    assertNotNull(response.data)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_unreadableUri_returns500() {
        Given("a LocalResourceHandler with a FromUri pointing to a non-existent file") {
            val resource = PaywallResource.FromUri(Uri.fromFile(File("/nonexistent/path/file.png")))
            val handler = LocalResourceHandler(context) { mapOf("missing" to resource) }

            When("handling a request for that resource") {
                val response = handler.handleRequest(Uri.parse("swlocal://missing"))

                Then("it returns a 500 error response") {
                    assertEquals(500, response.statusCode)
                    assertEquals("text/plain", response.mimeType)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_invalidResId_returns500() {
        Given("a LocalResourceHandler with a FromResources using an invalid resId") {
            val resource = PaywallResource.FromResources(0x7f_ff_ff_ff)
            val handler = LocalResourceHandler(context) { mapOf("bad" to resource) }

            When("handling a request for that resource") {
                val response = handler.handleRequest(Uri.parse("swlocal://bad"))

                Then("it returns a 500 error response") {
                    assertEquals(500, response.statusCode)
                    assertEquals("text/plain", response.mimeType)
                }
            }
        }
    }

    @Test
    fun test_handleRequest_unknownExtension_returnsOctetStream() {
        Given("a LocalResourceHandler with a file with unknown extension") {
            val file = File(context.filesDir, "data.xyz123")
            file.parentFile?.mkdirs()
            file.writeBytes(byteArrayOf(0x00, 0x01, 0x02))

            val resource = PaywallResource.FromUri(Uri.fromFile(file))
            val handler = LocalResourceHandler(context) { mapOf("data" to resource) }

            When("handling a request for that resource") {
                val response = handler.handleRequest(Uri.parse("swlocal://data"))

                Then("it falls back to application/octet-stream") {
                    assertEquals(200, response.statusCode)
                    assertEquals("application/octet-stream", response.mimeType)
                }
            }

            file.delete()
        }
    }

    @Test
    fun test_handleRequest_corsHeaderPresent() {
        Given("a LocalResourceHandler with a mapped file") {
            val file = File(context.filesDir, "test.txt")
            file.parentFile?.mkdirs()
            file.writeText("hello")

            val resource = PaywallResource.FromUri(Uri.fromFile(file))
            val handler = LocalResourceHandler(context) { mapOf("text" to resource) }

            When("handling a request") {
                val response = handler.handleRequest(Uri.parse("swlocal://text"))

                Then("the response includes CORS headers") {
                    assertEquals(200, response.statusCode)
                    assertEquals("*", response.responseHeaders["Access-Control-Allow-Origin"])
                }
            }

            file.delete()
        }
    }
}
