package com.superwall.superapp.test

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.superwall.sdk.analytics.superwall.SuperwallEvent
import com.superwall.superapp.ui.theme.MyApplicationTheme
import org.json.JSONArray
import org.json.JSONObject

class TimelineViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                TimelineViewerRoot()
            }
        }
    }
}

private sealed class TimelineSelection {
    object AllEvents : TimelineSelection()
    data class Single(val name: String, val timeline: EventTimeline) : TimelineSelection()
}

@Composable
private fun TimelineViewerRoot() {
    var selected by remember { mutableStateOf<TimelineSelection?>(null) }
    val timelines by TimelineStore.timelinesFlow.collectAsState()
    val mergedEvents by TimelineStore.mergedFlow.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val sel = selected) {
            null -> TimelineListScreen(
                timelines = timelines,
                mergedEvents = mergedEvents,
                onSelectAll = { selected = TimelineSelection.AllEvents },
                onSelectTimeline = { name, timeline -> selected = TimelineSelection.Single(name, timeline) },
            )
            is TimelineSelection.AllEvents -> TimelineDetailScreen(
                name = "All Events",
                events = mergedEvents,
                onBack = { selected = null },
            )
            is TimelineSelection.Single -> {
                val events by sel.timeline.eventsFlow.collectAsState()
                TimelineDetailScreen(
                    name = sel.name,
                    events = events,
                    onBack = { selected = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineListScreen(
    timelines: Map<String, EventTimeline>,
    mergedEvents: List<TimedEvent>,
    onSelectAll: () -> Unit,
    onSelectTimeline: (String, EventTimeline) -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Event Timelines",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        if (timelines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No timelines recorded yet.\nRun a test to capture events.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn {
                if (mergedEvents.isNotEmpty()) {
                    item(key = "__all_events__") {
                        AllEventsCard(
                            events = mergedEvents,
                            onClick = onSelectAll,
                            onLongClick = {
                                val json = JSONArray(
                                    mergedEvents.toSerializableList().map { entry ->
                                        JSONObject(entry.mapValues { (_, v) -> v ?: JSONObject.NULL })
                                    },
                                ).toString(2)
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("All Events", json))
                                Toast.makeText(context, "Copied ${mergedEvents.size} events as JSON", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
                timelines.entries.sortedByDescending { it.value.allEvents().size }.forEach { (name, timeline) ->
                    item(key = name) {
                        val liveEvents by timeline.eventsFlow.collectAsState()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .combinedClickable(
                                    onClick = { onSelectTimeline(name, timeline) },
                                    onLongClick = {
                                        val json = JSONArray(
                                            timeline.toSerializableList().map { entry ->
                                                JSONObject(entry.mapValues { (_, v) -> v ?: JSONObject.NULL })
                                            },
                                        ).toString(2)
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Timeline: $name", json))
                                        Toast.makeText(context, "Copied ${timeline.allEvents().size} events as JSON", Toast.LENGTH_SHORT).show()
                                    },
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${liveEvents.size} events",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    val lastElapsed = liveEvents.lastOrNull()?.elapsed
                                    Text(
                                        text = if (lastElapsed != null) formatDuration(lastElapsed.inWholeMilliseconds) else "--",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllEventsCard(
    events: List<TimedEvent>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "All Events",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${events.size} events (merged across all timelines)",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val lastElapsed = events.lastOrNull()?.elapsed
                Text(
                    text = if (lastElapsed != null) formatDuration(lastElapsed.inWholeMilliseconds) else "--",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineDetailScreen(
    name: String,
    events: List<TimedEvent>,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    var firstSelected by remember { mutableStateOf<Int?>(null) }
    var secondSelected by remember { mutableStateOf<Int?>(null) }

    // Reset selection if events list shrinks (e.g. timeline cleared)
    if (firstSelected != null && firstSelected!! >= events.size) firstSelected = null
    if (secondSelected != null && secondSelected!! >= events.size) secondSelected = null

    val selectionDuration by remember(firstSelected, secondSelected, events) {
        derivedStateOf {
            val a = firstSelected
            val b = secondSelected
            if (a != null && b != null && a < events.size && b < events.size) {
                val first = events[minOf(a, b)]
                val second = events[maxOf(a, b)]
                second.elapsed - first.elapsed
            } else {
                null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
        ) {
            Text(
                text = "< Back",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBack() },
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val totalMs = events.lastOrNull()?.elapsed?.inWholeMilliseconds ?: 0L
                Text(
                    text = "Total: ${formatDuration(totalMs)}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${events.size} events",
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Preload expandable section
            PreloadSection(events)

            // Selection duration banner
            val currentSelection = selectionDuration
            if (currentSelection != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                ) {
                    val fromIdx = minOf(firstSelected!!, secondSelected!!)
                    val toIdx = maxOf(firstSelected!!, secondSelected!!)
                    Column {
                        Text(
                            text = "Selection: ${formatDuration(currentSelection.inWholeMilliseconds)}",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${events[fromIdx].eventName} -> ${events[toIdx].eventName}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else if (firstSelected != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Long press a second event to measure duration",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Event list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(events, key = { index, _ -> index }) { index, event ->
                val isFirst = index == firstSelected
                val isSecond = index == secondSelected
                val isInRange = run {
                    val a = firstSelected
                    val b = secondSelected
                    a != null && b != null && index in minOf(a, b)..maxOf(a, b)
                }

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isFirst || isSecond -> MaterialTheme.colorScheme.primaryContainer
                        isInRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    },
                    label = "eventBg",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .combinedClickable(
                            onClick = {
                                // Clear selection on tap
                                firstSelected = null
                                secondSelected = null
                            },
                            onLongClick = {
                                when {
                                    firstSelected == null -> firstSelected = index
                                    secondSelected == null -> secondSelected = index
                                    else -> {
                                        firstSelected = index
                                        secondSelected = null
                                    }
                                }
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$index",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.width(32.dp),
                            )
                            Text(
                                text = event.eventName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Text(
                            text = formatDuration(event.elapsed.inWholeMilliseconds),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Delta from previous event
                    if (index > 0) {
                        val delta = event.elapsed - events[index - 1].elapsed
                        Text(
                            text = "+${formatDuration(delta.inWholeMilliseconds)}",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 32.dp),
                        )
                    }

                    // Show params if non-empty
                    if (event.params.isNotEmpty()) {
                        Text(
                            text = event.params.entries.joinToString(", ") { "${it.key}=${it.value}" },
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 32.dp, top = 2.dp),
                        )
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        }
    }
}

private data class LoadCounts(val started: Int, val completed: Int, val failed: Int)

@Composable
private fun PreloadSection(events: List<TimedEvent>) {
    var expanded by remember { mutableStateOf(false) }

    val preloadDuration by remember(events) {
        derivedStateOf {
            val configAttr = events.firstOrNull { it.event is SuperwallEvent.ConfigAttributes }
            val lastWebviewComplete = events.lastOrNull { it.event is SuperwallEvent.PaywallWebviewLoadComplete }
            if (configAttr != null && lastWebviewComplete != null) {
                lastWebviewComplete.elapsed - configAttr.elapsed
            } else {
                null
            }
        }
    }

    val preloadCounts by remember(events) {
        derivedStateOf {
            LoadCounts(
                started = events.count { it.event is SuperwallEvent.PaywallPreloadStart },
                completed = events.count { it.event is SuperwallEvent.PaywallPreloadComplete },
                failed = 0, // no preload error event exists
            )
        }
    }

    val responseCounts by remember(events) {
        derivedStateOf {
            LoadCounts(
                started = events.count { it.event is SuperwallEvent.PaywallResponseLoadStart },
                completed = events.count { it.event is SuperwallEvent.PaywallResponseLoadComplete },
                failed = events.count {
                    it.event is SuperwallEvent.PaywallResponseLoadFail || it.event is SuperwallEvent.PaywallResponseLoadNotFound
                },
            )
        }
    }

    val webviewCounts by remember(events) {
        derivedStateOf {
            LoadCounts(
                started = events.count { it.event is SuperwallEvent.PaywallWebviewLoadStart },
                completed = events.count { it.event is SuperwallEvent.PaywallWebviewLoadComplete },
                failed = events.count {
                    it.event is SuperwallEvent.PaywallWebviewLoadFail || it.event is SuperwallEvent.PaywallWebviewLoadTimeout
                },
            )
        }
    }

    val productsCounts by remember(events) {
        derivedStateOf {
            LoadCounts(
                started = events.count { it.event is SuperwallEvent.PaywallProductsLoadStart },
                completed = events.count { it.event is SuperwallEvent.PaywallProductsLoadComplete },
                failed = events.count { it.event is SuperwallEvent.PaywallProductsLoadFail },
            )
        }
    }

    val hasAnyPreloadEvents = preloadCounts.started > 0 || responseCounts.started > 0 ||
        webviewCounts.started > 0 || productsCounts.started > 0

    if (!hasAnyPreloadEvents && preloadDuration == null) return

    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "v" else ">",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    text = "Preload",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            preloadDuration?.let { d ->
                Text(
                    text = formatDuration(d.inWholeMilliseconds),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 20.dp, top = 8.dp)) {
                if (preloadCounts.started > 0) {
                    LoadCountRow("Preload", preloadCounts)
                }
                if (responseCounts.started > 0) {
                    LoadCountRow("Response", responseCounts)
                }
                if (webviewCounts.started > 0) {
                    LoadCountRow("Webview", webviewCounts)
                }
                if (productsCounts.started > 0) {
                    LoadCountRow("Products", productsCounts)
                }
            }
        }
    }
}

@Composable
private fun LoadCountRow(label: String, counts: LoadCounts) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountBadge(count = counts.started, label = "started", color = Color.Gray)
            CountBadge(count = counts.completed, label = "done", color = Color(0xFF4CAF50))
            if (counts.failed > 0) {
                CountBadge(count = counts.failed, label = "failed", color = Color(0xFFF44336))
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, label: String, color: Color) {
    Text(
        text = "$count $label",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = color,
    )
}

private fun formatDuration(ms: Long): String =
    when {
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> "%.2fs".format(ms / 1000.0)
        else -> {
            val minutes = ms / 60_000
            val seconds = (ms % 60_000) / 1000.0
            "${minutes}m %.1fs".format(seconds)
        }
    }
