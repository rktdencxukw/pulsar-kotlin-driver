package ai.platon.scent.driver

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Driver(
    private val server: String,
    private val authToken: String,
    private val httpTimeout: Duration = Duration.ofMinutes(3),
) : AutoCloseable {
    var timeout = Duration.ofSeconds(120)
    private val scrapeBaseUri = "http://$server:8182/api/x/a"
    private val scrapeApi = "$scrapeBaseUri/q"
    private val statusApi = "$scrapeBaseUri/status"

    private val userBaseUri = "http://$server:8182/api/users/$authToken"
    private val dashboardApi = "$userBaseUri/dashboard"
    private val downloadApi = "$userBaseUri/download"
    private val statusQueryApi = "$scrapeBaseUri/status/q"

    private val httpClient = HttpClient.newHttpClient()

    @Throws(ScrapeException::class)
    fun submit(sql: String, asap: Boolean = false) = submitTask(sql, asap)

    @Throws(ScrapeException::class)
    fun submitAll(sqls: Collection<String>) = sqls.map { submit(it) }

    fun dashboard(): Dashboard {
        val request = HttpRequest.newBuilder().uri(URI.create(dashboardApi)).timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return createGson().fromJson(httpResponse.body(), Dashboard::class.java)
    }

    fun findById(id: String): ScrapeResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusApi?id=$id&authToken=$authToken"))
            .timeout(httpTimeout).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return createGson().fromJson(httpResponse.body(), ScrapeResponse::class.java)
    }

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
    private fun submitTask(sql: String, asap: Boolean = false): String {
        val requestEntity = ScrapeRequest(authToken, sql, "HIGHER3", asap = asap)
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
