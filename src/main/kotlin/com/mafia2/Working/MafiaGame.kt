package com.mafia2.Working


import com.mafia2.data.DoAction
import com.mafia2.data.GameState
import com.mafia2.data.Phase

import com.mafia2.data.Player
import com.mafia2.data.PlayerDet
import com.mafia2.data.Role

import com.mafia2.data.gameSettings
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


class MafiaGame {


    val state = MutableStateFlow(GameState())
    private val playerSockets = ConcurrentHashMap<Int, WebSocketSession>()
    var currentTurn=Role.CITIZEN
    var toKill:Int?=null
    var toSave:Int?=null
    var toSuspect:Int?=null
    var isSuspect:Boolean=false
    var votingMap :MutableMap<Int,Int> = mutableMapOf()
    var isVoting:Boolean=false
    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {

        state.onEach(::BroadCast).launchIn(gameScope)

    }

    fun connectPlayer(player: PlayerDet, session: WebSocketSession, roomId:Int?=null){

        if(!playerSockets.containsValue(session)){


            val Player = Player(id = getRandomId(),name = player.name)
            playerSockets[Player.id] = session

            state.update{
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
        state.update{
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

    fun randomizeRoles(){
        if(state.value.players.size<5){

            return

        }
        val players = state.value.players.toMutableList()
        val roles = state.value.gameSettings
        val roleMap = mutableMapOf<Int, Role>()
        val shuffledRoles = settingsToList_Shuffled(roles).toMutableList()

        players.forEach {

                roleMap[it.id] = shuffledRoles.random()
            shuffledRoles.remove(roleMap[it.id])



        }
        state.update {
            it.copy(RolesMap = roleMap.toMap())
        }




    }
    fun startGame() {
        state.update {
            it.copy(currentPhase = Phase.DAY, day = 1,)

        }
        gameScope.launch {
            while(true){
                var mafia:Int = 0
                var normalPlayers:Int=0

                val pair = setMafia_Norm()
                mafia = pair.first
                normalPlayers = pair.second

                state.update {

                    it.copy(currentPhase = Phase.NIGHT,isSuspect = false,isVoting = false, toBeKilled = null, toBeSaved = null,toSuspect = null)

                }
                delay(1000)
                state.update {

                    it.copy(currentRoleTurn = Role.DOCTOR)



                }
                currentTurn=Role.DOCTOR

                while(currentTurn==Role.DOCTOR)

                delay(1000)
                state.update {

                    it.copy(currentRoleTurn = Role.MAFIA)


                }
                currentTurn=Role.MAFIA

                while(currentTurn==Role.MAFIA)
                delay(1000)
                state.update {

                    it.copy(currentRoleTurn = Role.DETECTIVE)

                }
                currentTurn=Role.DETECTIVE

                while(currentTurn==Role.DETECTIVE)
                delay(1000)

                println(toKill)
                state.update {

                    it.copy(currentRoleTurn = null, currentPhase = Phase.DAY,
                        day = it.day + 1, toBeKilled = toKill, toBeSaved = toSave, toSuspect = toSuspect)



                }
                if(toKill!=null){

                    state.update {

                        val p= it.players.map {

                            if(it.id==toKill){
                                Player(it.id,it.name,false)
                            }else{
                                Player(it.id,it.name,it.isAlive)
                            }
                        }
                        it.copy(players = p)

                    }

                }
                val Spair = setMafia_Norm()
                mafia = Spair.first
                normalPlayers = Spair.second


                if (checkifGameOver(mafia, normalPlayers)) return@launch

                state.update {
                    it.copy(isVoting = true)

                }
                isVoting=true

                while(isVoting)
                    delay(1000)




                val Tpair = setMafia_Norm()
                mafia = Tpair.first
                normalPlayers = Tpair.second

                if (checkifGameOver(mafia, normalPlayers)) return@launch

            }


        }

    }

    private fun setMafia_Norm(


    ): Pair<Int, Int> {
        var mafia1 = 0
        var normalPlayers1 = 0
        /*state.value.RolesMap.values.forEach {
            if (it == Role.MAFIA) {
                state.value.players.forEach { player ->

                    if (state.value.RolesMap[player.id] == Role.MAFIA && player.isAlive) {
                        mafia1++

                    }


                }

            }
        }*/

        with(state.value){
            RolesMap.forEach{

               if( players.find {player-> player.id==it.key }!!.isAlive) {
                    if(it.value==Role.MAFIA ){
                        mafia1++
                    }
                    else {
                        normalPlayers1++
                    }
                }

            }


        }

        return Pair(mafia1, normalPlayers1)
    }

    private fun checkifGameOver(mafia: Int, normalPlayers: Int): Boolean {
        println("$mafia#$normalPlayers")
        if (mafia == normalPlayers || mafia == 0) {

            state.update {
                it.copy(currentPhase = Phase.GAMEOVER, isGameOver = true, isWinnerMafia = mafia == normalPlayers )
            }
            isVoting = false
            return true


        }
        return false
    }

    fun detectiveAction(do_action: DoAction) {


        toSuspect = do_action.affectedPlayer
         if(state.value.RolesMap[toSuspect]==Role.MAFIA){

             isSuspect=true



         }
         state.update {
             it.copy(isSuspect = isSuspect)

         }

         currentTurn=Role.CITIZEN


    }

     fun doctorAction(do_action: DoAction) {
        toSave=do_action.affectedPlayer
        currentTurn=Role.CITIZEN

    }
     fun mafiaAction(do_action: DoAction) {

         toKill=do_action.affectedPlayer
         if(toKill==toSave){
             toKill=null

         }


         currentTurn=Role.CITIZEN
    }

    fun RoleAction(do_action: DoAction) {
        when(state.value.RolesMap[do_action.player.id] ){
            Role.MAFIA->{
                mafiaAction(do_action)
            }
            Role.DETECTIVE->{
                detectiveAction(do_action)
            }
            Role.DOCTOR->{
                doctorAction(do_action)
            }
            else->{}
        }
    }

    fun settingsToList_Shuffled(roles: gameSettings):List<Role>{

        val list = mutableListOf<Role>()

        repeat(roles.noGod) {
            list.add(Role.GOD)
        }

        // Add MAFIA roles
        repeat(roles.noMafia) {
            list.add(Role.MAFIA)
        }

        // Add DETECTIVE roles
        repeat(roles.noDetective) {
            list.add(Role.DETECTIVE)
        }

        // Add DOCTOR roles
        repeat(roles.noDoctor) {
            list.add(Role.DOCTOR)
        }

        // Add CITIZEN roles
        repeat(roles.noCitizen) {
            list.add(Role.CITIZEN)
        }
        return list.toList()

    }

    fun vote(action: DoAction) {
        if(!state.value.isVoting) return
        isVoting=true

        if(votingMap.containsKey(action.affectedPlayer))
        {
            votingMap[action.affectedPlayer]= votingMap[action.affectedPlayer]!!.inc()
                println(votingMap[action.affectedPlayer])

            }
            else{
            votingMap[action.affectedPlayer]=1

            }
        var noVotes =0
        votingMap.forEach { _, i2 ->

            noVotes+=i2
        }

        if(noVotes==state.value.players.size){
            println("Kicked")

            voteKick()
        }


    }
    fun voteKick()
    {

       var highest = votingMap.keys.first()
        votingMap.forEach {
            if(it.value>votingMap[highest]!!){

                highest=it.key

            }
        }
        println(highest)
        println(votingMap)

        state.update {

           val p= it.players.map {

               if(it.id==highest){
                   Player(it.id,it.name,false)
                }else{
                    Player(it.id,it.name,it.isAlive)
                }
            }
            it.copy(players = p, isVoting = false)

        }
        println("Updated")
        isVoting=false
        votingMap.clear()

    }
    fun reset(){

        state.update {

            val p= it.players.map {


                    Player(it.id,it.name,true)

            }
            it.copy(   day = 0,currentPhase = Phase.DAY,currentRoleTurn = null, RolesMap = emptyMap(),
                isGameOver = false, isWinnerMafia = false, toBeKilled = null, toBeSaved = null, toSuspect = null,
                isVoting = false, votersList = emptyMap(), isSuspect = true, players = p)

        }

    }

    //Testing
    fun connectPlayersForTest( allPlayers:List<PlayerDet>,session: WebSocketSession){

        allPlayers.forEach {
                player->

            val Player = Player(id = getRandomId(),name = player.name)
            playerSockets[Player.id] = session
            state.update{
                it.copy(players = it.players + Player)
            }

        }


    }
    fun showCurrentPlayers_t(session: WebSocketSession) {

       gameScope.launch {
           session.send(Json.encodeToString(state.value.players))
       }
    }
    fun doMafiaAction_t(target:Int) {

        val Player = state.value.players.find { state.value.RolesMap[it.id] == state.value.currentRoleTurn } ?: return
        println("DONEEE")
        val doAction = DoAction(Player,target)

        RoleAction(doAction)



    }
    fun doAllVotes_t(target:Int){
        state.value.players.forEach {
            println(it.id)
            vote(DoAction(it,target))

        }


    }


}