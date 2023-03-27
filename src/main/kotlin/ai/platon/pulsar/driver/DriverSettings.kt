package ai.platon.pulsar.driver

data class DriverSettings (
    val scrapeServer: String,
    val authToken : String,
    val scrapeServerPort: Int,
    var scrapeServerContextPath: String
)