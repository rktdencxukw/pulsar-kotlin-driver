package ai.platon.pulsar.driver

import java.time.OffsetDateTime

/**
 * Pagination
 * */
data class Page<T>(
    var number: Int,
    var size: Int,
    var totalPages: Int,
    var totalElements: Long,
    var numberOfElements: Int,
    var content: List<T>,
) {
    val offset get() = number * size
    val pageSize get() = size
    val pageNumber get() = number
    val hasPrevious get() = number > 0
    val hasNext get() = number + 1 < totalPages
    val isFirst get() = !hasPrevious
    val isLast get() = !hasNext
    val isEmpty get() = numberOfElements == 0
    val isNotEmpty get() = !isEmpty
}

/**
 * The scrape response
 * */
data class ScrapeResponse(
    var id: String? = null,
    var status: String? = null,
    var pageStatus: String? = null,

    var isDone: Boolean = false,
    var estimatedWaitTime: Long = -1,
    var pageContentBytes: Int = 0,

    var resultSet: List<Map<String, Any?>>? = null,

    var statusCode: Int = ResourceStatus.CREATED.value(),
    var pageStatusCode: Int = ResourceStatus.CREATED.value(),

    var timestamp: String? = null,
)

/**
 * Compacted scrape response, only important fields are included
 * */
data class CompactedScrapeResponse(
    var id: String? = null,

    var statusCode: Int = ResourceStatus.CREATED.value(),
    var pageStatusCode: Int = ResourceStatus.CREATED.value(),

    var pageContentBytes: Int = 0,
    var isDone: Boolean = false,
    var url: String? = null,
    var resultSet: List<Map<String, Any?>>? = null,
    var collectionId: Int = 0,
)

/**
 * The scrape summary
 * */
data class ScrapeSummary(
    var count: Long = 0,
    var pendingCount: Long = 0,
    var successCount: Long = 0,
    var finishedCount: Long = 0,

    var startTime: OffsetDateTime,
    var endTime: OffsetDateTime,
)

/**
 * Visit counters
 * */
data class VisitCounter(
    var visitsLastMinute: Int = 0,
    var visitsLastTenMinutes: Int = 0,
    var visitsLastHour: Int = 0,
    var visitsLastDay: Int = 0,

    var maxVisitsPMinute: Int = 0,
    var maxVisitsPTenMinutes: Int = 0,
    var maxVisitsPHour: Int = 0,
    var maxVisitsPDay: Int = 0,
)

/**
 * The user profile
 * */
data class UserProfile(
    var balance: Float = 0.0f,

    var visitCounter: VisitCounter? = null,

    var tel: String? = null,
    var address: String? = null,
)

/**
 * The user's dashboard
 * */
data class Dashboard(
    var authToken: String,
    var timestamp: OffsetDateTime,
    var profile: UserProfile?,
    var monthlySummary: List<ScrapeSummary> = listOf(),
    var dailySummary: List<ScrapeSummary> = listOf(),
    var hourlySummary: List<ScrapeSummary> = listOf()
)

/**
 * The exception information
 * */
data class ExceptionInfo(
    val timestamp: String?,
    val status: String? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
)

/**
 * The scrape exception on client side
 * */
class ScrapeException(val info: ExceptionInfo): Exception(
    "status: ${info.status}, error: ${info.error}, message: ${info.message}"
)
