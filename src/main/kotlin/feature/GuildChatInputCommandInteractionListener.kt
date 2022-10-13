package feature

import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction

interface GuildChatInputCommandInteractionListener {
    val command: String
    suspend fun onGuildChatInputCommand(interaction: GuildChatInputCommandInteraction)
}