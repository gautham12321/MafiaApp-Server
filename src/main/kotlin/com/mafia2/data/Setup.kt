package com.mafia2.data

import kotlinx.serialization.Serializable

@Serializable
data class Setup(val roomFound:Boolean=false,val hostDetails: Player?=null,val playerDetails: Player?=null)
