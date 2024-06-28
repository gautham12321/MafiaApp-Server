package com.mafia2.Working



import com.mafia2.data.AudioState
import com.mafia2.data.GameState


import com.mafia2.data.Player
import com.mafia2.data.PlayerDet
import com.mafia2.data.Request
import com.mafia2.data.Setup

import io.ktor.server.plugins.NotFoundException
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


class MafiaGame {



    val rooms = ConcurrentHashMap<String,Room>()
    val setup = MutableStateFlow(Setup())
    //val RoomId = AtomicInteger(1000)
    private val playerSockets = ConcurrentHashMap<Int, WebSocketSession>()



    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var testRoomId:String=""

    fun createRoom(player: PlayerDet,host: WebSocketSession)
    {


        var roomId:String=""
       do{
           roomId= generateRandomString()
       }while ( rooms.containsKey(roomId))
        rooms[roomId] = Room(
            GameState(
            id = roomId
             ), AudioState()
        )
        testRoomId=roomId

println(rooms)
        joinRoom(player, host,roomId,true)


    }

     fun joinRoom(playerdet: PlayerDet,session: WebSocketSession, roomId: String, isHost: Boolean = false,
                  playerConnection:(WebSocketSession)->Int? ={ connectPlayer(it)}) {
        val room = rooms[roomId] ?: throw NotFoundException()
        val playerid = playerConnection(session)
        val player = Player(id = playerid ?: return,name = playerdet.name,avatar = playerdet.avatar)
        if(isHost){

            room.setHost(player)

        }
         setup.update {
             it.copy(playerDetails = player, hostDetails = rooms[roomId]?.hostPlayer)
         }
         gameScope.launch {
             session.send(Json.encodeToString(setup.value))
         }

        room.addPlayer(player,session)
    println(room.playerSockets)


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

    fun disconnectPlayer(session: WebSocketSession,userCalled:Boolean=false){ // updated it to use session
        var id =0

        playerSockets.forEach { (i, webSocketSession) ->
            if(webSocketSession==session){
                if(!userCalled) {
                    playerSockets.remove(i)
                }
                id =i
            }

        }
        rooms.forEach { (Rid, room) ->
            if(room.playerSockets.containsKey(id)) {

                room.removePlayer(id=id, deleteRoom = {


                    rooms.remove(Rid)

                },onKillRoom={
                    room.state.update {
                        it.copy(syncNav = true)}
                    gameScope.launch {
                        delay(4000)
                        room.state.update {
                            it.copy(syncNav = false)}
                    }






                })
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
    fun searchRoom(roomId: String, session: WebSocketSession) {
        val roomExists=rooms.containsKey(roomId)
        val hostId = rooms[roomId]?.state?.value?.host
        setup.update {
            it.copy(roomFound = roomExists,)
        }
        val frame = Frame.Text(Json.encodeToString(setup.value))
        gameScope.launch {
            session.send(frame)
        }

    }

    fun generateRandomString(): String {
        val charPool : List<Char> = ('A'..'Z')-('O')
        val randomString = (1..6).map { charPool.shuffled().first() }.joinToString("")
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

    fun getRoomUpdate(session: WebSocketSession) {

        rooms.forEach { (Rid, room) ->
           if (room.playerSockets.values.contains(session))
           {
               room.sendUpdate(session)

           }

        }

    }


}