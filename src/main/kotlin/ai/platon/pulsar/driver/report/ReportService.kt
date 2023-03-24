package ai.platon.pulsar.driver.report

import ai.platon.pulsar.driver.pojo.WaitReportTask
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

//@Service
class ReportService {
    private val tasks = ConcurrentHashMap<String, WaitReportTask>()

    fun appendTask(serverTaskId: String, task: WaitReportTask) {
        tasks[serverTaskId] = task
    }

    fun removeTask(serverTaskId: String) {
        tasks.remove(serverTaskId)
    }

    fun getTask(serverTaskId: String): WaitReportTask? {
        return tasks[serverTaskId]
    }

    // 转为单例
    companion object {
        @JvmStatic // 要加这个才是
        val instance = ReportService()
    }
}