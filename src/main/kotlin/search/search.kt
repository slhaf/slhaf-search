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
    ): Flow<SearchEvent<Any>> = flow {
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

    private suspend fun FlowCollector<SearchEvent<Any>>.search(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        provider.search(query, size)
    }

    private suspend fun FlowCollector<SearchEvent<Any>>.enhancedSearch(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        TODO()
    }

    private suspend fun FlowCollector<SearchEvent<Any>>.agenticSearch(provider: SearchProvider, query: String, size: Int) {
        emit(SearchEvent.stage())
        TODO()
    }

}
