package work.slhaf.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import work.slhaf.search.provider.McpBingSearch
import work.slhaf.search.provider.SearchProvider

object SearchRouter {

    private val providers = mapOf<String, SearchProvider>(
        "bing" to McpBingSearch
    )

    fun search(
        mode: SearchMode = SearchMode.NORMAL,
        provider: String = "default",
        query: String,
        pageSize: Int = 10
    ): Flow<SearchEvent> = flow {
        val searchProvider = if (provider == "default") {
            providers.values.first()
        } else {
            providers[provider] ?: providers.values.first()
        }
        val source = Source(query = query)

        when (mode) {
            SearchMode.NORMAL -> search(searchProvider, source, pageSize)
            SearchMode.ENHANCED -> enhancedSearch(searchProvider, source, pageSize)
            SearchMode.AGENTIC -> agenticSearch(searchProvider, source, pageSize)
        }
    }

    private suspend fun FlowCollector<SearchEvent>.search(
        provider: SearchProvider,
        source: Source,
        pageSize: Int
    ) {
        emitNormalSearchEvents(provider, source, pageSize)
    }

    private suspend fun FlowCollector<SearchEvent>.enhancedSearch(
        provider: SearchProvider,
        source: Source,
        pageSize: Int
    ) {
        emit(SearchEvent.stage(mode = SearchMode.ENHANCED, content = "Enhanced Searching...", source = source))
        TODO()
    }

    private suspend fun FlowCollector<SearchEvent>.agenticSearch(
        provider: SearchProvider,
        source: Source,
        pageSize: Int
    ) {
        emit(SearchEvent.stage(mode = SearchMode.AGENTIC, content = "Agentic Searching...", source = source))
        TODO()
    }

    fun closeAllProviders() {
        providers.values.toSet().forEach { provider ->
            runCatching { provider.close() }
        }
    }
}

internal suspend fun FlowCollector<SearchEvent>.emitNormalSearchEvents(
    provider: SearchProvider,
    source: Source,
    pageSize: Int
) {
    emit(SearchEvent.stage(mode = SearchMode.NORMAL, content = "Searching...", source = source))
    try {
        val webContents = provider.search(source, pageSize)
        emit(SearchEvent.result(source = source, webContents = webContents, page = 1, pageSize = pageSize))
        emit(SearchEvent.done(mode = SearchMode.NORMAL, source = source, contentId = source.id))
    } catch (e: Exception) {
        val msg = e.message?.takeIf { it.isNotBlank() }
            ?: e::class.simpleName
            ?: "unknown error"
        emit(SearchEvent.error(mode = SearchMode.NORMAL, source = source, errors = listOf(msg)))
    }
}
