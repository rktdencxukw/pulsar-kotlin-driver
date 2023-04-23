package ai.platon.pulsar.driver.report

import ai.platon.pulsar.driver.pojo.WaitReportTask
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

//@Service
class ReportService {
    private val tasks = ConcurrentHashMap<String, WaitReportTask>()
    private val httpClient = HttpClient.newHttpClient()

    fun appendTask(serverTaskId: String, task: WaitReportTask) {
        tasks[serverTaskId] = task
    }

    fun removeTask(serverTaskId: String) {
        tasks.remove(serverTaskId)?.let {
            if (it.viaWebsocket.not()) {
                try {
                    val postRequest =
                        HttpRequest.newBuilder()
                            .uri(URI.create("${it.cleanUri}?uuid=${it.serverTaskId}"))
                            .timeout(Duration.ofSeconds(10))
                            .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                            .POST(HttpRequest.BodyPublishers.ofString(""))
                            .build()
                    httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getTask(serverTaskId: String): WaitReportTask? {
        return tasks[serverTaskId]
    }

    companion object {
        @JvmStatic // 要加这个才是
        val instance = ReportService()
    }
}