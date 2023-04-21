package ai.platon.pulsar.driver.scrape_node.services

import ai.platon.pulsar.common.websocket.Command
import ai.platon.pulsar.driver.pojo.WaitSubmitResponseTask
import ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.persist.metadata.IpType
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

//@Service
class ScrapeNodeService(
) {
    private val cache = ConcurrentHashMap<String, ScrapeNode>()
    private val waitingSubmitResponse = ConcurrentHashMap<Long, WaitSubmitResponseTask>()

    fun get(nodeId: String): ScrapeNode? {
        if (cache.contains(nodeId)) {
            return cache[nodeId]
        } else {
            return null
        }
    }

    fun getNodesByIpType(ipType: IpType): List<ScrapeNode> {
        return cache.values.filter { it.ipType == ipType }
    }

    fun put(ruleId: String, node: ScrapeNode) {
        cache[ruleId] = node
    }

    fun cleanCache() {
        cache.clear()
    }

    fun kick(nodeId: String) {
        cache.remove(nodeId)
    }

    fun removeBySessionId(sessionId: String) {
        cache.values.removeIf { it.wsSessionId == sessionId }
    }

    fun removeWaitingSubmitResponse(reqId: Long) {
        waitingSubmitResponse.remove(reqId)
    }


    fun putWaitingSubmitResponse(reqId: Long, onProcess: WaitSubmitResponseTask) {
        waitingSubmitResponse[reqId] = onProcess
    }

    fun getWaitingSubmitResponse(reqId: Long): WaitSubmitResponseTask? {
        return waitingSubmitResponse[reqId]
    }


    companion object {
        @JvmStatic
        val instance = ScrapeNodeService()
    }
}