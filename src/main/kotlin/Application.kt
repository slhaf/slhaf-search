package work.slhaf

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.sse.*
import work.slhaf.search.SearchRouter

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(SSE)
    configureSerialization()
    configureRouting()
    monitor.subscribe(ApplicationStopping) {
        SearchRouter.closeAllProviders()
    }
}
