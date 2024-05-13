package com.mafia2

import com.mafia2.Working.MafiaGame
import com.mafia2.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val mafiaGame: MafiaGame = MafiaGame()
    configureMonitoring()
    configureSerialization()
    configureSockets(mafiaGame)
    configureRouting()
}
