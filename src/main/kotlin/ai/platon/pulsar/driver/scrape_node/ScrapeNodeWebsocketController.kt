package ai.platon.pulsar.driver.scrape_node

import ai.platon.pulsar.common.websocket.CommandResponse
import ai.platon.pulsar.common.websocket.ExoticResponse
import ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.driver.scrape_node.services.ScrapeNodeService
import ai.platon.pulsar.common.websocket.ScrapeNodeRegisterInfo
import ai.platon.pulsar.driver.ScrapeRequestSubmitResponseTemp
import ai.platon.pulsar.driver.pojo.WaitReportTask
import ai.platon.pulsar.driver.report.ReportService
import ai.platon.pulsar.driver.utils.ResponseUtils
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.IpType
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.*
import javax.annotation.PostConstruct


@Controller
class ScrapeNodeWebsocketController(
    private val simpMessagingTemplate: SimpMessagingTemplate?,
    private val mongoTemplate: MongoTemplate,
    private val ohObjectMapper: ObjectMapper
) {
    private val reportService = ReportService.instance
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val scrapeNodeService = ScrapeNodeService.instance


    @PostConstruct
    fun init() {
        println("init ScrapeNodeController")
        if (simpMessagingTemplate == null) {
            throw Exception("simpMessagingTemplate is null, must enable websocket")
        }
    }

    @MessageMapping("/scrape_register")
    @SendToUser("/queue/scrape_register")
    fun register(
        @Payload message: ScrapeNodeRegisterInfo,
        headerAccessor: SimpMessageHeaderAccessor,
//        user: Principal?,
    ): ExoticResponse<String> {
        var scrapeNode = ScrapeNode()
        scrapeNode.ipType = IpType.fromString(message.ipType)
        scrapeNode.fetchModeSupport = message.fetchModeSupport!!.split(",").map { FetchMode.fromString(it) }
        if (message.nodeId.isNullOrEmpty()) {
            scrapeNode.nodeId = UUID.randomUUID().toString()
        }
        scrapeNode.online = true
//        println("kcdebug. userName 0 : ${headerAccessor.user}")
//        println("kcdebug. userName: ${headerAccessor.user!!.name}")
        scrapeNode.userName = headerAccessor.user!!.name
        scrapeNodeService.put(scrapeNode.nodeId!!, scrapeNode)
        return ExoticResponse.okWithData(ohObjectMapper.writeValueAsString(scrapeNode.nodeId!!))
    }

    @MessageMapping("/scrape_task_submitted")
    fun scrapeTaskSubmitted(
        @Payload submitResponse: CommandResponse<ScrapeRequestSubmitResponseTemp>,
        headerAccessor: SimpMessageHeaderAccessor,
//        user: Principal?,
    ) {
        if (submitResponse.code != 0) {
            throw Error("scrape task submitted failed: $submitResponse")
            return
        }

        val task = scrapeNodeService.getWaitingSubmitResponse(submitResponse.reqId)
        task?.serverTaskId = submitResponse.data!!.uuid!!

        val serverTaskId = submitResponse.data!!.uuid!!
        if (task?.onProcess != null) {
            reportService.appendTask(serverTaskId, WaitReportTask("", serverTaskId, task.sql, task.onProcess))
            return
        }
    }

    @MessageMapping("/scrape_task_finished")
    fun scrapeTaskFinished(
        @Payload scrapeResponse: ScrapeResponse,
        headerAccessor: SimpMessageHeaderAccessor,
//        user: Principal?,
    ) {
        println("scrape task finished: $scrapeResponse")
        val waitReportTask = reportService.getTask(scrapeResponse.uuid!!)
        // TODO coroutine handle
        if (waitReportTask != null) {
            try {
                val r = ResponseUtils.convertResponse(scrapeResponse)
                mongoTemplate.save(r)
                waitReportTask.onProcess(r)
                reportService.removeTask(scrapeResponse.uuid!!)
            } catch (e: Exception) {
                logger.error(
                    "Error while processing task: ${scrapeResponse.uuid}. err: {}\nresponse:{}, waitReportTask:{}",
                    e,
                    scrapeResponse,
                    waitReportTask
                )
            }
        } else {
            logger.warn("Task not found: ${scrapeResponse.uuid}")
        }

    }
}