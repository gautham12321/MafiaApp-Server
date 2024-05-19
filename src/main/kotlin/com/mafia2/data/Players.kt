package com.mafia2.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    var isAlive: Boolean = true,




    )
@Serializable
data class PlayerDet( val name: String)
