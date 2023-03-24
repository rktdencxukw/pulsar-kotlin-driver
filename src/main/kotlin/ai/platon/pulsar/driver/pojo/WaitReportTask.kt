package ai.platon.pulsar.driver.pojo

class WaitReportTask(
    val serverTaskId: String,
    val sql: String,
    val onProcess: (String) -> UInt
) {
}