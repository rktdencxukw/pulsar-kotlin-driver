package ai.platon.pulsar.driver.utils

object SQLUtils {
    /**
     * A single quote is replaced to be %27 by URLEncoder, to properly handle a url-encoded url
     * */
    const val SINGLE_QUOTE_PLACE_HOLDER = "^27"

    /**
     * Sanitize an url before it can be used in an X-SQL, e.g.
     * https://www.amazon.com/s?k=Baby+Girls'+One-Piece+Footies&rh=node:2475809011&page=1
     * is sanitized to be
     * https://www.amazon.com/s?k=Baby+Girls^27+One-Piece+Footies&rh=node:2475809011&page=1
     * */
    fun sanitizeUrl(url: String): String {
        return url.replace("'", SINGLE_QUOTE_PLACE_HOLDER)
    }

    fun unsanitizeUrl(sanitizedUrl: String): String {
        return sanitizedUrl.replace(SINGLE_QUOTE_PLACE_HOLDER, "'")
    }
}
