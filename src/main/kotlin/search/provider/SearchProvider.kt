package work.slhaf.search.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.slhaf.search.Source
import work.slhaf.search.WebContent

abstract class SearchProvider {

    val cache = mutableMapOf<String, List<WebContent>>()

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun search(source: Source, pageSize: Int): List<WebContent> {
        val result = doSearch(source.query, pageSize)
        putCache(source.id, result)
        return result
    }

    protected abstract fun doSearch(query: String, pageSize: Int): List<WebContent>

    open fun close() {}

    fun putCache(queryId: String, result: List<WebContent>) {
        cache[queryId] = result
    }

    fun selectPage(queryId: String, page: Int, pageSize: Int): List<WebContent> {
        val cached = cache[queryId] ?: return emptyList()
        if (page < 1 || pageSize <= 0) return emptyList()

        val totalCount = cached.size
        val start = (page - 1) * pageSize
        if (start !in 0 until totalCount) return emptyList()

        val end = (start + pageSize).coerceAtMost(totalCount)
        return cached.subList(start, end)
    }
}
