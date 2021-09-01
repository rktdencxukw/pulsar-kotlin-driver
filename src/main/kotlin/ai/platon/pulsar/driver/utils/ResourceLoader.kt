package ai.platon.pulsar.driver.utils

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

object ResourceLoader {

    fun readAllLines(fileResource: String): List<String> {
        return getResourceAsReader(fileResource)?.useLines {
            it.filter { it.isNotBlank() }.filter { !it.startsWith("#") }.toList()
        } ?: listOf()
    }

    fun getResourceAsReader(name: String): Reader? {
        return getResourceAsStream(name)?.let { InputStreamReader(it) }
    }

    fun getResourceAsStream(name: String): InputStream? {
        return try {
            val url = ResourceLoader::class.java.classLoader.getResource(name) ?: return null
            url.openStream()
        } catch (e: IOException) {
            System.err.println(String.format("Failed to read resource %s | %s", name, e.message))
            null
        }
    }
}
