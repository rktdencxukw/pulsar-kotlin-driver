package ai.platon.scent.driver

import java.lang.Exception
import java.time.OffsetDateTime

data class Page<T>(
    var number: Int,
    var size: Int,
    var totalPages: Int,
    var totalElements: Long,
    var numberOfElements: Int,
    var first: Boolean,
    var last: Boolean,
    var content: List<T>,
)

data class ScrapeResponse(
    var id: String? = null,
    var status: String? = null,
    var pageStatus: String? = null,

    var isDone: Boolean = false,
    var estimatedWaitTime: Long = -1,
    var pageContentBytes: Int = 0,

    var resultSet: List<Map<String, Any>>? = null,

    var statusCode: Int = ResourceStatus.CREATED.value(),
    var pageStatusCode: Int = ResourceStatus.CREATED.value(),

    var timestamp: String? = null,
)

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

data class ExceptionInfo(
    val timestamp: String?,
    val status: String? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
)

data class ScrapeSummary(
    var count: Long = 0,
    var pendingCount: Long = 0,
    var successCount: Long = 0,
    var finishedCount: Long = 0,

    var startTime: OffsetDateTime,
    var endTime: OffsetDateTime,
)

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

data class UserProfile(
    var balance: Float = 0.0f,

    var visitCounter: VisitCounter? = null,

    var tel: String? = null,
    var address: String? = null,
)

data class Dashboard(
    var authToken: String,
    var timestamp: OffsetDateTime,
    var profile: UserProfile?,
    var monthlySummary: List<ScrapeSummary> = listOf(),
    var dailySummary: List<ScrapeSummary> = listOf(),
    var hourlySummary: List<ScrapeSummary> = listOf()
)

class ScrapeException(val info: ExceptionInfo): Exception(
    "status: ${info.status}, error: ${info.error}, message: ${info.message}"
)
