package com.superwall.sdk.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.internal.trackable.Trackable
import com.superwall.sdk.analytics.internal.trackable.UserInitiatedEvent
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.config.options.SuperwallOptions
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.MainScope
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.events.EventsRequest
import com.superwall.sdk.network.Network
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

class EventsQueue(
    private val context: Context,
    private val network: Network,
    private val ioScope: IOScope,
    private val mainScope: MainScope,
    private val configManager: ConfigManager,
) : BroadcastReceiver(),
    CoroutineScope {
    private val maxEventCount = 50
    private var elements = mutableListOf<EventData>()
    private val timer = MutableSharedFlow<Long>()
    private var job: Job? = null
    override val coroutineContext: CoroutineContext = Dispatchers.IO.limitedParallelism(1)

    init {
        mainScope.launch {
            setupTimer()
            addObserver()
        }
    }

    private suspend fun setupTimer() {
        val timeInterval =
            if (configManager.options?.networkEnvironment is SuperwallOptions.NetworkEnvironment.Release) 20L else 1L
        job =
            ioScope.launch {
                while (isActive) {
                    delay(timeInterval * 1000) // delay works in milliseconds
                    timer.emit(System.currentTimeMillis())
                }
            }
        timer.collect {
            flushInternal()
        }
    }

    private fun addObserver() {
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(this, filter)
    }

    fun enqueue(
        data: EventData,
        event: Trackable,
    ) {
        if (!externalDataCollectionAllowed(event)) return

        launch {
            elements.add(data)
        }
    }

    private fun externalDataCollectionAllowed(event: Trackable): Boolean {
        if (Superwall.instance.options.isExternalDataCollectionEnabled) {
            return true
        }
        return when (event) {
            is InternalSuperwallEvent.TriggerFire,
            is InternalSuperwallEvent.Attributes,
            is UserInitiatedEvent.Track,
            -> false
            else -> true
        }
    }

    suspend fun flushInternal(depth: Int = 10) {
        launch {
            val eventsToSend = mutableListOf<EventData>()
            var i = 0
            while (i < maxEventCount && elements.isNotEmpty()) {
                eventsToSend.add(elements.removeAt(0))
                i += 1
            }
            if (eventsToSend.isNotEmpty()) {
                // Send to network
                val events = EventsRequest(eventsToSend)
                ioScope.launch {
                    network.sendEvents(events)
                }
            }
            if (elements.isNotEmpty() && depth > 0) {
                flushInternal(depth - 1)
            }
        }
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // equivalent to "applicationWillResignActive"
                // your code here
                ioScope.launch {
                    flushInternal()
                }
            }
        }
    }
}
