package com.superwall.sdk.storage

import android.content.Context
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@Serializable
data class TestData(
    val value: String,
    val count: Int,
)

@RunWith(RobolectricTestRunner::class)
class CacheTest {
    private lateinit var context: Context
    private lateinit var cache: Cache
    private lateinit var json: Json
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        json = Json { ignoreUnknownKeys = true }
        cache =
            Cache(
                context,
                ioQueue = testDispatcher,
                json = json,
            )
    }

    @Test
    fun `read returns null when key does not exist`() {
        val storable =
            object : Storable<String> {
                override val key = "non_existent_key"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<String>()
            }

        val result = cache.read(storable)

        assertNull(result)
    }

    @Test
    fun `write and read stores and retrieves data from memory cache`() {
        val storable =
            object : Storable<TestData> {
                override val key = "test_data"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<TestData>()
            }

        val testData = TestData("hello", 42)

        cache.write(storable, testData)
        val result = cache.read(storable)

        assertEquals(testData, result)
    }

    @Test
    fun `read retrieves data from disk when not in memory cache`() {
        val storable =
            object : Storable<TestData> {
                override val key = "disk_test_data"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<TestData>()
            }

        val testData = TestData("from_disk", 99)

        // Write data
        cache.write(storable, testData)

        // Create new cache instance to clear memory cache
        val newCache =
            Cache(
                context,
                ioQueue = testDispatcher,
                json = json,
            )

        // Read should retrieve from disk
        val result = newCache.read(storable)

        assertEquals(testData, result)
    }

    @Test
    fun `delete removes data from memory and disk`() {
        val storable =
            object : Storable<TestData> {
                override val key = "delete_test"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<TestData>()
            }

        val testData = TestData("to_delete", 123)

        // Write data
        cache.write(storable, testData)

        // Verify it exists
        assertEquals(testData, cache.read(storable))

        // Delete
        cache.delete(storable)

        // Verify it's gone
        assertNull(cache.read(storable))
    }

    @Test
    fun `clean clears all cached data`() {
        val storable1 =
            object : Storable<String> {
                override val key = "clean_test_1"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<String>()
            }

        val storable2 =
            object : Storable<Int> {
                override val key = "clean_test_2"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<Int>()
            }

        cache.write(storable1, "data1")
        cache.write(storable2, 42)

        cache.clean()

        assertNull(cache.read(storable1))
        assertNull(cache.read(storable2))
    }

    @Test
    fun `write overwrites existing data`() {
        val storable =
            object : Storable<String> {
                override val key = "overwrite_test"
                override val directory = SearchPathDirectory.CACHE
                override val serializer = serializer<String>()
            }

        cache.write(storable, "original")
        assertEquals("original", cache.read(storable))

        cache.write(storable, "updated")
        assertEquals("updated", cache.read(storable))
    }
}
