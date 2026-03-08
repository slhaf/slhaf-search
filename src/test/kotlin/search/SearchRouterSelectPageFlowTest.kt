package work.slhaf.search

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import work.slhaf.search.provider.SearchProvider

class SearchRouterSelectPageFlowTest {

    private fun createProvider(dataByQuery: Map<String, List<WebContent>>): SearchProvider {
        return object : SearchProvider() {
            override fun doSearch(query: String, pageSize: Int): List<WebContent> =
                dataByQuery[query] ?: emptyList()
        }
    }

    private fun seedData(size: Int): List<WebContent> =
        (1..size).map {
            WebContent(
                title = "t$it",
                abstract = "a$it",
                preview = "p$it",
                link = "https://example.com/$it"
            )
        }

    private suspend fun withProvidersForTest(
        providers: Map<String, SearchProvider>,
        block: suspend () -> Unit
    ) {
        val backup = SearchRouter.providers
        SearchRouter.providers = providers
        try {
            block()
        } finally {
            SearchRouter.providers = backup
        }
    }

    @Test
    fun cacheHitEmitsStageResultDoneWithPagedResults() = runBlocking {
        val provider = createProvider(mapOf("q1" to seedData(10)))
        val source = Source("q1")
        provider.search(source, pageSize = 10)

        withProvidersForTest(mapOf("test" to provider)) {
            val events = SearchRouter.selectPage(
                mode = SearchMode.NORMAL,
                id = source.id,
                page = 2,
                pageSize = 3
            ).toList()

            assertEquals(
                listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
                events.map { it.event }
            )

            val result = events[1].data as SearchResult.Search
            assertEquals(10, result.totalCount)
            assertEquals(4, result.pageCount)
            assertEquals(listOf("t4", "t5", "t6"), result.results.map { it.title })

            val done = events[2].data as SearchDone
            assertEquals(source.id, done.contentId)
        }
    }

    @Test
    fun cacheMissEmitsStageAndError() = runBlocking {
        val provider = createProvider(emptyMap())

        withProvidersForTest(mapOf("test" to provider)) {
            val events = SearchRouter.selectPage(
                mode = SearchMode.NORMAL,
                id = "missing-id",
                page = 1,
                pageSize = 3
            ).toList()

            assertEquals(
                listOf(SearchEvent.Event.STAGE, SearchEvent.Event.ERROR),
                events.map { it.event }
            )
            val error = events[1].data as SearchError
            assertTrue(error.errors.first().contains("cache miss"))
        }
    }

    @Test
    fun invalidPageAndPageSizeFollowProviderPagingSemantics() = runBlocking {
        val provider = createProvider(mapOf("q2" to seedData(5)))
        val source = Source("q2")
        provider.search(source, pageSize = 5)

        withProvidersForTest(mapOf("test" to provider)) {
            val invalidPageEvents = SearchRouter.selectPage(
                mode = SearchMode.NORMAL,
                id = source.id,
                page = 0,
                pageSize = 3
            ).toList()

            val invalidPageResult = invalidPageEvents[1].data as SearchResult.Search
            assertTrue(invalidPageResult.results.isEmpty())
            assertEquals(5, invalidPageResult.totalCount)
            assertEquals(2, invalidPageResult.pageCount)

            val invalidPageSizeEvents = SearchRouter.selectPage(
                mode = SearchMode.NORMAL,
                id = source.id,
                page = 1,
                pageSize = 0
            ).toList()

            val invalidPageSizeResult = invalidPageSizeEvents[1].data as SearchResult.Search
            assertTrue(invalidPageSizeResult.results.isEmpty())
            assertEquals(5, invalidPageSizeResult.totalCount)
            assertEquals(0, invalidPageSizeResult.pageCount)
        }
    }

    @Test
    fun outOfRangePageReturnsEmptyResultAndDone() = runBlocking {
        val provider = createProvider(mapOf("q3" to seedData(5)))
        val source = Source("q3")
        provider.search(source, pageSize = 5)

        withProvidersForTest(mapOf("test" to provider)) {
            val events = SearchRouter.selectPage(
                mode = SearchMode.NORMAL,
                id = source.id,
                page = 10,
                pageSize = 2
            ).toList()

            assertEquals(
                listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
                events.map { it.event }
            )

            val result = events[1].data as SearchResult.Search
            assertEquals(5, result.totalCount)
            assertEquals(3, result.pageCount)
            assertTrue(result.results.isEmpty())
        }
    }

    @Test
    fun enhancedModeRemainsTodo() = runBlocking {
        val provider = createProvider(emptyMap())

        withProvidersForTest(mapOf("test" to provider)) {
            val error = runCatching {
                SearchRouter.selectPage(
                    mode = SearchMode.ENHANCED,
                    id = "id",
                    page = 1,
                    pageSize = 10
                ).toList()
            }.exceptionOrNull()

            assertIs<NotImplementedError>(error)
        }
    }

    @Test
    fun agenticModeRemainsTodo() = runBlocking {
        val provider = createProvider(emptyMap())

        withProvidersForTest(mapOf("test" to provider)) {
            val error = runCatching {
                SearchRouter.selectPage(
                    mode = SearchMode.AGENTIC,
                    id = "id",
                    page = 1,
                    pageSize = 10
                ).toList()
            }.exceptionOrNull()

            assertIs<NotImplementedError>(error)
        }
    }
}
