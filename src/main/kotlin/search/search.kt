package work.slhaf.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import work.slhaf.search.provider.McpBingSearch
import work.slhaf.search.provider.SearchProvider

object SearchRouter {

    private val defaultProviderKey = "default"

    internal var providers = mapOf<String, SearchProvider>(
        "bing" to McpBingSearch
    )

    private fun resolveProvider(provider: String = defaultProviderKey): SearchProvider {
        return if (provider == defaultProviderKey) {
            providers.values.first()
        } else {
            providers[provider] ?: providers.values.first()
        }
    }

    fun search(
        mode: SearchMode = SearchMode.NORMAL,
        provider: String = defaultProviderKey,
        query: String,
        pageSize: Int = 10
    ): Flow<SearchEvent> = flow {
        val searchProvider = resolveProvider(provider)
        val source = Source(query = query)

        suspend fun FlowCollector<SearchEvent>.search(
            provider: SearchProvider,
            source: Source,
            pageSize: Int
        ) {
            emitNormalSearchEvents(provider, source, pageSize)
        }

        suspend fun FlowCollector<SearchEvent>.enhancedSearch(
            provider: SearchProvider,
            source: Source,
            pageSize: Int
        ) {
            emit(SearchEvent.stage(mode = SearchMode.ENHANCED, content = "Enhanced Searching...", source = source))
            TODO()
        }

        suspend fun FlowCollector<SearchEvent>.agenticSearch(
            provider: SearchProvider,
            source: Source,
            pageSize: Int
        ) {
            emit(SearchEvent.stage(mode = SearchMode.AGENTIC, content = "Agentic Searching...", source = source))
            TODO()
        }

        when (mode) {
            SearchMode.NORMAL -> search(searchProvider, source, pageSize)
            SearchMode.ENHANCED -> enhancedSearch(searchProvider, source, pageSize)
            SearchMode.AGENTIC -> agenticSearch(searchProvider, source, pageSize)
        }
    }

    fun selectPage(
        mode: SearchMode,
        id: String,
        page: Int, pageSize: Int,
    ): Flow<SearchEvent> = flow {
        val source = Source(query = id, id = id)
        when (mode) {
            SearchMode.NORMAL -> {
                val provider = resolveProvider()
                emit(SearchEvent.stage(mode = SearchMode.NORMAL, content = "Selecting page...", source = source))
                val cached = provider.cache[id]
                if (cached == null) {
                    emit(
                        SearchEvent.error(
                            mode = SearchMode.NORMAL,
                            source = source,
                            errors = listOf("cache miss for source id: $id")
                        )
                    )
                    return@flow
                }
                val pageCount = if (cached.isEmpty() || pageSize <= 0) {
                    0
                } else {
                    (cached.size + pageSize - 1) / pageSize
                }
                val webContents = provider.selectPage(id, page, pageSize)
                emit(
                    SearchEvent.result(
                        source = source,
                        totalCount = cached.size,
                        pageCount = pageCount,
                        results = webContents
                    )
                )
                emit(SearchEvent.done(mode = SearchMode.NORMAL, source = source, contentId = id))
            }

            SearchMode.ENHANCED -> {
                emit(SearchEvent.stage(mode = SearchMode.ENHANCED, content = "Enhanced Selecting Page...", source = source))
                TODO()
            }

            SearchMode.AGENTIC -> {
                emit(SearchEvent.stage(mode = SearchMode.AGENTIC, content = "Agentic Selecting Page...", source = source))
                TODO()
            }
        }
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
