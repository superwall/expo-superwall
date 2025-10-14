package com.superwall.sdk.storage.memory

/**
 * [PerpetualCache] caches the items perpetually unless they're manually [remove]ed.
 */
class PerpetualCache<K, V> : GenericCache<K, V> {
    private val cache = HashMap<K, V>()

    override val size: Int
        get() = cache.size

    override fun set(
        key: K,
        value: V,
    ) {
        cache[key] = value
    }

    override fun remove(key: K) = cache.remove(key)

    override fun get(key: K) = cache[key]

    override fun clear() = cache.clear()
}
