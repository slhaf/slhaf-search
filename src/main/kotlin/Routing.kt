package work.slhaf

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import work.slhaf.search.SearchMode
import work.slhaf.search.SearchRouter

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
            val pageSize = request.queryParameters["size"]?.toIntOrNull() ?: 10
            val provider = request.queryParameters["provider"] ?: "default"

            val flow = SearchRouter.search(mode, provider, query, pageSize)
            flow.collect { searchEvent ->
                val (event, data) = searchEvent.serialize()
                val sendEvent = ServerSentEvent(
                    event = event,
                    data = data
                )
                send(sendEvent)
            }
        }

        sse("/search/{id}") {
            val id = call.parameters["id"] ?: return@sse call.respond(
                HttpStatusCode.BadRequest,
                "missing path parameter id"
            )
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

            val flow = SearchRouter.selectPage(
                id = id,
                page = page,
                pageSize = pageSize
            )
            flow.collect { searchEvent ->
                val (event, data) = searchEvent.serialize()
                val sendEvent = ServerSentEvent(
                    event = event,
                    data = data
                )
                send(sendEvent)
            }
        }
    }
}
