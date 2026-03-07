package work.slhaf.search.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import work.slhaf.search.WebContent

class SearchProviderPagingTest {

    private fun createProvider(): SearchProvider {
        return object : SearchProvider() {
            override fun search(query: String, pageSize: Int): List<WebContent> = emptyList()
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
        val provider = createProvider()
        val data = seedData(10)
        provider.putCache("q1", data)

        val result = provider.selectPage("q1", page = 1, pageSize = 3)

        assertEquals(listOf("t1", "t2", "t3"), result.map { it.title })
    }

    @Test
    fun middlePageSlice() {
        val provider = createProvider()
        val data = seedData(10)
        provider.putCache("q2", data)

        val result = provider.selectPage("q2", page = 2, pageSize = 3)

        assertEquals(listOf("t4", "t5", "t6"), result.map { it.title })
    }

    @Test
    fun lastPageWithRemainder() {
        val provider = createProvider()
        val data = seedData(10)
        provider.putCache("q3", data)

        val result = provider.selectPage("q3", page = 4, pageSize = 3)

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
        val provider = createProvider()
        provider.putCache("q4", seedData(5))

        val invalidPage = provider.selectPage("q4", page = 0, pageSize = 3)
        val invalidPageSize = provider.selectPage("q4", page = 1, pageSize = 0)

        assertTrue(invalidPage.isEmpty())
        assertTrue(invalidPageSize.isEmpty())
    }

    @Test
    fun returnsEmptyWhenPageOutOfRange() {
        val provider = createProvider()
        provider.putCache("q5", seedData(5))

        val result = provider.selectPage("q5", page = 10, pageSize = 2)

        assertTrue(result.isEmpty())
    }
}
