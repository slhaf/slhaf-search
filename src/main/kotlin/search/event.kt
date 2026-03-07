package work.slhaf.search

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*


@ConsistentCopyVisibility
data class SearchEvent private constructor(
    val event: Event,
    val data: SearchEventPayload
) {

    companion object {

        fun stage(
            mode: SearchMode,
            query: String,
            progress: Int = 0,
            content: String
        ): SearchEvent = buildSearchEvent(Event.STAGE, query) {
            LoadingStageData(it, mode, progress, content)
        }

        fun result(
            query: String,
            suppliedSources: List<Source>
        ): SearchEvent = buildSearchEvent(Event.RESULT, query) {
            SearchResult.Agentic(it, suppliedSources)
        }

        fun result(
            query: String,
            requestId: String,
            activeAbstract: String
        ): SearchEvent = buildSearchEvent(Event.RESULT, query) {
            SearchResult.ActiveEnhance(it, requestId, activeAbstract)
        }

        fun result(
            query: String,
            requestId: String,
            summaries: List<String>
        ): SearchEvent = buildSearchEvent(Event.RESULT, query) {
            SearchResult.ShortEnhance(it, requestId, summaries)
        }

        fun result(
            query: String,
            webContents: List<WebContent>,
            page: Int,
            pageSize: Int,
        ): SearchEvent = buildSearchEvent(Event.RESULT, query) {

            val totalCount = webContents.size

            val pageCount = if (totalCount == 0) {
                0
            } else {
                (totalCount + pageSize - 1) / pageSize
            }

            val safePage = page.coerceAtLeast(1)

            val start = (safePage - 1) * pageSize
            val end = (start + pageSize).coerceAtMost(totalCount)

            val results =
                if (start in 0 until totalCount)
                    webContents.subList(start, end)
                else
                    emptyList()

            SearchResult.Search(
                source = it,
                totalCount = totalCount,
                pageCount = pageCount,
                results = results
            )
        }

        fun done(
            mode: SearchMode,
            query: String,
            requestId: String
        ): SearchEvent = buildSearchEvent(Event.DONE, query) {
            SearchDone(it, requestId, mode)
        }

        fun error(
            mode: SearchMode,
            query: String,
            errors: List<String>
        ): SearchEvent = buildSearchEvent(Event.ERROR, query) {
            SearchError(it, mode, errors)
        }

        private fun buildSearchEvent(
            event: Event,
            query: String,
            buildPayload: (source: Source) -> SearchEventPayload
        ): SearchEvent {
            val source = Source(query)
            val payload = buildPayload(source)
            return SearchEvent(event, payload)
        }
    }

    fun serialize(): Pair<String, String> {
        return event.name.lowercase() to Json.encodeToString(data)
    }

    enum class Event {
        STAGE,
        RESULT,
        DONE,
        ERROR
    }
}

@Serializable
sealed class SearchEventPayload {

    abstract val source: Source

}

@Serializable
data class SearchDone(
    override val source: Source,
    val resultId: String,
    val mode: SearchMode
) : SearchEventPayload() {
}

@Serializable
sealed class SearchResult : SearchEventPayload() {

    @Serializable
    data class Agentic(
        override val source: Source,
        val suppliedSources: List<Source>
    ) : SearchResult()

    @Serializable
    data class ActiveEnhance(
        override val source: Source,
        val resultId: String,
        val activeAbstract: String
    ) : SearchResult()

    @Serializable
    data class ShortEnhance(
        override val source: Source,
        val resultId: String,
        val summaries: List<String>
    ) : SearchResult() {
        init {
            require(summaries.size in 1..3)
        }
    }

    @Serializable
    data class Search(
        override val source: Source,
        val totalCount: Int,
        val pageCount: Int,
        val results: List<WebContent>
    ) : SearchResult()
}

@Serializable
data class SearchError(
    override val source: Source,
    val mode: SearchMode,
    val errors: List<String>
) : SearchEventPayload()

@Serializable
data class LoadingStageData(
    override val source: Source,
    val mode: SearchMode,
    val progress: Int,
    val content: String
) : SearchEventPayload()

@Serializable
data class WebContent(
    val title: String,
    val abstract: String,
    val preview: String,
    val link: String
) {
    val resultId = UUID.randomUUID().toString()
}

@Serializable
data class Source(
    val query: String
) {
    val id = UUID.randomUUID().toString()
}