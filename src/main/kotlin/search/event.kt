package work.slhaf.search

data class SearchEvent(
    val event: Event,
    val data: String
) {

    companion object {
        fun stage(): SearchEvent = TODO()
        fun response(): SearchEvent = TODO()
        fun done(): SearchEvent = TODO()
        fun error(): SearchEvent = TODO()
    }

    enum class Event {
        STAGE,
        RESULT,
        DONE,
        ERROR,
    }
}