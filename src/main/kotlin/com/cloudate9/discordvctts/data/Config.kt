package com.cloudate9.discordvctts.data

import kotlinx.serialization.Serializable

@Serializable
data class Config(val botToken: String, val discordNoMicChannel: Long)
