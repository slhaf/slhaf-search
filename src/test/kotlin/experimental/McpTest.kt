package work.slhaf.experimental

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.mcp.McpToolExecutor
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import dev.langchain4j.service.tool.ToolExecutor
import kotlin.test.Test

class McpTest {
    @Test
    fun bingMcp() {
        val transport: McpTransport = StdioMcpTransport.builder()
            .command(listOf("npx", "bing-cn-mcp-enhanced"))
            .logEvents(true)
            .build()

        val client: McpClient = DefaultMcpClient.builder()
            .key("bing-cn")
            .transport(transport)
            .build()

        val tools: List<ToolSpecification> = client.listTools()

        println("=== MCP tools ===")
        tools.forEach { tool ->
            println("${tool.name()} -> ${tool.description()}")
        }

        val executor: ToolExecutor = McpToolExecutor(client)

        // 1) 搜索
        val searchRequest = ToolExecutionRequest.builder()
            .name("bing_search")
            .arguments(
                """
                    {
                      "query": "LangChain4j MCP 教程",
                      "num_results": 5
                    }
                    """.trimIndent()
            )
            .build()

        val searchResult = executor.execute(searchRequest, "default")
        println("=== search result ===")
        println(searchResult)

        val fetchRequest = ToolExecutionRequest.builder()
            .name("mcp__fetch_webpage")
            .arguments(
                """
                    {
                      "result_id": 1
                    }
                    """.trimIndent()
            )
            .build()

        val pageResult = executor.execute(fetchRequest, "default")
        println("=== page result ===")
        println(pageResult)

    }
}