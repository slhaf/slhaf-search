package work.slhaf.search


sealed class SearchEvent<T> {

    abstract val event: Event
    abstract val data: T

    companion object {
        fun stage(): SearchEvent<Any> = TODO()
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