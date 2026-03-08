package work.slhaf.search.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import work.slhaf.search.Source
import work.slhaf.search.WebContent

class SearchProviderPagingTest {

    private fun createProvider(dataByQuery: Map<String, List<WebContent>> = emptyMap()): SearchProvider {
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

    @Test
    fun firstPageSlice() {
        val data = seedData(10)
        val provider = createProvider(mapOf("q1" to data))
        val source = Source("q1")
        provider.search(source, pageSize = 3)

        val result = provider.selectPage(source.id, page = 1, pageSize = 3)

        assertEquals(listOf("t1", "t2", "t3"), result.map { it.title })
    }

    @Test
    fun middlePageSlice() {
        val data = seedData(10)
        val provider = createProvider(mapOf("q2" to data))
        val source = Source("q2")
        provider.search(source, pageSize = 3)

        val result = provider.selectPage(source.id, page = 2, pageSize = 3)

        assertEquals(listOf("t4", "t5", "t6"), result.map { it.title })
    }

    @Test
    fun lastPageWithRemainder() {
        val data = seedData(10)
        val provider = createProvider(mapOf("q3" to data))
        val source = Source("q3")
        provider.search(source, pageSize = 3)

        val result = provider.selectPage(source.id, page = 4, pageSize = 3)

        assertEquals(listOf("t10"), result.map { it.title })
    }

    @Test
    fun returnsEmptyWhenQueryMissing() {
        val provider = createProvider()

        val result = provider.selectPage("missing", page = 1, pageSize = 3)

        assertTrue(result.isEmpty())
    }

    @Test
    fun returnsEmptyWhenPageOrPageSizeInvalid() {
        val data = seedData(5)
        val provider = createProvider(mapOf("q4" to data))
        val source = Source("q4")
        provider.search(source, pageSize = 3)

        val invalidPage = provider.selectPage(source.id, page = 0, pageSize = 3)
        val invalidPageSize = provider.selectPage(source.id, page = 1, pageSize = 0)

        assertTrue(invalidPage.isEmpty())
        assertTrue(invalidPageSize.isEmpty())
    }

    @Test
    fun returnsEmptyWhenPageOutOfRange() {
        val data = seedData(5)
        val provider = createProvider(mapOf("q5" to data))
        val source = Source("q5")
        provider.search(source, pageSize = 2)

        val result = provider.selectPage(source.id, page = 10, pageSize = 2)

        assertTrue(result.isEmpty())
    }
}
