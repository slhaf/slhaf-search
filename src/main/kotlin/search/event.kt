package work.slhaf.search

import java.util.*


sealed class SearchEvent<T> {

    abstract val event: Event
    abstract val data: T

    companion object {

        fun stage(mode: SearchMode, progress: Int = 0, content: String, query: String): SearchEvent<out Any> {
            val source = Source(query)
            val data = LoadingStage.Data(mode, progress, content, source)
            return LoadingStage(data)
        }

        fun result(): SearchEvent<Any> = TODO()
        fun done(): SearchEvent<Any> = TODO()
        fun error(): SearchEvent<Any> = TODO()
    }

    enum class Event {
        STAGE,
        RESULT,
        DONE,
        ERROR,
    }
}

private data class LoadingStage(
    override val data: Data
) : SearchEvent<LoadingStage.Data>() {

    override val event: Event = Event.STAGE

    data class Data(
        val mode: SearchMode,
        val progress: Int,
        val content: String,
        val source: Source
    )
}

data class Source(
    val query: String
) {
    val id = UUID.randomUUID().toString()
}