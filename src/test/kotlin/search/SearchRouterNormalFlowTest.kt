package work.slhaf.search

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import work.slhaf.search.provider.SearchProvider

class SearchRouterNormalFlowTest {

    @Test
    fun successEmitsStageResultDone() = runBlocking {
        val provider = object : SearchProvider() {
            override fun search(query: String, pageSize: Int): List<WebContent> = listOf(
                WebContent(
                    title = "t1",
                    abstract = "a1",
                    preview = "p1",
                    link = "https://example.com/1"
                )
            )
        }

        val events = flow {
            emitNormalSearchEvents(provider, "kotlin", 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
            events.map { it.event }
        )
    }

    @Test
    fun emptyResultStillEmitsResultAndDone() = runBlocking {
        val provider = object : SearchProvider() {
            override fun search(query: String, pageSize: Int): List<WebContent> = emptyList()
        }

        val events = flow {
            emitNormalSearchEvents(provider, "empty", 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
            events.map { it.event }
        )
        val searchPayload = events[1].data as SearchResult.Search
        assertEquals(0, searchPayload.totalCount)
    }

    @Test
    fun exceptionEmitsStageAndErrorOnly() = runBlocking {
        val provider = object : SearchProvider() {
            override fun search(query: String, pageSize: Int): List<WebContent> {
                throw IllegalStateException("boom")
            }
        }

        val events = flow {
            emitNormalSearchEvents(provider, "failure", 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.ERROR),
            events.map { it.event }
        )
        val errorPayload = events[1].data as SearchError
        assertEquals(listOf("boom"), errorPayload.errors)
    }

    @Test
    fun doneContentIdStableForSameQuery() = runBlocking {
        val provider = object : SearchProvider() {
            override fun search(query: String, pageSize: Int): List<WebContent> = emptyList()
        }

        val first = flow { emitNormalSearchEvents(provider, "stable-query", 10) }.toList()
        val second = flow { emitNormalSearchEvents(provider, "stable-query", 10) }.toList()

        val firstDone = first.last().data as SearchDone
        val secondDone = second.last().data as SearchDone
        assertEquals(firstDone.contentId, secondDone.contentId)
        assertTrue(firstDone.contentId.isNotBlank())
    }
}
