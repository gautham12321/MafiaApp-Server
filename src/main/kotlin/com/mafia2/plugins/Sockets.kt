package com.mafia2.plugins

import com.mafia2.Working.MafiaGame
import com.mafia2.data.DoAction
import com.mafia2.data.PlayerDet
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
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


                        when(extractAction(it.readText()) ) {

                            "player_details" -> {

                                val playerDetails: PlayerDet =
                                    Json.decodeFromString(it.readText().substringAfter("#"))

                                send(playerDetails.toString())
                                //println(roomId)
                                mafiaGame.connectPlayer(playerDetails, this)
                            }

                            "randomize_roles" -> {

                                mafiaGame.randomizeRoles()

                            }

                            "start_game" -> {

                                mafiaGame.startGame()


                            }

                            "role_action" -> {

                                val do_action: DoAction =
                                    Json.decodeFromString(it.readText().substringAfter("#"))

                                mafiaGame.RoleAction(do_action)


                            }

                            "vote" -> {


                                val do_action: DoAction =
                                    Json.decodeFromString(it.readText().substringAfter("#"))
                                mafiaGame.vote(do_action)
                            }
                            "restartGame"->{


                                mafiaGame.reset()

                            }
                            //Testing purpose
                            //TESTINGGGGGG
                            "doDefaultTests"->{
                                val players = "t_addPlayers#[\n" +
                                        "    {\"name\":\"gau\"},\n" +
                                        "     {\"name\":\"adam\"},\n" +
                                        "      {\"name\":\"fd\"},\n" +
                                        "       {\"name\":\"sfd\"},\n" +
                                        "        {\"name\":\"gdd\"}\n" +
                                        "\n" +
                                        "    \n" +
                                        "]"
                                mafiaGame.showCurrentPlayers_t(this)
                                addPlayers(players,mafiaGame)

                                mafiaGame.randomizeRoles()
                                mafiaGame.startGame()





                            }
                            "t_addPlayers" -> {

                                addPlayers(it.readText(), mafiaGame)

                            }

                            "showCurrentPlayers"->{
                                mafiaGame.showCurrentPlayers_t(this)
                            }
                            "doCurrentRole"->{
                                val target: Int = it.readText().substringAfter("#").toInt()
                                mafiaGame.doMafiaAction_t(target)
                            }
                            "voteall"->{
                                val target: Int = it.readText().substringAfter("#").toInt()
                                mafiaGame.doAllVotes_t(target)


                            }


                            else->{}


                        }

                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                this.close()
                mafiaGame.disconnectPlayer(this)
            }
        }
    }

}

private fun DefaultWebSocketServerSession.addPlayers(
    it: String,
    mafiaGame: MafiaGame
) {
    val allPlayers: List<PlayerDet> =
        Json.decodeFromString(it.substringAfter("#"))
    mafiaGame.connectPlayersForTest(allPlayers, this)
}

fun extractAction(readText: String): String {

    val message = readText.substringBefore("#")

    return message

}
