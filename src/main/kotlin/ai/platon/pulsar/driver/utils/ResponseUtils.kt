package ai.platon.pulsar.driver.utils

class ResponseUtils {
    companion object {
        fun convertResponse(
            scrapeResponse: ai.platon.pulsar.rest.api.entities.ScrapeResponse,
        ): ai.platon.pulsar.driver.ScrapeResponse {
            var toResponse = ai.platon.pulsar.driver.ScrapeResponse()
            toResponse.id = scrapeResponse.uuid
            toResponse.pageStatusCode = scrapeResponse.pageStatusCode
            toResponse.statusCode = scrapeResponse.statusCode
            toResponse.status = scrapeResponse.status
            toResponse.isDone = scrapeResponse.isDone
            toResponse.pageContentBytes = scrapeResponse.pageContentBytes
            toResponse.resultSet = scrapeResponse.resultSet
            return toResponse
        }
    }
}