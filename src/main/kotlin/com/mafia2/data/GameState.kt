package com.mafia2.data

import kotlinx.serialization.Serializable

enum class Phase{
    GAMESTARTING,
    DAY,
    NIGHT,
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
data class GameState(
    val day:Int=0,
    val currentPhase: Phase = Phase.GAMESTARTING,
    val currentRoleTurn: Role?=null,
    val players:List<Player> = emptyList(),
    val RolesMap:Map<Role,Int> = emptyMap(),


    ){


}

@Serializable
data class DoAction(

    val player: Player,
    val affectedPlayer:Int,

    )