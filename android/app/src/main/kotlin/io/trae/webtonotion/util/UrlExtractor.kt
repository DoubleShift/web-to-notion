package io.trae.webtonotion.util

object UrlExtractor {
    private val URL_REGEX = Regex("https?://[^\\s]+")

    fun extract(text: String): String? = URL_REGEX.find(text)?.value
}
