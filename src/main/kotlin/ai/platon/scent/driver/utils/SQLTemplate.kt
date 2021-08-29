package ai.platon.scent.driver.utils

class SQLTemplate(
    val template: String,
) {
    fun createSQL(url: String) = createInstance(url).sql

    fun createInstance(url: String) = SQLInstance(url, this)

    override fun toString() = template
}

class SQLInstance(
    val url: String,
    val template: SQLTemplate,
) {
    val sql = createSQL()

    override fun toString() = sql

    private fun createSQL(): String {
        val sanitizedUrl = SQLUtils.sanitizeUrl(url)
        return template.template.replace("{{url}}", sanitizedUrl)
            .replace("@url", "'$sanitizedUrl'")
            .replace("{{snippet: url}}", "'$sanitizedUrl'")
    }
}
