package work.slhaf

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import work.slhaf.search.SearchMode
import work.slhaf.search.SearchRouter
import java.util.Locale.getDefault

fun Application.configureRouting() {

    routing {
        sse("/search") {
            val request = call.request
            val query = request.queryParameters["q"] ?: return@sse call.respond(
                HttpStatusCode.BadRequest,
                "missing query parameter q"
            )
            val mode = request.queryParameters["mode"]
                ?.uppercase()
                ?.let { SearchMode.valueOf(it) }
                ?: SearchMode.NORMAL
            val size = request.queryParameters["size"]?.toIntOrNull() ?: 30
            val provider = request.queryParameters["provider"] ?: "default"

            val flow = SearchRouter.search(mode, provider, query, size)
            flow.collect { event ->
                val sendEvent = ServerSentEvent(
                    event = event.event.name.lowercase(getDefault()),
                    data = event.data
                )
                send(sendEvent)
            }
        }
    }
}
