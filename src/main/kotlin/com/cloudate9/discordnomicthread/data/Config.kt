package com.cloudate9.discordnomicthread.data

import kotlinx.serialization.Serializable

@Serializable
data class Config(val botToken: String, val discordNoMicChannel: Long)
