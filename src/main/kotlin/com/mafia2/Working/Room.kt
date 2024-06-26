package com.mafia2.Working

import com.mafia2.data.AudioState
import com.mafia2.data.AudioToPlay
import com.mafia2.data.DoAction
import com.mafia2.data.GameState
import com.mafia2.data.Phase
import com.mafia2.data.Player
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class Room (val RoomState: GameState,val audioState: AudioState ){

    var hostPlayer:Player?=null
    val state = MutableStateFlow(RoomState)
    val audiostate = MutableStateFlow(audioState)
    var currentTurn=Role.CITIZEN
    var toKill:Int?=null
    var toSave:Int?=null
    var toSuspect:Int?=null
    var isSuspect:Boolean=false
    var votingMap :MutableMap<Int,Int> = mutableMapOf()
    var isVoting:Boolean=false
    val playerSockets : MutableMap<Int, WebSocketSession> = mutableMapOf()
    val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO )

    fun resetRoomVars(){

        currentTurn=Role.CITIZEN
        toKill=null
        toSave=null
        toSuspect=null
        isSuspect=false
        votingMap.clear()
        isVoting=false
    }

    init {


        audiostate.onEach(::BroadCastAudioState).launchIn(gameScope)
        state.onEach(::BroadCastGameState).launchIn(gameScope)
    }
    suspend fun BroadCastGameState(state:GameState){


        playerSockets.values.forEach { session ->
            println(state)
            session.send(Json.encodeToString(state))
        }

    }
    suspend fun BroadCastAudioState(state:AudioState){


        playerSockets.values.forEach { session ->
            println(state)
            session.send(Json.encodeToString(state))
        }

    }

    fun setHost(player: Player) {
        hostPlayer=player
        state.update {
            it.copy(host = player.id)
        }
    }


    fun addPlayer(player: Player, session: WebSocketSession) {
        if(playerSockets.containsKey(player.id)){
            return

        }
        state.update {
            it.copy(players = it.players.plus(player))
        }

        playerSockets[player.id] = session

    }
    fun removePlayer(id:Int,deleteRoom:()->Unit={},onKillRoom: () -> Unit) {
        state.update {
            it.copy(players = it.players.minus(it.players.find {

                player->
                player.id == id } ?: throw NullPointerException()) )
        }
        if(playerSockets.containsKey(id)){playerSockets.remove(id)}

        if( state.value.currentPhase!= Phase.GAMESTARTING && state.value.currentPhase!=Phase.GAMEOVER && playerSockets.isNotEmpty()){

            onKillRoom()

        }

        if(playerSockets.isEmpty()){
            println("\n\n\n\nRoomDeleted\n\n\n\n")
            deleteRoom()



        }
        else{
            println("\n\n\n\nRoom NOT Deleted : ${state.value}\n\n\n\n")

            val player= state.value.players.random()
            setHost(player)


        }
    }



    fun randomizeRoles(){
        if(state.value.players.size<state.value.playersNeeded){

            return

        }
        val players = state.value.players.toMutableList()
        val roles = state.value.gameSettings
        val roleMap = mutableMapOf<Int, Role>()
        val shuffledRoles = settingsToList_Shuffled(roles).toMutableList()

        players.forEach {
            println(shuffledRoles)
            roleMap[it.id] = shuffledRoles.random()
            shuffledRoles.remove(roleMap[it.id])



        }
        state.update {
            it.copy(RolesMap = roleMap.toMap())
        }




    }
    fun roleRevealed() {
        state.update {
            it.copy(roleRevealedNo = it.roleRevealedNo.inc())
        }



    }

    fun updateGameSettings(settings: gameSettings) {
        val noPlayers = settings.totalP // changed up here to support the new gamesettings with totalp
        state.update {
            it.copy(gameSettings = settings, playersNeeded = noPlayers)
        }

    }
    fun startGame() {
        state.update {

            it.copy(currentPhase = Phase.DAY, day = 1,)

        }
        println("CHECK LOOP")

        gameScope.launch {
            println("COROTINE START")
            while(true /*&& isActive*/){ //might or might not work
                println("$state:INFINITE LOOP")
                var mafia:Int = 0
                var normalPlayers:Int=0

                val pair = setMafia_Norm()
                mafia = pair.first
                normalPlayers = pair.second


                delay(3000)
                audiostate.update {
                    it.copy(AudioToPlay.VILLAGERCLOSE)
                }
                state.update {

                    it.copy(currentPhase = Phase.NIGHT,isSuspect = false,isVoting = false, toBeKilled = null, toBeSaved = null,toSuspect = null)

                }
                delay(5000)
                audiostate.update {
                    it.copy(AudioToPlay.DOCTOR_WAKE)
                }
                state.update {

                    it.copy(currentRoleTurn = Role.DOCTOR)



                }
                currentTurn= Role.DOCTOR
                val isDoctorAlive=findIfRoleAlive(Role.DOCTOR)
                while(currentTurn== Role.DOCTOR && isDoctorAlive)

                    delay(1000)

                if(!isDoctorAlive){


                    fakeOutRole()
                }
                audiostate.update {
                    it.copy(AudioToPlay.DOCTOR_CLOSE)
                }
                state.update {

                    it.copy(currentRoleTurn = null)


                }
                delay(5000)
                audiostate.update {
                    it.copy(AudioToPlay.MAFIA_WAKE)
                }
                state.update {

                    it.copy(currentRoleTurn = Role.MAFIA)


                }
                currentTurn= Role.MAFIA


                while(currentTurn== Role.MAFIA && mafia>0)
                    delay(1000)
                if(mafia==0){

                    fakeOutRole()
                }
                audiostate.update {
                    it.copy(AudioToPlay.MAFIA_CLOSE)
                }
                state.update {

                    it.copy(currentRoleTurn = null)


                }
                delay(5000)
                audiostate.update {
                    it.copy(AudioToPlay.DETECTIVE_WAKE)
                }
                state.update {

                    it.copy(currentRoleTurn = Role.DETECTIVE)

                }
                currentTurn= Role.DETECTIVE

                val detectiveIsAlive = findIfRoleAlive(role = Role.DETECTIVE)

                while(currentTurn== Role.DETECTIVE && detectiveIsAlive)
                    delay(1000)

                if(!detectiveIsAlive){

                    fakeOutRole()
                }

                println(toKill)

                delay(5000)
                state.update {

                    it.copy(currentRoleTurn = null)


                }
                //delay(5000)
                audiostate.update {
                    it.copy(AudioToPlay.DETECTIVE_CLOSE)
                }
                delay(5000)

                state.update {

                    it.copy(currentRoleTurn = null, currentPhase = Phase.DAY, toBeKilled = toKill,
                        day = it.day + 1, toBeSaved = toSave, toSuspect = toSuspect)



                }
                audiostate.update {
                    it.copy(
                        if(toKill==null)
                            AudioToPlay.WAKE_NODEATH
                        else
                            AudioToPlay.WAKE_WITHDEATH

                    )

                }
                if(toKill!=null){

                    state.update {

                        val p= it.players.map {

                            if(it.id==toKill){
                                Player(it.id,it.name,false,it.avatar)
                            }else{
                                Player(it.id,it.name,it.isAlive,it.avatar)
                            }
                        }
                        it.copy(players = p)

                    }

                }
                val Spair = setMafia_Norm()
                mafia = Spair.first
                normalPlayers = Spair.second


                if (checkifGameOver(mafia, normalPlayers)) return@launch
                delay(5000)
                audiostate.update {
                    it.copy(AudioToPlay.START_VOTE)
                }
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

    private suspend fun fakeOutRole() {
        delay(Random.nextLong(5000, 10000))
    }

    private fun findIfRoleAlive(role: Role ) =
        state.value.players.find { state.value.RolesMap[it.id] == role }!!.isAlive

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
        if (mafia >= normalPlayers || mafia == 0) {

            audiostate.update { it.copy(if(mafia==normalPlayers) AudioToPlay.MAFIA_WIN else AudioToPlay.VILLAGER_WIN) }
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

        if(state.value.RolesMap[toSuspect]== Role.MAFIA){

            isSuspect=true



        }else{
            isSuspect=false
        }
        state.update {
            it.copy(isSuspect = isSuspect,toSuspect = toSuspect)

        }

        currentTurn= Role.CITIZEN


    }

    fun doctorAction(do_action: DoAction) {
        toSave=do_action.affectedPlayer
        currentTurn= Role.CITIZEN

    }
    fun mafiaAction(do_action: DoAction) {

        toKill=do_action.affectedPlayer
        if(toKill==toSave){
            toKill=null

        }


        currentTurn= Role.CITIZEN
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
        state.update {
            it.copy(votedPlayersID = it.votedPlayersID.plus(action.player.id) )
        }

        var noVotes =0
        votingMap.forEach { _, i2 ->

            noVotes+=i2
        }

        if(noVotes==state.value.players.filter { it.isAlive }.size){
            //println("Kicked")

            voteKick()
        }


    }
    fun voteKick()
    {

        var highest = votingMap.keys.first()
        var noOfvotes = votingMap[highest]!!
        votingMap.forEach {
            if(it.value>votingMap[highest]!!){

                highest=it.key
                noOfvotes=it.value

            }
        }
        if(votingMap.minus(highest).containsValue(noOfvotes)){

            highest=-1
        }

        println(highest)
        println(votingMap)
        gameScope.launch {
            delay(3000)
            audiostate.update {
                it.copy(if(highest==-1) AudioToPlay.VOTE_NOTKICKED else AudioToPlay.VOTE_KICKED)
            }

            if (highest != -1) { // sends -1 if skip vote
                state.update {


                    val p = it.players.map {

                        if (it.id == highest) {
                            Player(it.id, it.name, false, it.avatar)
                        } else {
                            Player(it.id, it.name, it.isAlive, it.avatar)
                        }
                    }
                    it.copy(players = p)

                }
            }
            delay(3000)

            state.update { it.copy(isVoting = false, votedPlayersID = emptyList()) }
            println("Updated")
            isVoting = false
            votingMap.clear()

        }

    }
    fun reset(){

        resetRoomVars()

        state.update {

            val p= it.players.map {


                Player(it.id,it.name,true,it.avatar)

            }
            it.copy(day = 0,currentPhase = Phase.GAMESTARTING,currentRoleTurn = null, RolesMap = emptyMap(),
                isGameOver = false, isWinnerMafia = false, toBeKilled = null, toBeSaved = null, toSuspect = null,
                isVoting = false, isSuspect = false, players = p,roleRevealedNo = 0)

        }

    }
    fun syncPlayers() {

        state.update {
            it.copy(syncNav = true)
        }
        gameScope.launch {
            delay(5000)
            setSyncOff()
        }

    }
    fun setSyncOff(){
        state.update {
            it.copy(syncNav = false)
        }


    }


    //Testing

    fun showCurrentPlayers_t(session: WebSocketSession) {

        gameScope.launch {
            session.send(Json.encodeToString(state.value.players))
        }
    }
    fun doMafiaAction_t(target:Int)
    {

        val Player = state.value.players.find { state.value.RolesMap[it.id] == state.value.currentRoleTurn } ?: return


        val doAction = DoAction(Player,target)

        RoleAction(doAction)



    }
    fun doAllVotes_t(target:Int){
        state.value.players.forEach {
            println(it.id)
            vote(DoAction(it,target))

        }


    }

    fun exitRoom(session: WebSocketSession) {


    }


}
