package com.mafia2.plugins

import com.mafia2.Working.MafiaGame
import com.mafia2.data.PlayerDet
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets(mafiaGame: MafiaGame) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/play") { // websocketSession
            try {
                incoming.consumeEach {

                    if (it is Frame.Text) {


                        if (extractAction(it.readText()) == "player_details") { // check if its player details

                            val playerDetails: PlayerDet =
                                Json.decodeFromString(it.readText().substringAfter("#"))

                            this.send(Frame.Text(playerDetails.toString()))
                            //println(roomId)
                            mafiaGame.connectPlayer(playerDetails, this)

                        }

                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                this.close()
            }
        }
    }

}
fun extractAction(readText: String): String {

    val message = readText.substringBefore("#")

    return message

}