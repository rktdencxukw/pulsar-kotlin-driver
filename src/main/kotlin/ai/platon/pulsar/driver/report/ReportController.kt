package ai.platon.pulsar.driver.report

import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
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
    private val mongoTemplate: MongoTemplate,
) {

    private val reportService = ReportService.instance

    private var logger: Logger = LoggerFactory.getLogger(ReportController::class.java)

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
        logger.debug("Task update event: $scrapeResponse")
        val waitReportTask = reportService.getTask(scrapeResponse.uuid!!)
        // TODO coroutine handle
        if (waitReportTask != null) {
            try {
                val r = convertResponse(scrapeResponse)
                mongoTemplate.save(r)
                waitReportTask.onProcess(r)
                reportService.removeTask(scrapeResponse.uuid!!)
            } catch (e: Exception) {
                logger.error("Error while processing task: ${scrapeResponse.uuid}. err: {}\nresponse:{}, waitReportTask:{}", e, scrapeResponse, waitReportTask)
            }
        } else {
            logger.warn("Task not found: ${scrapeResponse.uuid}")
        }

        return 0u;
    }

    private fun convertResponse(
        scrapeResponse: ai.platon.pulsar.rest.api.entities.ScrapeResponse,
    ): ai.platon.pulsar.driver.ScrapeResponse {
        var toResponse =  ai.platon.pulsar.driver.ScrapeResponse()
        toResponse.id = scrapeResponse.uuid
        toResponse.pageStatusCode = scrapeResponse.pageStatusCode
        toResponse.statusCode = scrapeResponse.statusCode
        toResponse.status = scrapeResponse.status
        toResponse.isDone =  scrapeResponse.isDone
        toResponse.pageContentBytes =  scrapeResponse.pageContentBytes
        toResponse.resultSet =  scrapeResponse.resultSet
        return toResponse
    }

}