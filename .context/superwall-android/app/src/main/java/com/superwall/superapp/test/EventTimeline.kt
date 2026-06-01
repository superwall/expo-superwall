package com.superwall.superapp.test

import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.sdk.analytics.superwall.SuperwallEventInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A single captured event with its timestamp relative to timeline start.
 */
data class TimedEvent(
    val eventName: String,
    val eventType: String,
    val event: SuperwallEvent,
    val params: Map<String, Any>,
    val elapsed: Duration,
    val epochMillis: Long,
)

/**
 * Thread-safe timeline that captures all SDK events with timing information.
 * Created per-test to track event flow and measure durations.
 */
class EventTimeline(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var startMark: TimeMark = timeSource.markNow()
    @PublishedApi
    internal val _events = CopyOnWriteArrayList<TimedEvent>()

    private val _eventsFlow = MutableStateFlow<List<TimedEvent>>(emptyList())

    /** Observable snapshot of all events — emits a new list on every record/clear. */
    val eventsFlow: StateFlow<List<TimedEvent>> = _eventsFlow.asStateFlow()

    val events: List<TimedEvent> get() = _events.toList()

    fun record(eventInfo: SuperwallEventInfo) {
        val elapsed = startMark.elapsedNow()
        _events.add(
            TimedEvent(
                eventName = eventInfo.event.rawName,
                eventType = eventInfo.event::class.simpleName ?: "Unknown",
                event = eventInfo.event,
                params = eventInfo.params,
                elapsed = elapsed,
                epochMillis = System.currentTimeMillis(),
            ),
        )
        _eventsFlow.value = _events.toList()
    }

    fun clear() {
        _events.clear()
        _eventsFlow.value = emptyList()
        startMark = timeSource.markNow()
    }

    /** All events in chronological order. */
    fun allEvents(): List<TimedEvent> = events

    /** Total elapsed time from timeline start to the last recorded event. */
    fun totalDuration(): Duration =
        _events.lastOrNull()?.elapsed ?: Duration.ZERO

    /** Time from timeline start to the first occurrence of the given event type. */
    inline fun <reified T : SuperwallEvent> durationTo(): Duration? =
        _events.firstOrNull { it.event is T }?.elapsed

    /** Time from timeline start to the first event matching the given name. */
    fun durationTo(eventName: String): Duration? =
        _events.firstOrNull { it.eventName == eventName }?.elapsed

    /** Duration between the first occurrence of event A and event B. */
    inline fun <reified A : SuperwallEvent, reified B : SuperwallEvent> durationBetween(): Duration? {
        val a = _events.firstOrNull { it.event is A }?.elapsed ?: return null
        val b = _events.firstOrNull { it.event is B }?.elapsed ?: return null
        return b - a
    }

    /** Duration between two events matched by name. */
    fun durationBetween(from: String, to: String): Duration? {
        val a = _events.firstOrNull { it.eventName == from }?.elapsed ?: return null
        val b = _events.firstOrNull { it.eventName == to }?.elapsed ?: return null
        return b - a
    }

    /** All events matching the given type. */
    inline fun <reified T : SuperwallEvent> eventsOf(): List<TimedEvent> =
        _events.filter { it.event is T }

    /** All events matching the given name. */
    fun eventsOf(eventName: String): List<TimedEvent> =
        _events.filter { it.eventName == eventName }

    /** First event matching the given type, or null. */
    inline fun <reified T : SuperwallEvent> firstOf(): TimedEvent? =
        _events.firstOrNull { it.event is T }

    /** Check whether an event of the given type was recorded. */
    inline fun <reified T : SuperwallEvent> contains(): Boolean =
        _events.any { it.event is T }

    /** Serialize all events to a list of maps for JSON output. */
    fun toSerializableList(): List<Map<String, Any?>> = _events.toList().toSerializableList()
}

/** Top-level serializer so the merged-view (which is a bare list, not a timeline) can reuse it. */
fun List<TimedEvent>.toSerializableList(): List<Map<String, Any?>> =
    map { event ->
        mapOf(
            "eventName" to event.eventName,
            "eventType" to event.eventType,
            "elapsedMs" to event.elapsed.inWholeMilliseconds,
            "epochMillis" to event.epochMillis,
            "params" to event.params.mapValues { (_, v) -> v.toString() },
        )
    }
