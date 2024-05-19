package com.mafia2.Working



import com.mafia2.data.GameState


import com.mafia2.data.Player
import com.mafia2.data.PlayerDet

import io.ktor.server.plugins.NotFoundException
import io.ktor.websocket.WebSocketSession

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


class MafiaGame {



    val rooms = ConcurrentHashMap<String,Room>()
    //val RoomId = AtomicInteger(1000)
    private val playerSockets = ConcurrentHashMap<Int, WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var testRoomId:String=""

    fun createRoom(player: PlayerDet,host: WebSocketSession){
        var roomId:String=""
       do{
           roomId= generateRandomString()
       }while ( rooms.containsKey(roomId))
        rooms[roomId] = Room(
            GameState(
            id = roomId
             )
        )
        testRoomId=roomId


        joinRoom(player,host,roomId,true)


    }

     fun joinRoom(playerdet: PlayerDet,session: WebSocketSession, roomId: String, isHost: Boolean = false,playerConnection:(WebSocketSession)->Int? ={ connectPlayer(it)}) {
        val room = rooms[roomId] ?: throw NotFoundException()
        val playerid = playerConnection(session)
        val player = Player(id = playerid ?: return,name = playerdet.name)
        if(isHost){

            room.setHost(player)

        }
        room.addPlayer(player,session)



    }


    fun connectPlayer( session: WebSocketSession):Int?{

        if(!playerSockets.containsValue(session)){


            /*val Player = Player(id = getRandomId(),name = player.name)*/
            val id = getRandomId()
            playerSockets[id] = session

            /*state.update{
                it.copy(players = it.players + Player)
            }*/
            return id


        }
        else{
            return null
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
        rooms.forEach { (Rid, room) ->

            room.removePlayer(id){


                    rooms.remove(Rid)

            }

        }
        /*state.update{
            val playerToDisconnect =it.players.find {
                it.id == id
            }
            if(playerToDisconnect == null){
                return
            }
                it.copy(players = it.players - playerToDisconnect  )

        }*/

    }


    private fun getRandomId():Int {
        var id = 0
        do {
            id = Random.nextInt(100)

        } while (playerSockets.containsKey(id))
        return id
    }
    /*fun connectPlayersForTest(allPlayers:List<PlayerDet>, session: WebSocketSession){

        allPlayers.forEach {
                player->

            val Player = Player(id = getRandomId(),name = player.name)
            playerSockets[Player.id] = session
            state.update{
                it.copy(players = it.players + Player)
            }

        }


    }*/


    fun generateRandomString(): String {
        val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
        val randomString = (1..4).map { charPool.shuffled().first() }.joinToString("")
        return randomString
    }
    //Testing
    fun doTasks_t(players: List<PlayerDet>, session: WebSocketSession) {
        createRoom(players[0],session)

        for (i in 1 until players.size) {
            joinRoom(players[i],session,testRoomId){
                connectplayer_t(session)
            }


        }
        with(rooms[testRoomId]!!){

            randomizeRoles()
            startGame()

        }



    }


    private fun connectplayer_t(session: WebSocketSession): Int {
        val id = getRandomId()
        playerSockets[id] = session

        /*state.update{
                    it.copy(players = it.players + Player)
                }*/
        return id
    }

    fun doMafiaAction_t(target: Int) {
        with(rooms[testRoomId]!!){
            doMafiaAction_t(target)
        }

    }

    fun doAllVotes_t(target: Int) {
        with(rooms[testRoomId]!!){
            doAllVotes_t(target)
        }

    }

}