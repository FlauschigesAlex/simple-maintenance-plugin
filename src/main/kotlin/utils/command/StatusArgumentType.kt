package at.flauschigesalex.maintenance.utils.command

import at.flauschigesalex.lib.minecraft.brigadier.CommandArgumentType
import net.kyori.adventure.audience.Audience

internal object StatusArgumentType : CommandArgumentType<Boolean>() {
    
    private val entries = listOf("on" to true, "off" to false)
    
    override fun suggestType(value: String, sender: Audience): Boolean =
        entries.any { it.first.startsWith(value, true) }

    override suspend fun parse(value: String, sender: Audience): Boolean? =
        entries.find { it.first.equals(value, true) }?.second

    override fun defaultChatSuggestions(provided: String, sender: Audience): List<String> = entries.map { it.first }
}