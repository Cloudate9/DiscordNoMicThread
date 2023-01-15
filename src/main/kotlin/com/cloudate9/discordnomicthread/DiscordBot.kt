package com.cloudate9.discordnomicthread

import com.cloudate9.discordnomicthread.data.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

typealias VcId = Snowflake
typealias NoMicChannel = TextChannel
typealias NoMicThread = TextChannelThread

private val channelMap = mutableMapOf<VcId, Pair<NoMicChannel, NoMicThread>>()

suspend fun main() = runBlocking {

    val configFile = File("DiscordNoMicThreadConfig.json")
    val config: Config = if (configFile.exists()) {
        logger.info { "Config file found at ${configFile.absolutePath}" }
        Json.decodeFromString(configFile.readText())
    } else {
        logger.info { "No config file found, creating one now..." }
        var botToken: String? = null
        var discordNoMicChannelId: Long? = null

        while (botToken == null) {
            logger.info { "Enter your Discord Bot Token" }
            botToken = readlnOrNull()
        }

        while (discordNoMicChannelId == null) {
            logger.info { "Enter your Discord no-mic Channel Id" }
            discordNoMicChannelId = readlnOrNull()?.toLongOrNull()
        }

        Config(botToken, discordNoMicChannelId).also {
            configFile.writeText(Json.encodeToString(it))
            logger.info { "Config file created at ${configFile.absolutePath}" }
        }

    }

    val kord = Kord(config.botToken)

    kord.on<VoiceStateUpdateEvent> {

        old?.channelId?.let { leftChannelId -> // If a user has left a vc
            val threadToLeave = channelMap[leftChannelId]?.second
                ?: run {
                    "Cannot find the thread to leave when ${old?.getMemberOrNull()?.displayName} joined left a vc".also(
                        logger::error
                    )
                    return@on
                }

            old?.userId?.let { threadToLeave.removeUser(it) }
            "Removed ${old?.getMemberOrNull()?.displayName} from no-mic for #${threadToLeave.name}".also(logger::info)

            threadToLeave.join() // Ensure that the bot is in the thread
            if (threadToLeave.memberCount.asNullable == 1) { // If it is 1, it means that the bot is the only member in the thread
                threadToLeave.delete("All members have left the vc")
                logger.info { "No-mic for ${threadToLeave.name} has been closed" }
                channelMap[leftChannelId]?.first?.getLastMessage()?.let {
                   if (it.author?.id == kord.selfId) it.delete("Delete no-mic vc thread message")
                }
            }
            channelMap.remove(leftChannelId)
        }

        state.channelId?.let { joinedChannelId ->  // If user has joined a vc
            val threadToJoin = channelMap[joinedChannelId]?.second
                ?: run {
                    var noMicChannel: TextChannel? = null

                    state.getGuildOrNull()
                        ?.getChannelOfOrNull<TextChannel>(Snowflake(config.discordNoMicChannel))
                        ?.also { noMicChannel = it }
                        ?.startPublicThread("No-mic for vc #${state.getChannelOrNull()?.asChannelOf<VoiceChannel>()?.name}")
                        ?.also {
                            logger.info { "Created ${it.name}" }
                            channelMap[joinedChannelId] = Pair(noMicChannel!!, it)
                        } // Not null as we got it from another ?.also
                }
                ?: run {
                    "Cannot find or create a thread to join when ${state.getMemberOrNull()?.displayName} joined a vc".also(
                        logger::error
                    )
                    return@on
                }

            threadToJoin.addUser(state.userId)
            "Added ${state.getMemberOrNull()?.displayName} to no-mic for ${threadToJoin.name}".also(logger::info)
            threadToJoin.createMessage {
                content = "<@${state.userId}> joined the vc"
            }
        }
    }

    kord.login()
}
