package com.superwall.superapp.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Global registry of named [EventTimeline] instances.
 * The app delegate records into a "Live" timeline.
 * Each UITestInfo registers its timeline here when a test runs.
 */
object TimelineStore {
    private val timelines = ConcurrentHashMap<String, EventTimeline>()
    private val _timelinesFlow = MutableStateFlow<Map<String, EventTimeline>>(emptyMap())
    private val mergeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Observable snapshot of all registered timelines. */
    val timelinesFlow: StateFlow<Map<String, EventTimeline>> = _timelinesFlow.asStateFlow()

    /**
     * Merged view of every event across every registered timeline, sorted by epoch.
     * Each event's [TimedEvent.elapsed] is recomputed relative to the earliest event so the
     * existing detail UI renders deltas/totals correctly against a single virtual t=0.
     *
     * Useful because the SDK delegate is reassigned per-test, so no single real timeline ever
     * holds the full picture of what happened in the app.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val mergedFlow: StateFlow<List<TimedEvent>> =
        _timelinesFlow
            .flatMapLatest { map ->
                if (map.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(map.values.map { it.eventsFlow }) { lists ->
                        val all = lists.flatMap { it.toList() }
                        if (all.isEmpty()) {
                            emptyList()
                        } else {
                            val sorted = all.sortedBy { it.epochMillis }
                            val base = sorted.first().epochMillis
                            sorted.map { event ->
                                event.copy(elapsed = (event.epochMillis - base).milliseconds)
                            }
                        }
                    }
                }
            }
            .stateIn(mergeScope, SharingStarted.Eagerly, emptyList())

    fun register(name: String, timeline: EventTimeline) {
        timelines[name] = timeline
        _timelinesFlow.value = timelines.toMap()
    }

    fun remove(name: String) {
        timelines.remove(name)
        _timelinesFlow.value = timelines.toMap()
    }

    fun get(name: String): EventTimeline? = timelines[name]

    fun all(): Map<String, EventTimeline> = timelines.toMap()

    fun clear() {
        timelines.clear()
        _timelinesFlow.value = emptyMap()
    }
}
