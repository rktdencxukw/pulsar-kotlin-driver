package ai.platon.pulsar.driver

/**
 * The scrape request
 * */
data class ScrapeRequest(
    var authToken: String,
    var sql: String,
    var priority: String = "HIGHER2",
    var asap: Boolean = false,
)
