package ai.platon.pulsar.driver.pojo

import ai.platon.pulsar.driver.ScrapeResponse


class WaitSubmitResponseTask(
    val cleanUri: String,
    val sql: String,
    val onProcess: ((ScrapeResponse) -> UInt)?
) {
    var serverTaskId: String? = null
}