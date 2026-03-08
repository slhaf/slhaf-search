package work.slhaf.search.provider

import kotlin.test.Test
import work.slhaf.search.Source

class McpBingSearchTest {
    @Test
    fun test() {
        var result = McpBingSearch.search(Source("hyprgrass"), 30)
        val before = System.currentTimeMillis()
        result = McpBingSearch.search(Source("hyprland"), 30)
        val after = System.currentTimeMillis()
        println("time: " + (after - before) + "ms")
        var count = 0
        result.forEach {
            count++
            println("$count: ${it.title}")
        }

        McpBingSearch.close()
        McpBingSearch.close()
    }
}
