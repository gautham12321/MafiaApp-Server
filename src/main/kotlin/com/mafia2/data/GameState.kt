package com.mafia2.data

import kotlinx.serialization.Serializable

enum class Phase{
    GAMESTARTING,
    DAY,
    NIGHT,
    VOTING,
    GAMEOVER

}
enum class Role{

    DETECTIVE,
    MAFIA,
    DOCTOR,
    CITIZEN,
    GOD

}
@Serializable
data class gameSettings(

    var totalP:Int = defaultSettings().totalP,
    var noGod:Int= defaultSettings().noGod,
    var noDetective :Int = defaultSettings().noDetective,
    var noMafia:Int = defaultSettings().noMafia,
    var noDoctor:Int = defaultSettings().noDoctor,
    var noCitizen:Int = defaultSettings().noCitizen,

    ){

    companion object{

        fun defaultSettings():gameSettings
        {
         return  gameSettings(
             noGod=0,
             noDetective=1,
             noMafia=1,
             noDoctor=1,
             noCitizen=2,
             totalP = 5
         )
        }
        }
}

@Serializable
data class GameState(
    val day: Int = 0,
    val currentPhase: Phase = Phase.GAMESTARTING,
    val currentRoleTurn: Role? = null,
    var players: List<Player> = emptyList(),
    val playersNeeded: Int = 5,
    val RolesMap: Map<Int, Role> = emptyMap(),
    val gameSettings: gameSettings = gameSettings(),
    val toBeKilled: Int? = null,
    val toBeSaved: Int? = null,
    val toSuspect: Int? = null,
    val isSuspect: Boolean = false,
    var isVoting: Boolean = false,
    val isGameOver: Boolean = false,
    val isWinnerMafia: Boolean = false,
    val id: String,
    val host :Int?=null,
    val syncNav:Boolean=false,
    val roleRevealedNo:Int=0,
    val votedPlayersID:List<Int> = emptyList(),


){


    fun getPlayerInstance(userId:Int):Player {

        return players.find { it.id == userId }!!

    }

}

@Serializable
data class DoAction(

    val player: Player,
    val affectedPlayer:Int,

    )