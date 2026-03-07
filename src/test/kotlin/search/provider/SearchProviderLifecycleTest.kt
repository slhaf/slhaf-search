package work.slhaf.search.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import work.slhaf.search.WebContent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SearchProviderLifecycleTest {

    @Test
    fun defaultCloseNoop() {
        val provider = object : SearchProvider {
            override fun search(query: String, pageSize: Int): List<WebContent> = emptyList()
        }

        provider.close()
    }

    @Test
    fun idempotentClosePattern() {
        val closeCalls = AtomicInteger(0)
        val closed = AtomicBoolean(false)
        val provider = object : SearchProvider {
            override fun search(query: String, pageSize: Int): List<WebContent> = emptyList()

            override fun close() {
                if (!closed.compareAndSet(false, true)) return
                closeCalls.incrementAndGet()
            }
        }

        provider.close()
        provider.close()
        provider.close()

        assertEquals(1, closeCalls.get())
    }
}
