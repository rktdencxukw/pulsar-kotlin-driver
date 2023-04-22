package ai.platon.pulsar.examples

import ai.platon.pulsar.driver.Driver
import ai.platon.pulsar.driver.report.ReportService
import ai.platon.pulsar.driver.report.TaskUpdateEvent
import ai.platon.pulsar.driver.utils.SQLTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files


import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*

@SpringBootApplication
@ComponentScan(basePackages = ["ai.platon.pulsar.driver", "ai.platon.pulsar.examples"])
open class DriverTestMain(
) {
    private val logger = LoggerFactory.getLogger(DriverTestMain::class.java)
}


@RestController
class IndexController {
    // 定义index接口
    @GetMapping("/index")
    fun index(): String {
        return "Hello, Kotlin for Spring Boot!!"
    }
}



@Component
class AppStartupRunner : ApplicationRunner
{
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Throws(Exception::class)
    override fun run(args: ApplicationArguments) {
        println("Application started , with args: $args")
        if (args.containsOption("test_run")) {
            println("test option found")
            testRun(mongoTemplate)
            println("test finished")
        }
    }

    companion object {
    }
}
fun main(args: Array<String>) {
    runApplication<DriverTestMain>(*args) {
//        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master")
        setLogStartupInfo(true)
    }
//    SpringApplication.run(DriverTestMain::class.java, *args)
}

fun testRun(mongoTemplate: MongoTemplate) {
    val server = "127.0.0.1"
    val authToken = "b12yCTcfWnw0dFS767eadcea57a6ce4077348b7b3699578"

//    val urls = ResourceLoader.readAllLines("sites/amazon/asin/urls.txt").shuffled().take(10)
//    val sqlTemplate =
    """select
//            |   dom_first_text(dom, '#productTitle') as `title`,
//            |   dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
//            |   dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
//            |   array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
//            |   dom_base_uri(dom) as `baseUri`
            |from
//            |   load_and_select('{{url}} -i 1h', ':root')
//            |""".trimMargin()
    val urls = arrayOf("https://www.theblockbeats.info/newsflash")
    val sqlTemplate = """
select
   dom_all_texts(dom, '.news-flash-item-title') as titles
from
load_and_select('{{url}} -i 1h', 'body')
""".trimIndent()

    val simpMessagingTemplate = SimpMessagingTemplate(null)
    TODO("not implement")
    Driver(server, authToken, "", mongoTemplate, simpMessagingTemplate, ObjectMapper() ).use { driver ->
        val ids = mutableSetOf<String>()
        urls.forEach { url ->
            val sql = SQLTemplate(sqlTemplate).createSQL(url)
//            val id = driver.submit(sql, asap = true)
            val id = driver.submitWithProcess(sql, null, null) { println("got response in upper level $it"); 0u }
//            ids.add(id)
        }
        val path = Files.createTempFile("pulsar-", ".txt")
        Files.write(path, ids)
        println("Ids are written to $path")

        val gson = driver.createGson()

        // we may want to check the status of a scrape task with a specified id
        val status = driver.findById(ids.first())
        println(gson.toJson(status))

        // we may want to check our dashboard
        val dashboard = driver.dashboard()
        println(gson.toJson(dashboard))

        // we download all the scrape results
        val results = driver.download(pageSize = 10)
        println(gson.toJson(results))
    }
}
