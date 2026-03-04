package work.slhaf.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

object SearchRouter {

    private val providers = mapOf<String, SearchProvider>()

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

    private suspend fun FlowCollector<SearchEvent>.search(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        provider.search(query, size)
    }

    private suspend fun FlowCollector<SearchEvent>.enhancedSearch(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        TODO()
    }

    private suspend fun FlowCollector<SearchEvent>.agenticSearch(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        TODO()
    }

}

enum class SearchMode {
    NORMAL,
    ENHANCED,
    AGENTIC
}