package com.mafia2.Working

import com.mafia.data.GameState

import com.mafia2.data.Player
import com.mafia2.data.PlayerDet
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


class MafiaGame {


    val gameState = MutableStateFlow(GameState())
    private val playerSockets = ConcurrentHashMap<Int, WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {

        gameState.onEach(::BroadCast).launchIn(gameScope)

    }

    fun connectPlayer(player: PlayerDet, session: WebSocketSession, roomId:Int?=null){

        if(!playerSockets.containsValue(session)){


            val Player = Player(id = getRandomId(),name = player.name)
            playerSockets[Player.id] = session

            gameState.update{
                it.copy(players = it.players + Player)
            }


        }
        else{
            return
        }



    }
    fun disconnectPlayer(session: WebSocketSession){ // updated it to use session
        var id =0

        playerSockets.forEach { (i, webSocketSession) ->
            if(webSocketSession==session){

                playerSockets.remove(i)
                id =i
            }

        }
        gameState.update{
            val playerToDisconnect =it.players.find {
                it.id == id
            }
            if(playerToDisconnect == null){
                return
            }
                it.copy(players = it.players - playerToDisconnect  )

        }

    }
    suspend fun BroadCast(state:GameState){


        playerSockets.values.forEach { session ->
            session.send(Json.encodeToString(state))
        }

    }


    private fun getRandomId():Int {
        var id = 0
        do {
            id = Random.nextInt(100)

        } while (playerSockets.containsKey(id))
        return id
    }


}