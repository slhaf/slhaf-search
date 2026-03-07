package work.slhaf.search.provider

import work.slhaf.search.WebContent

interface SearchProvider {
    fun search(query: String, size: Int): List<WebContent>

    fun close() {}
}
