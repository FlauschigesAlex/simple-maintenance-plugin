package at.flauschigesalex.maintenance.utils.command

import at.flauschigesalex.lib.minecraft.brigadier.CommandArgumentType
import net.kyori.adventure.audience.Audience
import java.time.Duration

internal object DurationArgumentType : CommandArgumentType<Duration>() {
    
    private val map = mapOf<Char, Duration>(
        's' to Duration.ofSeconds(1),
        'm' to Duration.ofMinutes(1),
        'h' to Duration.ofHours(1),
        'd' to Duration.ofDays(1),
    )
    
    override fun suggestType(value: String, sender: Audience): Boolean {
        if (value.isEmpty()) return true
        value.toLongOrNull()?.let { 
            return true
        }
        
        val chars = value.dropWhile { it.isDigit() }
        if (chars.length != 1) return false
        val char = chars[0]
        return map.containsKey(char) || map.containsKey(char.uppercase()[0])
    }

    override suspend fun parse(value: String, sender: Audience): Duration? {
        val num = value.dropLast(1).toLongOrNull() ?: return null
        val char = value.lowercase().lastOrNull() ?: return null
        
        val mapped = map[char] ?: return null
        val duration = Duration.ofSeconds(num).multipliedBy(mapped.toSeconds())
        return duration
    }

    override fun defaultChatSuggestions(provided: String, sender: Audience): List<String> {
        val num = provided.toLongOrNull() ?: return emptyList()
        return map.keys.map { "$num${it}" }
    }
}