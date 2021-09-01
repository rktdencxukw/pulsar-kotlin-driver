package ai.platon.scent.examples

import ai.platon.scent.driver.Driver
import ai.platon.scent.driver.utils.SQLTemplate
import ai.platon.scent.driver.utils.ResourceLoader
import java.nio.file.Files

fun main() {
    val server = "master"
    val authToken = "b12yCTcfWnw0dFS767eadcea57a6ce4077348b7b3699578"

    val urls = ResourceLoader.readAllLines("asin/asin-urls.txt").shuffled().take(10)
    val sqlTemplate =
        """select
            |   dom_first_text(dom, '#productTitle') as `title`,
            |   dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
            |   dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
            |   array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
            |   dom_base_uri(dom) as `baseUri`
            |from
            |   load_and_select('{{url}} -i 1h', ':root')
            |""".trimMargin()

    Driver(server, authToken).use { driver ->
        val ids = mutableSetOf<String>()
        urls.forEach { url ->
            val sql = SQLTemplate(sqlTemplate).createSQL(url)
            val id = driver.submit(sql, asap = true)
            ids.add(id)
        }
        val path = Files.createTempFile("scent-", ".txt")
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
