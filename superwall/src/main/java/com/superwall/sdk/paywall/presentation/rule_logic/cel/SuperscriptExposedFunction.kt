package com.superwall.sdk.paywall.presentation.rule_logic.cel

import com.superwall.sdk.models.config.ComputedPropertyRequest
import com.superwall.sdk.models.config.ComputedPropertyRequest.ComputedPropertyRequestType
import com.superwall.sdk.paywall.presentation.rule_logic.cel.InPeriod.Period.*
import com.superwall.sdk.paywall.presentation.rule_logic.cel.SuperscriptExposedFunction.Names.*
import com.superwall.sdk.paywall.presentation.rule_logic.cel.models.PassableValue
import com.superwall.sdk.storage.core_data.CoreDataManager
import java.util.Calendar
import java.util.Date

sealed class SuperscriptExposedFunction {
    enum class Names(
        val rawName: String,
    ) {
        MINUTES_SINCE("minutesSince"),
        HOURS_SINCE("hoursSince"),
        DAYS_SINCE("daysSince"),
        MONTHS_SINCE("monthsSince"),
        PLACEMENTS_IN_HOUR("placementsInHour"),
        PLACEMENTS_IN_DAY("placementsInDay"),
        PLACEMENTS_IN_WEEK("placementsInWeek"),
        PLACEMENTS_IN_MONTH("placementsInMonth"),
        PLACEMENTS_SINCE_INSTALL("placementsSinceInstall"),
        REVIEW_REQUESTS_IN_HOUR("reviewRequestsInHour"),
        REVIEW_REQUESTS_IN_DAY("reviewRequestsInDay"),
        REVIEW_REQUESTS_IN_WEEK("reviewRequestsInWeek"),
        REVIEW_REQUESTS_IN_MONTH("reviewRequestsInMonth"),
        REVIEW_REQUESTS_IN_YEAR("reviewRequestsInYear"),
        REVIEW_REQUESTS_TOTAL("reviewRequestsSinceInstall"),
        REQUEST_REVIEW("requestReview"),
    }

    class MinutesSince(
        override val event: String,
    ) : SuperscriptExposedFunction(),
        TimeSince {
        override val propertyRequest = ComputedPropertyRequestType.MINUTES_SINCE
    }

    class HoursSince(
        override val event: String,
    ) : SuperscriptExposedFunction(),
        TimeSince {
        override val propertyRequest = ComputedPropertyRequestType.HOURS_SINCE
    }

    class DaysSince(
        override val event: String,
    ) : SuperscriptExposedFunction(),
        TimeSince {
        override val propertyRequest = ComputedPropertyRequestType.DAYS_SINCE
    }

    class MonthsSince(
        override val event: String,
    ) : SuperscriptExposedFunction(),
        TimeSince {
        override val propertyRequest = ComputedPropertyRequestType.MONTHS_SINCE
    }

    class PlacementCount(
        val event: String,
        val period: InPeriod.Period,
    ) : SuperscriptExposedFunction() {
        suspend operator fun invoke(storage: CoreDataManager): Int? {
            val now = Date()
            val startDate =
                when (period) {
                    INSTALL -> Date(0) // Start from epoch for install period
                    else -> {
                        val calendar = Calendar.getInstance()
                        calendar.time = now
                        when (period) {
                            HOUR -> calendar.add(Calendar.HOUR, -1)
                            DAY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
                            WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                            MONTH -> calendar.add(Calendar.MONTH, -1)
                            else -> return null
                        }
                        calendar.time
                    }
                }

            return storage.countEventsByNameInPeriod(
                name = event,
                startDate = startDate,
                endDate = now,
            )
        }
    }

    class ReviewRequestCount(
        val period: InPeriod.Period,
    ) : SuperscriptExposedFunction() {
        suspend operator fun invoke(storage: CoreDataManager): Int? {
            val now = Date()
            val startDate =
                when (period) {
                    INSTALL -> Date(0) // Start from epoch for total period
                    else -> {
                        val calendar = Calendar.getInstance()
                        calendar.time = now
                        when (period) {
                            HOUR -> calendar.add(Calendar.HOUR, -1)
                            DAY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
                            WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                            MONTH -> calendar.add(Calendar.MONTH, -1)
                            YEAR -> calendar.add(Calendar.YEAR, -1)
                            else -> return null
                        }
                        calendar.time
                    }
                }

            return storage.countEventsByNameInPeriod(
                name = "review_requested",
                startDate = startDate,
                endDate = now,
            )
        }
    }

    object RequestReview : SuperscriptExposedFunction() {
        suspend operator fun invoke(storage: CoreDataManager): Boolean {
            // This will be handled differently - we need access to context and review manager
            // For now, just return true to indicate the function exists
            return true
        }
    }

    companion object {
        fun from(
            name: String,
            args: List<PassableValue>,
        ) = when (name) {
            MINUTES_SINCE.rawName -> MinutesSince((args.first() as PassableValue.StringValue).value)
            HOURS_SINCE.rawName -> HoursSince((args.first() as PassableValue.StringValue).value)
            DAYS_SINCE.rawName -> DaysSince((args.first() as PassableValue.StringValue).value)
            MONTHS_SINCE.rawName -> MonthsSince((args.first() as PassableValue.StringValue).value)
            PLACEMENTS_IN_HOUR.rawName ->
                PlacementCount(
                    event = (args.first() as PassableValue.StringValue).value,
                    HOUR,
                )

            PLACEMENTS_IN_DAY.rawName ->
                PlacementCount(
                    event = (args.first() as PassableValue.StringValue).value,
                    DAY,
                )

            PLACEMENTS_IN_WEEK.rawName ->
                PlacementCount(
                    event = (args.first() as PassableValue.StringValue).value,
                    WEEK,
                )

            PLACEMENTS_IN_MONTH.rawName ->
                PlacementCount(
                    event = (args.first() as PassableValue.StringValue).value,
                    MONTH,
                )

            PLACEMENTS_SINCE_INSTALL.rawName,
            ->
                PlacementCount(
                    event = (args.first() as PassableValue.StringValue).value,
                    period = INSTALL,
                )

            REVIEW_REQUESTS_IN_HOUR.rawName ->
                ReviewRequestCount(HOUR)

            REVIEW_REQUESTS_IN_DAY.rawName ->
                ReviewRequestCount(DAY)

            REVIEW_REQUESTS_IN_WEEK.rawName ->
                ReviewRequestCount(WEEK)

            REVIEW_REQUESTS_IN_MONTH.rawName ->
                ReviewRequestCount(MONTH)

            REVIEW_REQUESTS_IN_YEAR.rawName ->
                ReviewRequestCount(YEAR)

            REVIEW_REQUESTS_TOTAL.rawName ->
                ReviewRequestCount(INSTALL)

            REQUEST_REVIEW.rawName ->
                RequestReview

            else -> null
        }
    }
}

sealed interface TimeSince {
    val event: String
    val propertyRequest: ComputedPropertyRequestType

    suspend operator fun invoke(storage: CoreDataManager): Int =
        storage.getComputedPropertySinceEvent(
            null,
            ComputedPropertyRequest(
                propertyRequest,
                event,
            ),
        ) ?: 0
}

sealed interface InPeriod {
    enum class Period {
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR,
        INSTALL,
    }

    val event: String
}
