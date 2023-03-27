package ai.platon.pulsar.driver.report

import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

class TaskUpdateEvent(
    val serverTaskId: String,
    val status: String,
    val reason: String,
    val data: String
)


@RestController
@CrossOrigin
@RequestMapping(
    "report",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ReportController(
    val applicationContext: ApplicationContext,
) {

    private val reportService = ReportService.instance

    @PostConstruct
    fun init() {
        println("pulsar driver report controller initialized")
    }

    /**
     * @param sql The sql to execute
     * @return The response
     * */
    @PostMapping("task_update")
    fun execute(@RequestBody scrapeResponse: ScrapeResponse): UInt {
        println("kcdebug. Task update event: $scrapeResponse")
        val waitReportTask = reportService.getTask(scrapeResponse.uuid!!)
        // TODO coroutine handle
        if (waitReportTask != null) {
            waitReportTask.onProcess(convertResponse(scrapeResponse))
            reportService.removeTask(scrapeResponse.uuid!!)
        } else {
            println("Task not found: ${scrapeResponse.uuid}")
        }

        return 0u;
    }

    private fun convertResponse(
        scrapeResponse: ai.platon.pulsar.rest.api.entities.ScrapeResponse,
    ): ai.platon.pulsar.driver.ScrapeResponse {
        var toResponse =  ai.platon.pulsar.driver.ScrapeResponse()
        toResponse.pageStatusCode = scrapeResponse.pageStatusCode
        toResponse.statusCode = scrapeResponse.statusCode
        toResponse.status = scrapeResponse.status
        toResponse.isDone =  scrapeResponse.isDone
        toResponse.pageContentBytes =  scrapeResponse.pageContentBytes
        toResponse.resultSet =  scrapeResponse.resultSet
        return toResponse
    }

}