package com.superwall.sdk.analytics

import com.superwall.sdk.analytics.session.AppSession
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.network.Network
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.store.abstractions.transactions.StoreTransactionType
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

fun interface SessionEventsDelegate {
    suspend fun enqueue(transaction: StoreTransactionType)
}

// TODO: Re-enable session stuff

class SessionEventsManager(
//    private val queue: SessionEnqueuable,
    private val storage: LocalStorage,
    private val network: Network,
    private val configManager: ConfigManager,
) : CoroutineScope,
    SessionEventsDelegate {
    // The Coroutine Context is derived from SupervisorJob() + Dispatchers.Default
    // so that if a child job fails it does not affect the other child jobs
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Default

    // A queue of trigger session events that get sent to the server.
    private var cancellables: MutableList<Job> = mutableListOf()

    init {
        launch {
            postCachedSessionEvents()
        }
    }

    private suspend fun postCachedSessionEvents() {
//        configManager.config?.featureFlags?.enableSessionEvents?.let { enableSessionEvents ->
//            if (enableSessionEvents) {
//                val cachedTriggerSessions = storage.get(TriggerSessions::class.java) ?: listOf()
//                val cachedTransactions = storage.get(Transactions::class.java) ?: listOf()
//
//                if (cachedTriggerSessions.isEmpty() && cachedTransactions.isEmpty()) {
//                    return
//                }
//
//                val sessionEvents = SessionEventsRequest(
//                    triggerSessions = cachedTriggerSessions,
//                    transactions = cachedTransactions
//                )
//
//                network.sendSessionEvents(sessionEvents)
//
//                storage.clearCachedSessionEvents()
//            }
//        }
    }

    suspend fun updateAppSession(appSession: AppSession) {
//        triggerSession.updateAppSession(to = appSession)
    }

    override suspend fun enqueue(transaction: StoreTransactionType) {
//        configManager.config?.featureFlags?.enableSessionEvents?.let { enableSessionEvents ->
//            if (enableSessionEvents) {
//                queue.enqueue(transaction)
//            }
//        }
    }
}
