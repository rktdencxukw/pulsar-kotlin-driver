package ai.platon.pulsar.driver

import ai.platon.pulsar.driver.pojo.WaitReportTask
import ai.platon.pulsar.driver.report.ReportController
import ai.platon.pulsar.driver.report.ReportService
import ai.platon.pulsar.driver.utils.NetUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import java.io.IOException
import java.lang.reflect.Type
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime
import javax.annotation.PostConstruct

/**
 * The pulsar driver
 * */
class Driver(
    private val server: String,
    private val authToken: String,
    private val reportServer: String,
    private val mongoTemplate: MongoTemplate,
    private val httpTimeout: Duration = Duration.ofMinutes(3),
) : AutoCloseable {
    var timeout = Duration.ofSeconds(120)
    private val scrapeBaseUri = "http://$server:8182/api/x"
    private val scrapeJsonApi = "$scrapeBaseUri/e_json_async"
    private val statusApi = "$scrapeBaseUri/status"
    private val statusesApi = "$scrapeBaseUri/statuses"

    private val userBaseUri = "http://$server:8182/api/users/$authToken"
    private val dashboardApi = "$userBaseUri/dashboard"
    private val countApi = "$userBaseUri/count"
    private val fetchApi = "$userBaseUri/fetch"
    private val downloadApi = "$userBaseUri/download"
    private val statusQueryApi = "$scrapeBaseUri/status/q"

    private val httpClient = HttpClient.newHttpClient()
    private var logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val reportService = ReportService.instance
    private lateinit var externalReportServer: String
    private lateinit var internalIps: List<String>

    init {
        val externalIp = NetUtils.selfPublicIp!!
        println("self public ip: $externalIp")
        require(externalIp.isNotBlank()) { "Can't get self public ip" }
        externalReportServer = "http://$externalIp:2718/exotic"

        internalIps = NetUtils.getInternalIps();
        println("self internal ips: $internalIps")
    }

    private fun choiceInternalIp(ip: String): String {
        return internalIps.random()
    }
    /**
     * Submit an SQL to scrape
     * */
    @Throws(ScrapeException::class)
    fun submit(sql: String, priority: Int = 2, asap: Boolean = false) = submitTask(sql, priority, asap)

    /**
     * Submit an SQL to scrape
     * scrapeServer 83.234.13.23:8081 likes
     * */
    @Throws(ScrapeException::class)
    fun submitWithProcess(sql: String, scrapeServer: String?, onProcess: (ScrapeResponse) -> UInt) =
        submitTask(sql, 2, false, onProcess, scrapeServer)

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
//    fun findById(id: String): ScrapeResponse {
//        val request = HttpRequest.newBuilder()
//            .uri(URI.create("$statusApi?id=$id&authToken=$authToken"))
//            .timeout(httpTimeout).GET().build()
//        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
//        return createGson().fromJson(httpResponse.body(), ScrapeResponse::class.java)
//    }

    fun findById(id: String): ScrapeResponse? {
        return mongoTemplate.findById<ScrapeResponse>(id)
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

    //    A类地址：10.0.0.0 - 10.255.255.255
//    B类地址：172.16.0.0 - 172.31.255.255
//    C类地址：192.168.0.0 -192.168.255.255
    private fun isIpInternal(ip: String): Boolean {
        return ip.startsWith("127.0.0.1") || ip.startsWith("localhost") || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith(
            "172.16."
        );
    }

    @Throws(ScrapeException::class, IOException::class, InterruptedException::class)
    private fun submitTask(
        sql: String,
        priority: Int,
        asap: Boolean = false,
        onProcess: ((ScrapeResponse) -> UInt)? = null,
        scrapeServer: String? = null
    ): String {
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
        var scrapeServerUri = scrapeJsonApi
        var curReportServer = reportServer
        if (!scrapeServer.isNullOrEmpty()) {
            scrapeServerUri = "http://$scrapeServer/api/x/e_json_async"
            if (isIpInternal(scrapeServer)) {
                // TODO 暂时exotic 启动时指定
//                curReportServer = "http://192.168.68.137:2718/exotic"
            } else {
                curReportServer = externalReportServer
            }
        }
        logger.debug(
            "scrapeServerUri: {}, curReportServer: {}, scrapeServer: {}, sql: {}",
            scrapeServerUri,
            curReportServer,
            scrapeServer,
            sql
        )
        val requestEntity =
            ScrapeRequest(authToken, sql, priorityName, asap = asap, "$curReportServer/report/task_update")
        val request = post(scrapeServerUri, requestEntity)
//        val request = postString(scrapeApi, sql)

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
        } else {
//            val scrapeResponse = createGson().fromJson(body, ScrapeRequestSubmitResponse::class.java)
            val scrapeResponse = ObjectMapper().readValue(body, ScrapeRequestSubmitResponseTemp::class.java)
            val serverTaskId = scrapeResponse.uuid!!
            if (scrapeResponse.code == 0) {
                if (onProcess != null) {
                    reportService.appendTask(serverTaskId, WaitReportTask(serverTaskId, sql, onProcess))
                    return serverTaskId
                }
            } else {
                println("submitTask failed: $body")
            }
        }
        return ""
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

    private fun postString(url: String, requestEntity: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(3))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(requestEntity))
            .build()
    }
}


/*
临时性。exotic中用到的scent与这里冲突了
 */
data class ScrapeRequestSubmitResponseTemp(
    var uuid: String? = null,
    var code: Int = 0,
    var errorMessage: String = ""
) {
}
