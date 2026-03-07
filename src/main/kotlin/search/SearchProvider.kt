package work.slhaf.search

interface SearchProvider {
    fun search(query: String, size: Int): List<WebContent>
}