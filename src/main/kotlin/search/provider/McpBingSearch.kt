package work.slhaf.search.provider

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.mcp.McpToolExecutor
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.slhaf.search.WebContent
import java.util.concurrent.atomic.AtomicBoolean

object McpBingSearch : SearchProvider() {
    private val json = Json { ignoreUnknownKeys = true }
    private val executor: McpToolExecutor
    private val closeClient: () -> Unit
    private val closed = AtomicBoolean(false)

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        val transport: McpTransport = StdioMcpTransport.builder()
            .command(listOf("npx", "bing-cn-mcp-enhanced"))
            .build()
        val client: McpClient = DefaultMcpClient.builder()
            .key("bing-cn")
            .transport(transport)
            .build()
        executor = McpToolExecutor(client)
        closeClient = { client.close() }

        log.info("bing-mcp loaded")
    }

    override fun search(query: String, pageSize: Int): List<WebContent> {
        if (closed.get()) {
            log.warn("bing-mcp client already closed, skip search")
            return emptyList()
        }
        log.debug("Searching $query")
        if (query.isBlank() || pageSize <= 0) return emptyList()
        val arguments = buildJsonObject {
            put("query", query)
            put("num_results", pageSize)
        }

        val request = ToolExecutionRequest.builder()
            .name("bing_search")
            .arguments(json.encodeToString(arguments))
            .build()

        val raw = executor.execute(request, "default")
        val items = json.decodeFromString(ListSerializer(McpBingSearchResultItem.serializer()), raw)
        log.debug("found ${items.size} items")
        return items.map {
            WebContent(
                title = it.title,
                abstract = it.snippet,
                preview = it.snippet,
                link = it.link
            )
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching(closeClient)
            .onSuccess { log.info("bing-mcp closed") }
            .onFailure { log.warn("failed to close bing-mcp client", it) }
    }
}

@Serializable
data class McpBingSearchResultItem(
    val id: String,
    val title: String,
    val link: String,
    val snippet: String,
    val timestamp: Long
)
