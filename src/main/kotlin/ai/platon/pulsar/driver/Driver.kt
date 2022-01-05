package ai.platon.pulsar.driver

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.time.Duration
import java.time.OffsetDateTime

/**
 * The pulsar driver
 * */
class Driver(
    private val server: String,
    private val authToken: String,
    private val httpTimeout: Duration = Duration.ofMinutes(3),
) : AutoCloseable {
    var timeout = Duration.ofSeconds(120)
    private val scrapeBaseUri = "http://$server:8182/api/x/a"
    private val scrapeApi = "$scrapeBaseUri/q"
    private val statusApi = "$scrapeBaseUri/status"
    private val statusesApi = "$scrapeBaseUri/statuses"

    private val userBaseUri = "http://$server:8182/api/users/$authToken"
    private val dashboardApi = "$userBaseUri/dashboard"
    private val countApi = "$userBaseUri/count"
    private val fetchApi = "$userBaseUri/fetch"
    private val downloadApi = "$userBaseUri/download"
    private val statusQueryApi = "$scrapeBaseUri/status/q"

    private val httpClient = HttpClient.newHttpClient()

    /**
     * Submit an SQL to scrape
     * */
    @Throws(ScrapeException::class)
    fun submit(sql: String, priority: Int = 2, asap: Boolean = false) = submitTask(sql, priority, asap)

    /**
     * Submit SQLs to scrape
     * */
    @Throws(ScrapeException::class)
    fun submitAll(
        sqls: Iterable<String>, priority: Int = 2, asap: Boolean = false
    ) = sqls.map { submit(it, priority, asap) }

    /**
     * Show my dashboard
     * */
    fun dashboard(): Dashboard {
        val request = HttpRequest.newBuilder().uri(URI.create(dashboardApi)).timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return createGson().fromJson(httpResponse.body(), Dashboard::class.java)
    }

    /**
     * Find a scrape response by scrape task id which returned by [submit]
     * */
    fun findById(id: String): ScrapeResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusApi?id=$id&authToken=$authToken"))
            .timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return createGson().fromJson(httpResponse.body(), ScrapeResponse::class.java)
    }

    /**
     * Find scrape responses by scrape task ids which returned by [submit]
     * */
    fun findAllByIds(ids: Iterable<String>): List<ScrapeResponse> {
        val idsString = ids.joinToString("\n").trim()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusesApi?authToken=$authToken"))
            .timeout(httpTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(idsString))
            .build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        val body = httpResponse.body()
        if (!body.contains("estimatedWaitTime")) {
            return listOf()
        }

        return createGson().fromJson(body, object : TypeToken<List<ScrapeResponse>>() {}.type)
    }

    /**
     * Count all submitted tasks
     * */
    fun count(): Long {
        val request = HttpRequest.newBuilder().uri(URI.create(countApi)).timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return httpResponse.body()?.toLongOrNull() ?: -1
    }

    /**
     * Fetch [limit] scrape responses start from [offset]
     * */
    fun fetch(offset: Long = 0, limit: Int = 500): List<CompactedScrapeResponse> {
        val uri = "$fetchApi?offset=$offset&limit=$limit"
        val request = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        val listType = object : TypeToken<List<CompactedScrapeResponse>>() {}.type
        return createGson().fromJson(httpResponse.body(), listType)
    }

    /**
     * Download scrape responses page by page
     * */
    fun download(pageNumber: Int = 0, pageSize: Int = 500): Page<CompactedScrapeResponse> {
        val uri = "$downloadApi?pageNumber=$pageNumber&pageSize=$pageSize"
        val request = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        val listType = object : TypeToken<Page<CompactedScrapeResponse?>?>() {}.type
        return createGson().fromJson(httpResponse.body(), listType)
    }

    fun createGson(): Gson {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(OffsetDateTime::class.java, object : JsonDeserializer<OffsetDateTime> {
            @Throws(JsonParseException::class)
            override fun deserialize(ele: JsonElement, type: Type, ctx: JsonDeserializationContext?): OffsetDateTime {
                return OffsetDateTime.parse(ele.asString)
            }
        })

        builder.registerTypeAdapter(OffsetDateTime::class.java, object : JsonSerializer<OffsetDateTime> {
            override fun serialize(time: OffsetDateTime, type: Type, ctx: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(time.toString())
            }
        })

        return builder.create()
    }

    override fun close() {
    }

    @Throws(ScrapeException::class, IOException::class, InterruptedException::class)
    private fun submitTask(sql: String, priority: Int, asap: Boolean = false): String {
        val priorityName = when (priority) {
            3 -> "HIGHER3"
            2 -> "HIGHER2"
            1 -> "HIGHER1"
            0 -> "NORMAL"
            -1 -> "LOWER1"
            -2 -> "LOWER2"
            -3 -> "LOWER3"
            else -> "LOWER3"
        }
        val requestEntity = ScrapeRequest(authToken, sql, priorityName, asap = asap)
        val request = post(scrapeApi, requestEntity)

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val body = response.body()
        if (response.statusCode() != 200) {
            /**
             * {
                "timestamp" : "2021-08-28T17:12:33.567+00:00",
                "status" : 401,
                "error" : "Unauthorized",
                "message" : "",
                "path" : "/api/x/a/q"
                }
             * */
            // println(response.body())
            val info = createGson().fromJson(body, ExceptionInfo::class.java)
            throw ScrapeException(info)
        }

        return body
    }

    private fun post(url: String, requestEntity: Any): HttpRequest {
        val requestBody = Gson().toJson(requestEntity)
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(3))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
    }
}
