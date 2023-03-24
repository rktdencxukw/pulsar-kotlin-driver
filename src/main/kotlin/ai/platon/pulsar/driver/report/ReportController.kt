package ai.platon.pulsar.driver.report

import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

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
    /**
     * @param sql The sql to execute
     * @return The response
     * */
    @PostMapping("task_update")
    fun execute(@RequestBody scrapeResponse: ScrapeResponse): UInt {
        println("Task update event: $scrapeResponse")
        val waitReportTask = reportService.getTask(scrapeResponse.uuid!!)
        // TODO coroutine handle
        if (waitReportTask != null) {
            waitReportTask.onProcess(scrapeResponse.pageStatus)
        } else {
            println("Task not found: ${scrapeResponse.uuid}")
        }

        return 0u;
    }

}