package work.slhaf.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import work.slhaf.search.provider.McpBingSearch
import work.slhaf.search.provider.SearchProvider

object SearchRouter {

    private val providers = mapOf<String, SearchProvider>(
        "default" to McpBingSearch,
        "bing" to McpBingSearch
    )

    fun search(
        mode: SearchMode = SearchMode.NORMAL,
        provider: String = "default",
        query: String,
        size: Int = 30
    ): Flow<SearchEvent> = flow {
        val searchProvider = if (provider == "default") {
            providers.values.first()
        } else {
            providers[provider] ?: providers.values.first()
        }

        when (mode) {
            SearchMode.NORMAL -> search(searchProvider, query, size)
            SearchMode.ENHANCED -> enhancedSearch(searchProvider, query, size)
            SearchMode.AGENTIC -> agenticSearch(searchProvider, query, size)
        }
    }

    private suspend fun FlowCollector<SearchEvent>.search(
        provider: SearchProvider,
        query: String,
        size: Int
    ) {
        emit(SearchEvent.stage(mode = SearchMode.NORMAL, content = "Searching...", query = query))
        provider.search(query, size)
    }

    private suspend fun FlowCollector<SearchEvent>.enhancedSearch(
        provider: SearchProvider,
        query: String,
        size: Int
    ) {
        emit(SearchEvent.stage(mode = SearchMode.ENHANCED, content = "Enhanced Searching...", query = query))
        TODO()
    }

    private suspend fun FlowCollector<SearchEvent>.agenticSearch(
        provider: SearchProvider,
        query: String,
        size: Int
    ) {
        emit(SearchEvent.stage(mode = SearchMode.AGENTIC, content = "Agentic Searching...", query = query))
        TODO()
    }

    fun closeAllProviders() {
        providers.values.toSet().forEach { provider ->
            runCatching { provider.close() }
        }
    }
}
