package com.mafia2.plugins

import com.mafia2.Working.MafiaGame
import com.mafia2.data.DoAction
import com.mafia2.data.PlayerDet
import com.mafia2.data.Request
import com.mafia2.data.gameSettings
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
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
        webSocket("/play") {
              // websocketSession
            try {
                incoming.consumeEach {

                    if (it is Frame.Text) {


                        when(extractAction(it.readText()) ) {

                            /*"player_details" -> {

                                val playerDetails: PlayerDet =
                                    Json.decodeFromString(it.readText().substringAfter("#"))

                                send(playerDetails.toString())
                                //println(roomId)
                                mafiaGame.connectPlayer(playerDetails, this)
                            }*/
                            "Create_Room"->{
                                val playerDetails: PlayerDet =Json.decodeFromString(it.readText().substringAfter("#"))
                                mafiaGame.createRoom(playerDetails,this)
                            }
                            "Join_Room"->{

                                val request: Request<PlayerDet> = Json.decodeFromString(it.readText().substringAfter("#"))

                                mafiaGame.joinRoom(request.information,this,request.roomId)

                            }


                            "randomize_roles" -> {

                                val roomId = it.readText().substringAfter("#")

                                mafiaGame.rooms[roomId]?.randomizeRoles() ?: {

                                    println("Room not Found")
                                }  //do something here maybe

                            }
                            "game_Settings"->{

                                val request:Request<gameSettings> = Json.decodeFromString(it.readText().substringAfter("#"))
                                mafiaGame.rooms[request.roomId]?.updateGameSettings(request.information)

                            }

                            "start_game" -> {
                                val roomId = it.readText().substringAfter("#")
                                mafiaGame.rooms[roomId]?.startGame() ?:{println("Room not Found")}


                            }

                            "role_action" -> {

                                val request: Request<DoAction> =
                                    Json.decodeFromString(it.readText().substringAfter("#"))

                                mafiaGame.rooms[request.roomId]?.RoleAction(request.information)


                            }

                            "vote" -> {


                                val request: Request<DoAction> =
                                    Json.decodeFromString(it.readText().substringAfter("#"))
                                mafiaGame.rooms[request.roomId]?.vote(request.information)

                            }
                            "restartGame"->{

                                val roomId = it.readText().substringAfter("#")

                                with(mafiaGame.rooms[roomId]!!){

                                    reset()
                                    randomizeRoles()
                                    startGame()

                                }


                            }
                            //Testing purpose
                            //TESTINGGGGGG

                            "DoTasks" -> {

                                val players:List<PlayerDet> = listOf(
                                    PlayerDet("gau")
                                    ,PlayerDet("adnan"),
                                    PlayerDet("ali"),
                                    PlayerDet("mo"),
                                    PlayerDet("mohamed"),
                                )
                                mafiaGame.doTasks_t(players,this)


                            }



                            "showCurrentPlayers"->{
                                val roomId = it.readText().substringAfter("#")
                                mafiaGame.rooms[roomId]?.showCurrentPlayers_t(this)
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

/*private fun DefaultWebSocketServerSession.addPlayers(
    it: String,
    mafiaGame: MafiaGame
) {
    val allPlayers: List<PlayerDet> =
        Json.decodeFromString(it.substringAfter("#"))
    mafiaGame.connectPlayersForTest(allPlayers, this)
}*/

fun extractAction(readText: String): String {

    val message = readText.substringBefore("#")

    return message

}
