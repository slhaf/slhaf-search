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

        fun stage(mode: SearchMode, progress: Int = 0, content: String, query: String): SearchEvent {
            val source = Source(query)
            val data = SearchEventPayload.LoadingStageData(mode, progress, content, source)
            return SearchEvent(Event.STAGE, data)
        }

        fun result(): SearchEvent = TODO()
        fun done(): SearchEvent = TODO()
        fun error(): SearchEvent = TODO()
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
sealed interface SearchEventPayload {

    @Serializable
    data class LoadingStageData(
        val mode: SearchMode,
        val progress: Int,
        val content: String,
        val source: Source
    ) : SearchEventPayload
}


@Serializable
data class Source(
    val query: String
) {
    val id = UUID.randomUUID().toString()
}