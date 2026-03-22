package work.slhaf.experimental

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.mcp.McpToolExecutor
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import dev.langchain4j.service.tool.ToolExecutor
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomExtractTest {

    @Test
    fun domToMarkdown_extractsMainReadableContent() {
        val domContent = """
            <html>
              <head>
                <style>.hidden {display:none}</style>
                <script>console.log("tracking")</script>
              </head>
              <body>
                <nav>Home Docs Login</nav>
                <article>
                  <h1>Kotlin DOM 提取实验</h1>
                  <p>这是正文第一段，主要描述目标。</p>
                  <p>正文第二段包含 <a href="https://example.com/spec">规格文档</a> 链接。</p>
                  <ul>
                    <li>支持标题和段落</li>
                    <li>支持列表与引用</li>
                  </ul>
                  <blockquote>可读性优先。</blockquote>
                  <pre><code>println("hello")</code></pre>
                </article>
                <footer>© 2026 Example Inc.</footer>
              </body>
            </html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(domContent)

        assertTrue(markdown.contains("# Kotlin DOM 提取实验"))
        assertTrue(markdown.contains("这是正文第一段，主要描述目标。"))
        assertTrue(markdown.contains("[规格文档](https://example.com/spec)"))
        assertTrue(markdown.contains("- 支持标题和段落"))
        assertTrue(markdown.contains("> 可读性优先。"))
        assertTrue(markdown.contains("```"))
        assertTrue(markdown.contains("println(\"hello\")"))
    }

    @Test
    fun domToMarkdown_ignoresNonReadableNodes() {
        val domContent = """
            <html>
              <body>
                <header>Sign in</header>
                <nav>导航 菜单 主页</nav>
                <div class="content">
                  <h2>可读内容</h2>
                  <p>真正需要提取的文本。</p>
                </div>
                <script>alert("xss")</script>
                <style>.x { color:red }</style>
                <footer>cookie policy</footer>
              </body>
            </html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(domContent)

        assertTrue(markdown.contains("## 可读内容"))
        assertTrue(markdown.contains("真正需要提取的文本。"))
        assertFalse(markdown.contains("alert(\"xss\")"))
        assertFalse(markdown.contains("cookie policy"))
        assertFalse(markdown.contains("导航 菜单 主页"))
    }

    @Test
    fun domToMarkdown_handlesEmptyOrNoiseOnlyDom() {
        assertEquals("", extractReadableMarkdown(""))
        val noiseOnly = """
            <html>
              <body>
                <nav>home login sign in</nav>
                <footer>privacy cookie terms</footer>
                <script>console.log("noise")</script>
              </body>
            </html>
        """.trimIndent()
        assertEquals("", extractReadableMarkdown(noiseOnly))
    }

    @Test
    fun domToMarkdown_preservesStructureForMixedLayout() {
        val dom = """
            <html><body>
              <main>
                <h1>Guide</h1>
                <p>Intro paragraph.</p>
                <ul>
                  <li>Item A</li>
                  <li>Item B</li>
                </ul>
                <blockquote>Important note.</blockquote>
                <pre><code>echo "ok"</code></pre>
              </main>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("# Guide"))
        assertTrue(markdown.contains("Intro paragraph."))
        assertTrue(markdown.contains("- Item A"))
        assertTrue(markdown.contains("> Important note."))
        assertTrue(markdown.contains("```"))
    }

    @Test
    fun dedup_skipsNestedHeadingAndListDuplicates() {
        val dom = """
            <html><body>
              <ul>
                <li><a href="/c/1">Fix parser bug</a> by Alice</li>
              </ul>
              <h4><a href="/c/1">Fix parser bug</a></h4>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("- [Fix parser bug](/c/1) by Alice"))
        assertFalse(markdown.contains("#### [Fix parser bug](/c/1)"))
    }

    @Test
    fun dedup_suppresses_inlineCodeUnderHeadingAndList() {
        val dom = """
            <html><body>
              <h3>Use <code>ParserConfig</code> for setup</h3>
              <ul><li>Enable <code>strictMode</code></li></ul>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("### Use ParserConfig for setup"))
        assertTrue(markdown.contains("- Enable strictMode"))
        assertFalse(markdown.lines().any { it.trim() == "`ParserConfig`" })
        assertFalse(markdown.lines().any { it.trim() == "`strictMode`" })
    }

    @Test
    fun dedup_keeps_distinctItemsWithSharedPrefix() {
        val dom = """
            <html><body>
              <ul>
                <li><a href="/c/1">refactor(core): improve parser speed</a></li>
                <li><a href="/c/2">refactor(core): improve parser stability</a></li>
              </ul>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("improve parser speed"))
        assertTrue(markdown.contains("improve parser stability"))
    }

    @Test
    fun dedup_skipsAnchorWrappedLiNestedChildren() {
        val dom = """
            <html><body>
              <a target="_blank" href="/servers/regenrek/deepwiki-mcp">
                <li>
                  <h3>Deepwiki</h3>
                  <p>从 deepwiki.com 获取内容并将其转换为 LLM 可读的 markdown。</p>
                </li>
              </a>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        val deepwikiLines = markdown.lines().filter { it.contains("Deepwiki") }
        assertTrue(deepwikiLines.size == 1)
        assertFalse(markdown.contains("### Deepwiki"))
    }

    @Test
    fun dedup_skipsParagraphInsideAcceptedListItem() {
        val dom = """
            <html><body>
              <ul>
                <li>
                  <p>Primary item paragraph</p>
                  <div>extra detail inside same item</div>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.lines().count { it.contains("Primary item paragraph") } == 1)
        assertFalse(markdown.contains("extra detail inside same item"))
    }

    @Test
    fun dedup_keepsNestedSubListItems() {
        val dom = """
            <html><body>
              <ul>
                <li>Parent item
                  <ul>
                    <li>Child item A</li>
                    <li>Child item B</li>
                  </ul>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("- Parent item"))
        assertTrue(markdown.contains("  - Child item A"))
        assertTrue(markdown.contains("  - Child item B"))
    }

    @Test
    fun domToMarkdown_keepsHeadingInsideHeader() {
        val dom = """
            <html><body>
              <header>
                <h1>Commits</h1>
              </header>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("# Commits"))
    }

    @Test
    fun domToMarkdown_keepsBannerMainHeading() {
        val dom = """
            <html><body>
              <div role="banner">
                <h1>Release Dashboard</h1>
              </div>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("# Release Dashboard"))
    }

    @Test
    fun domToMarkdown_noiseFilterDoesNotDropLongParagraph() {
        val dom = """
            <html><body>
              <p>This release notes section mentions privacy and terms in context, but it is still a valid long paragraph with meaningful description for users.</p>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("release notes section mentions privacy and terms"))
    }

    @Test
    fun domToMarkdown_formatsPreAndTableBlocks() {
        val dom = """
            <html><body>
              <pre><code>line1
line2
line3</code></pre>
              <table>
                <tr><th>Name</th><th>Lang</th></tr>
                <tr><td>Partner</td><td>Kotlin</td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val markdown = extractReadableMarkdown(dom)
        assertTrue(markdown.contains("```\nline1\nline2\nline3\n```"))
        assertTrue(markdown.contains("| Name | Lang |"))
        assertTrue(markdown.contains("| --- | --- |"))
        assertTrue(markdown.contains("| Partner | Kotlin |"))
    }

    @Test
    fun domToMarkdown_cleansEscapedNewlineLiterals() {
        val dom = """
            <html><body>
              <p>foo\nbar</p>
            </body></html>
        """.trimIndent()
        val markdown = extractReadableMarkdown(dom)
        assertFalse(markdown.contains("\\n"))
        assertTrue(markdown.contains("foo bar"))
    }

    @Test
    fun usableTest() {
        val transport: McpTransport = StdioMcpTransport.builder()
            .command(listOf("npx", "-y", "@playwright/mcp@latest", "--headless"))
            .logEvents(true)
            .build()

        val client: McpClient = DefaultMcpClient.builder()
            .key("playwright")
            .transport(transport)
            .build()

        try {
            val tools: List<ToolSpecification> = client.listTools()
            println("=== Playwright MCP tools ===")
            tools.forEach { tool -> println("${tool.name()} -> ${tool.description()}") }

            val executor: ToolExecutor = McpToolExecutor(client)
            val navigateRequest = ToolExecutionRequest.builder()
                .name("browser_navigate")
                .arguments(
                    """
                    {
                      "url": "https://www.ibm.com/docs/zh/rpa/23.0.x?topic=text-html-markdown"
                    }
                    """.trimIndent()
                )
                .build()
            executor.execute(navigateRequest, "default")

            val waitRequest = ToolExecutionRequest.builder()
                .name("browser_wait_for")
                .arguments(
                    """
                    {
                      "time": 3
                    }
                    """.trimIndent()
                )
                .build()
            executor.execute(waitRequest, "default")

            val evaluateRequest = ToolExecutionRequest.builder()
                .name("browser_evaluate")
                .arguments(
                    """
                    {
                      "function": "() => document.documentElement.outerHTML"
                    }
                    """.trimIndent()
                )
                .build()

            val rawDom = executor.execute(evaluateRequest, "default")
            val stats = extractReadableMarkdownWithStats(rawDom)

            val outputDir = Path.of("build", "reports", "dom-extract")
            Files.createDirectories(outputDir)
            val rawDomFile = outputDir.resolve("raw_dom.html")
            val extractedFile = outputDir.resolve("extracted.md")
            val summaryFile = outputDir.resolve("summary.txt")

            Files.writeString(rawDomFile, rawDom, StandardCharsets.UTF_8)
            Files.writeString(extractedFile, stats.markdown, StandardCharsets.UTF_8)
            Files.writeString(summaryFile, buildSummary(stats), StandardCharsets.UTF_8)

            println("raw dom -> $rawDomFile")
            println("extracted markdown -> $extractedFile")
            println("summary -> $summaryFile")
            println("raw=${stats.rawLength}, extracted=${stats.markdownLength}, blocks=${stats.keptBlocks}/${stats.totalBlocks}")

            assertTrue(Files.exists(rawDomFile))
            assertTrue(Files.exists(extractedFile))
            assertTrue(Files.exists(summaryFile))
            assertTrue(Files.size(rawDomFile) > 0)
            assertTrue(Files.size(extractedFile) >= 0)
            val summaryText = Files.readString(summaryFile, StandardCharsets.UTF_8)
            assertTrue(summaryText.contains("markdownLength="))
            assertTrue(summaryText.contains("dedupSkippedByParent="))
            assertTrue(summaryText.contains("dedupSkippedBySignature="))
            assertTrue(summaryText.contains("codeSuppressed="))
            assertTrue(summaryText.contains("nestedSkipped="))
            assertTrue(summaryText.contains("anchorWrappedLiRecovered="))
        } finally {
            client.close()
        }
    }

    private fun extractReadableMarkdown(dom: String): String = extractReadableMarkdownWithStats(dom).markdown

    private fun extractReadableMarkdownWithStats(dom: String): ExtractStats {
        if (dom.isBlank()) {
            return ExtractStats(0, 0, 0, 0, 0, "", emptyList(), 0, 0, 0, 0, 0, 0, 0)
        }
        val document = Jsoup.parse(dom)
        val removedByStructuralSanitize = sanitizeDom(document)

        val trace = ExtractTrace()
        val markdownBlocks = extractByTextTags(document.body(), trace)
        val markdown = normalizeMarkdown(markdownBlocks.joinToString("\n\n"))

        return ExtractStats(
            rawLength = dom.length,
            markdownLength = markdown.length,
            totalBlocks = trace.totalBlocks,
            keptBlocks = trace.keptBlocks,
            filteredBlocks = trace.filteredBlocks,
            markdown = markdown,
            filteredSamples = trace.filteredSamples,
            dedupSkippedByParent = trace.dedupSkippedByParent,
            dedupSkippedBySignature = trace.dedupSkippedBySignature,
            codeSuppressed = trace.codeSuppressed,
            removedByStructuralSanitize = removedByStructuralSanitize,
            removedByNoiseFilter = trace.filteredBlocks,
            nestedSkipped = trace.nestedSkipped,
            anchorWrappedLiRecovered = trace.anchorWrappedLiRecovered
        )
    }

    private fun sanitizeDom(document: Document): Int {
        var removed = 0
        val unreadable = document.select("script,style,noscript,template,svg,canvas,iframe,meta,link")
        removed += unreadable.size
        unreadable.remove()

        val structural = document.select("nav,footer,aside,[role=navigation],[role=contentinfo]")
        removed += structural.size
        structural.remove()

        val hidden = document.select("[hidden],[aria-hidden=true],[style*=display:none],[style*=visibility:hidden]")
        removed += hidden.size
        hidden.remove()

        // keep headers by default; remove only clearly auxiliary sr-only fragments
        val srOnly = document.select("[class*=sr-only],[class*=visually-hidden]")
        srOnly.forEach { node ->
            val semanticChildren = node.select("h1,h2,h3,h4,h5,h6,p,li,blockquote,pre,code,table").size
            if (semanticChildren == 0 && node.text().trim().length <= 30) {
                node.remove()
                removed++
            }
        }
        removeComments(document)
        return removed
    }

    private fun removeComments(element: Element) {
        element.childNodes().filterIsInstance<Comment>().forEach { it.remove() }
        element.children().forEach { child -> removeComments(child) }
    }

    private fun extractByTextTags(root: Element, trace: ExtractTrace): List<String> {
        val blocks = mutableListOf<String>()
        val elements = root.select("h1,h2,h3,h4,h5,h6,p,li,blockquote,pre,code,table")
        val acceptedElements = mutableListOf<Element>()
        val seenSignatures = mutableSetOf<String>()

        for (element in elements) {
            trace.totalBlocks++

            if (shouldSkipAsNestedTextBlock(element, acceptedElements)) {
                trace.nestedSkipped++
                continue
            }

            val raw = when (element.tagName()) {
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = element.tagName().drop(1).toInt()
                    "${"#".repeat(level)} ${inlineText(element)}"
                }
                "p" -> inlineText(element)
                "li" -> {
                    val depth = element.parents().count { it.tagName() == "ul" || it.tagName() == "ol" }.coerceAtLeast(1) - 1
                    "${"  ".repeat(depth)}- ${listItemText(element, trace)}"
                }
                "blockquote" -> inlineText(element).lineSequence().joinToString("\n") { "> $it" }
                "pre" -> {
                    val code = extractPreformattedText(element)
                    if (code.isBlank()) "" else "```\n$code\n```"
                }
                "code" -> {
                    val inBlockedParent = element.parents().any { p ->
                        p.tagName().matches(Regex("h[1-6]")) || p.tagName() == "li" || p.tagName() == "a"
                    }
                    val parentAlreadyAccepted = element.parents().any { it in acceptedElements }
                    if (element.parent()?.tagName() == "pre" || (inBlockedParent && parentAlreadyAccepted)) {
                        trace.codeSuppressed++
                        ""
                    } else {
                        "`${element.text().trim()}`"
                    }
                }
                "table" -> tableToMarkdown(element)
                else -> ""
            }

            val normalized = normalizeBlock(raw, element.tagName())
            if (normalized.isBlank() || isNoise(normalized)) {
                if (normalized.isNotBlank()) {
                    trace.filteredBlocks++
                    if (trace.filteredSamples.size < 8) trace.filteredSamples += normalized
                }
                continue
            }

            if (isCoveredByAcceptedParentOrSameAnchor(element, acceptedElements)) {
                trace.dedupSkippedByParent++
                continue
            }

            val signature = textSignature(normalized)
            if (!seenSignatures.add(signature)) {
                trace.dedupSkippedBySignature++
                continue
            }

            trace.keptBlocks++
            blocks += normalized
            acceptedElements += element
        }
        return blocks
    }

    private fun shouldSkipAsNestedTextBlock(
        element: Element,
        acceptedElements: List<Element>
    ): Boolean {
        val tag = element.tagName()
        val nestedTextTags = setOf("a", "code", "span", "strong", "em", "small", "time", "p", "div", "h1", "h2", "h3", "h4", "h5", "h6")
        if (tag !in nestedTextTags) return false
        return acceptedElements.any { accepted ->
            accepted.tagName() in setOf("li", "p", "blockquote") && element.parents().contains(accepted)
        }
    }

    private fun inlineText(element: Element): String {
        val clone = element.clone()
        clone.select("a[href]").forEach { link ->
            val text = link.text().trim()
            val href = cleanHref(link.attr("href"))
            val replacement = if (text.isNotBlank() && href.isNotBlank()) "[$text]($href)" else text
            link.after(replacement)
            link.remove()
        }
        return clone.text()
    }

    private fun listItemText(element: Element, trace: ExtractTrace): String {
        val clone = element.clone()
        clone.select("ul,ol").remove()

        val primary = extractPrimaryListItemText(clone)
        val innerLink = clone.selectFirst("a[href]")
        if (innerLink == null) {
            val parent = element.parent()
            if (parent != null && parent.tagName() == "a" && parent.hasAttr("href")) {
                val href = cleanHref(parent.attr("href"))
                if (href.isNotBlank() && primary.isNotBlank()) {
                    trace.anchorWrappedLiRecovered++
                    return "[$primary]($href)"
                }
            }
        }
        return primary
    }

    private fun extractPrimaryListItemText(element: Element): String {
        val directAnchor = element.children().firstOrNull { it.tagName() == "a" && it.hasAttr("href") }
        if (directAnchor != null) {
            return inlineText(element)
        }

        val own = normalizeText(element.ownText())
        if (own.isNotBlank()) {
            val directCodeText = element.children()
                .filter { it.tagName() == "code" }
                .mapNotNull {
                    val codeText = normalizeText(it.text())
                    codeText.takeIf { text -> text.isNotBlank() }
                }
            if (directCodeText.isNotEmpty()) {
                return normalizeText((listOf(own) + directCodeText).joinToString(" "))
            }
            return own
        }

        val firstBlock = element.children().firstOrNull {
            it.tagName() in setOf("h1", "h2", "h3", "h4", "h5", "h6", "p", "div", "span", "strong", "em", "small", "time") ||
                (it.tagName() == "a" && it.hasAttr("href"))
        }
        if (firstBlock != null) {
            return inlineText(firstBlock)
        }

        return inlineText(element)
    }

    private fun tableToMarkdown(table: Element): String {
        val rows = table.select("tr")
            .map { row -> row.select("th,td").map { normalizeText(inlineText(it)) } }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return ""

        val width = rows.maxOf { it.size }
        val normalizedRows = rows.map { row ->
            if (row.size < width) row + List(width - row.size) { "" } else row
        }

        val header = normalizedRows.first()
        val divider = List(width) { "---" }
        val body = normalizedRows.drop(1)

        val lines = mutableListOf<String>()
        lines += "| ${header.joinToString(" | ")} |"
        lines += "| ${divider.joinToString(" | ")} |"
        lines += body.map { row -> "| ${row.joinToString(" | ")} |" }
        return lines.joinToString("\n")
    }

    private fun normalizeText(text: String): String {
        val cleaned = sanitizeEscapedLiterals(text, keepEscapedNewline = false)
        return cleaned.replace(Regex("[\\t\\r\\n ]+"), " ").trim()
    }

    private fun normalizeBlock(text: String, tag: String): String {
        if (tag == "pre") return text.trim()
        if (tag == "table") return text.trim()
        if (tag != "li") return normalizeText(text)
        val leadingSpaces = text.takeWhile { it == ' ' }
        val body = text.trimStart().removePrefix("-").trim()
        val normalizedBody = normalizeText(body)
        if (normalizedBody.isBlank()) return ""
        return "$leadingSpaces- $normalizedBody"
    }

    private fun extractPreformattedText(element: Element): String {
        val codeNode = element.selectFirst("code")
        var text = codeNode?.wholeText() ?: element.wholeText()
        text = sanitizeEscapedLiterals(text, keepEscapedNewline = true)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim('\n', ' ', '\t')
        return text
    }

    private fun sanitizeEscapedLiterals(text: String, keepEscapedNewline: Boolean): String {
        var normalized = text
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\t", " ")
            .replace("\\r", " ")
        normalized = if (keepEscapedNewline) normalized.replace("\\n", "\n") else normalized.replace("\\n", " ")
        return normalized
    }

    private fun cleanHref(rawHref: String): String {
        return sanitizeEscapedLiterals(rawHref.trim(), keepEscapedNewline = false)
            .trim('"')
    }

    private fun normalizeMarkdown(markdown: String): String {
        return markdown.replace(Regex("\\n{3,}"), "\n\n").trim()
    }

    private fun isNoise(text: String): Boolean {
        val lowered = text.lowercase()
        val noiseKeywords = listOf(
            "sign in", "log in", "login", "register", "privacy", "cookie", "terms",
            "navigation menu", "unable to load page", "system error", "github status", "try reloading the page",
            "导航", "菜单", "主页"
        )
        if (text.length < 3) return true
        if (text.startsWith("©")) return true
        return text.length <= 80 && noiseKeywords.any { lowered.contains(it) }
    }

    private fun isCoveredByAcceptedParentOrSameAnchor(
        element: Element,
        acceptedElements: List<Element>
    ): Boolean {
        val currentAnchor = mainAnchorHref(element)
        for (accepted in acceptedElements) {
            val acceptedTag = accepted.tagName()
            val currentTag = element.tagName()
            val descendantCovered =
                element.parents().contains(accepted) &&
                    acceptedTag in setOf("li", "p", "blockquote") &&
                    currentTag in setOf("a", "code", "span", "strong", "em", "small", "time", "p", "div", "h1", "h2", "h3", "h4", "h5", "h6")
            if (descendantCovered) return true

            val acceptedAnchor = mainAnchorHref(accepted)
            if (!currentAnchor.isNullOrBlank() && currentAnchor == acceptedAnchor) {
                return true
            }
        }
        return false
    }

    private fun mainAnchorHref(element: Element): String? {
        return when (element.tagName()) {
            "a" -> element.attr("href").takeIf { it.isNotBlank() }
            else -> element.selectFirst("a[href]")?.attr("href")?.takeIf { it.isNotBlank() }
        }
    }

    private fun textSignature(text: String): String {
        val stripped = stripMarkdownSyntax(text).lowercase()
        return normalizeText(stripped)
    }

    private fun stripMarkdownSyntax(text: String): String {
        return text
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1")
            .replace(Regex("^\\s{0,6}#{1,6}\\s+"), "")
            .replace(Regex("^\\s*[-*+]\\s+"), "")
    }

    private fun buildSummary(stats: ExtractStats): String {
        val ratio = if (stats.totalBlocks == 0) 0.0 else stats.filteredBlocks.toDouble() / stats.totalBlocks.toDouble()
        return buildString {
            appendLine("timestamp=${Instant.now()}")
            appendLine("rawLength=${stats.rawLength}")
            appendLine("markdownLength=${stats.markdownLength}")
            appendLine("totalBlocks=${stats.totalBlocks}")
            appendLine("keptBlocks=${stats.keptBlocks}")
            appendLine("filteredBlocks=${stats.filteredBlocks}")
            appendLine("filterRatio=${"%.4f".format(ratio)}")
            appendLine("dedupSkippedByParent=${stats.dedupSkippedByParent}")
            appendLine("dedupSkippedBySignature=${stats.dedupSkippedBySignature}")
            appendLine("codeSuppressed=${stats.codeSuppressed}")
            appendLine("removedByStructuralSanitize=${stats.removedByStructuralSanitize}")
            appendLine("removedByNoiseFilter=${stats.removedByNoiseFilter}")
            appendLine("nestedSkipped=${stats.nestedSkipped}")
            appendLine("anchorWrappedLiRecovered=${stats.anchorWrappedLiRecovered}")
            appendLine("filteredSamples=")
            stats.filteredSamples.forEach { appendLine("- $it") }
        }
    }

    private data class ExtractTrace(
        var totalBlocks: Int = 0,
        var keptBlocks: Int = 0,
        var filteredBlocks: Int = 0,
        var dedupSkippedByParent: Int = 0,
        var dedupSkippedBySignature: Int = 0,
        var codeSuppressed: Int = 0,
        var nestedSkipped: Int = 0,
        var anchorWrappedLiRecovered: Int = 0,
        val filteredSamples: MutableList<String> = mutableListOf()
    )

    private data class ExtractStats(
        val rawLength: Int,
        val markdownLength: Int,
        val totalBlocks: Int,
        val keptBlocks: Int,
        val filteredBlocks: Int,
        val markdown: String,
        val filteredSamples: List<String>,
        val dedupSkippedByParent: Int,
        val dedupSkippedBySignature: Int,
        val codeSuppressed: Int,
        val removedByStructuralSanitize: Int,
        val removedByNoiseFilter: Int,
        val nestedSkipped: Int,
        val anchorWrappedLiRecovered: Int
    )
}
