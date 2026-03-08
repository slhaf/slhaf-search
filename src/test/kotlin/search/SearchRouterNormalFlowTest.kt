package work.slhaf.search

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import work.slhaf.search.provider.SearchProvider

class SearchRouterNormalFlowTest {

    @Test
    fun successEmitsStageResultDone() = runBlocking {
        val provider = object : SearchProvider() {
            override fun doSearch(query: String, pageSize: Int): List<WebContent> = listOf(
                WebContent(
                    title = "t1",
                    abstract = "a1",
                    preview = "p1",
                    link = "https://example.com/1"
                )
            )
        }

        val events = flow {
            emitNormalSearchEvents(provider, Source("kotlin"), 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
            events.map { it.event }
        )
        val ids = events.map { it.data.source.id }.toSet()
        assertEquals(1, ids.size)
        val donePayload = events.last().data as SearchDone
        assertEquals(events.first().data.source.id, donePayload.contentId)
    }

    @Test
    fun emptyResultStillEmitsResultAndDone() = runBlocking {
        val provider = object : SearchProvider() {
            override fun doSearch(query: String, pageSize: Int): List<WebContent> = emptyList()
        }

        val events = flow {
            emitNormalSearchEvents(provider, Source("empty"), 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.RESULT, SearchEvent.Event.DONE),
            events.map { it.event }
        )
        val ids = events.map { it.data.source.id }.toSet()
        assertEquals(1, ids.size)
        val searchPayload = events[1].data as SearchResult.Search
        assertEquals(0, searchPayload.totalCount)
    }

    @Test
    fun exceptionEmitsStageAndErrorOnly() = runBlocking {
        val provider = object : SearchProvider() {
            override fun doSearch(query: String, pageSize: Int): List<WebContent> {
                throw IllegalStateException("boom")
            }
        }

        val events = flow {
            emitNormalSearchEvents(provider, Source("failure"), 10)
        }.toList()

        assertEquals(
            listOf(SearchEvent.Event.STAGE, SearchEvent.Event.ERROR),
            events.map { it.event }
        )
        val ids = events.map { it.data.source.id }.toSet()
        assertEquals(1, ids.size)
        val errorPayload = events[1].data as SearchError
        assertEquals(listOf("boom"), errorPayload.errors)
    }

    @Test
    fun doneContentIdMatchesSourceAndDiffersAcrossRequests() = runBlocking {
        val provider = object : SearchProvider() {
            override fun doSearch(query: String, pageSize: Int): List<WebContent> = emptyList()
        }

        val first = flow {
            emitNormalSearchEvents(provider, Source("stable-query"), 10)
        }.toList()
        val second = flow {
            emitNormalSearchEvents(provider, Source("stable-query"), 10)
        }.toList()

        val firstDone = first.last().data as SearchDone
        val secondDone = second.last().data as SearchDone
        assertEquals(first.first().data.source.id, firstDone.contentId)
        assertEquals(second.first().data.source.id, secondDone.contentId)
        assertNotEquals(firstDone.contentId, secondDone.contentId)
        assertTrue(firstDone.contentId.isNotBlank())
        assertTrue(secondDone.contentId.isNotBlank())
    }
}
