package ai.platon.pulsar.driver.scrape_node.services

import ai.platon.pulsar.driver.pojo.WaitSubmitResponseTask
import ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.persist.metadata.IpType
import java.util.concurrent.ConcurrentHashMap

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

    fun getAll(): List<ScrapeNode> {
        return cache.values.toList()
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
        cache.values.removeIf { it.userName == sessionId }
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