package ai.platon.pulsar.driver.pojo

import ai.platon.pulsar.driver.ScrapeResponse


class WaitReportTask(
    val cleanUri: String,
    val serverTaskId: String,
    val sql: String,
    val onProcess: (ScrapeResponse) -> UInt,
    val viaWebsocket: Boolean = false
) {
}