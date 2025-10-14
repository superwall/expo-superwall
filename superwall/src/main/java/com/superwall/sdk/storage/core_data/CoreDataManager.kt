package com.superwall.sdk.storage.core_data

import android.content.Context
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.triggers.TriggerRuleOccurrence
import com.superwall.sdk.storage.core_data.entities.ManagedEventData
import com.superwall.sdk.storage.core_data.entities.ManagedTriggerRuleOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.CoroutineContext

// TODO: https://linear.app/superwall/issue/SW-2346/[android]-coredatamanager-implementation
class CoreDataManager(
    context: Context,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO.limitedParallelism(1)
    private val superwallDatabase by lazy { SuperwallDatabase.getDatabase(context = context) }

    fun saveEventData(
        eventData: EventData,
        completion: ((ManagedEventData) -> Unit)? = null,
    ) {
        launch {
            try {
                // Create a new EventData object
                val managedEventData =
                    ManagedEventData(
                        id = eventData.id,
                        createdAt = eventData.createdAt,
                        name = eventData.name,
                        parameters = eventData.parameters,
                    )

                // Insert the data into the Room database
                superwallDatabase.managedEventDataDao().insert(managedEventData)

                // Call the completion (note: still on a bg thread)
                completion?.invoke(managedEventData)
            } catch (error: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.coreData,
                    message = "Error saving to Room database.",
                    error = error,
                )
            }
        }
    }

    fun save(
        triggerRuleOccurrence: TriggerRuleOccurrence,
        completion: ((ManagedTriggerRuleOccurrence) -> Unit)? = null,
    ) {
        launch {
            try {
                val managedRuleOccurrence =
                    ManagedTriggerRuleOccurrence(
                        occurrenceKey = triggerRuleOccurrence.key,
                    )

                // Insert the data into the Room database
                superwallDatabase.managedTriggerRuleOccurrenceDao().insert(managedRuleOccurrence)

                completion?.invoke(managedRuleOccurrence)
            } catch (error: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.coreData,
                    message = "Error saving to Room database.",
                    error = error,
                )
            }
        }
    }

    fun deleteAllEntities() {
        launch {
            try {
                superwallDatabase.managedTriggerRuleOccurrenceDao().deleteAll()
                superwallDatabase.managedEventDataDao().deleteAll()
            } catch (error: Throwable) {
                Logger.debug(
                    logLevel = LogLevel.error,
                    scope = LogScope.coreData,
                    message = "Could not delete entities in Room database.",
                    error = error,
                )
            }
        }
    }

    suspend fun getComputedPropertySinceEvent(
        event: EventData?,
        request: ComputedPropertyRequest,
    ): Int? {
        val lastEventDate: Date? =
            event?.let {
                if (it.name == request.eventName) it.createdAt else null
            }

        try {
            val event =
                superwallDatabase.managedEventDataDao().getLastSavedEvent(
                    name = request.eventName,
                    date = lastEventDate,
                ) ?: return null

            // This will store the date components
            val componentsMap = mutableMapOf<Int, Int>()
            val createdAtCalendar = Calendar.getInstance().apply { time = event.createdAt }
            val currentCalendar = Calendar.getInstance()

            when (request.type.calendarComponent) {
                Calendar.YEAR -> {
                    val yearsDifference = currentCalendar.get(Calendar.YEAR) - createdAtCalendar.get(Calendar.YEAR)
                    componentsMap[Calendar.YEAR] = yearsDifference
                }
                Calendar.MONTH -> {
                    val diffYear = currentCalendar.get(Calendar.YEAR) - createdAtCalendar.get(Calendar.YEAR)
                    val diffMonth = diffYear * 12 + currentCalendar.get(Calendar.MONTH) - createdAtCalendar.get(Calendar.MONTH)
                    componentsMap[Calendar.MONTH] = diffMonth
                }
                Calendar.DAY_OF_MONTH -> {
                    val diffTime = currentCalendar.timeInMillis - createdAtCalendar.timeInMillis
                    val diffDays = (diffTime / (24 * 60 * 60 * 1000)).toInt()
                    componentsMap[Calendar.DAY_OF_MONTH] = diffDays
                }
                Calendar.HOUR_OF_DAY -> {
                    val diffTime = currentCalendar.timeInMillis - createdAtCalendar.timeInMillis
                    val diffHours = (diffTime / (60 * 60 * 1000) % 24).toInt()
                    componentsMap[Calendar.HOUR_OF_DAY] = diffHours
                }
                Calendar.MINUTE -> {
                    val diffTime = currentCalendar.timeInMillis - createdAtCalendar.timeInMillis
                    val diffMinutes = (diffTime / (60 * 1000) % 60).toInt()
                    componentsMap[Calendar.MINUTE] = diffMinutes
                }
            }

            return request.type.dateComponent(componentsMap)
        } catch (error: Throwable) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.coreData,
                message = "Error getting last saved event from Room database.",
                error = error,
            )
            return null
        }
    }

    suspend fun countTriggerRuleOccurrences(ruleOccurrence: TriggerRuleOccurrence): Int {
        val dao = superwallDatabase.managedTriggerRuleOccurrenceDao() // Replace with your actual database instance retrieval method

        return when (ruleOccurrence.interval) {
            is TriggerRuleOccurrence.Interval.Minutes -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, -(ruleOccurrence.interval).minutes)
                val date = calendar.time

                // Fetch occurrences based on date and key
                val occurrences =
                    dao.getManagedTriggerRuleOccurrencesSinceDate(
                        key = ruleOccurrence.key,
                        date = date,
                    )

                occurrences.size // This gives you the count of occurrences that match the conditions
            }
            TriggerRuleOccurrence.Interval.Infinity -> {
                // Fetch all occurrences with the given key
                val occurrences = dao.getManagedTriggerRuleOccurrencesByKey(ruleOccurrence.key)
                occurrences.size ?: 0 // If null, return 0
            }
        }
    }

    suspend fun countEventsByNameInPeriod(
        name: String,
        startDate: Date,
        endDate: Date,
    ): Int =
        try {
            superwallDatabase.managedEventDataDao().countEventsByNameInPeriod(
                name = name,
                startDate = startDate,
                endDate = endDate,
            )
        } catch (error: Throwable) {
            Logger.debug(
                logLevel = LogLevel.error,
                scope = LogScope.coreData,
                message = "Error counting events by name in period from Room database.",
                error = error,
            )
            0
        }
}
