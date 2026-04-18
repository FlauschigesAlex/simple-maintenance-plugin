package at.flauschigesalex.maintenance.utils.command

import at.flauschigesalex.lib.minecraft.brigadier.CommandArgumentType
import at.flauschigesalex.maintenance.SimpleMaintenancePlugin
import com.velocitypowered.api.proxy.server.ServerInfo
import net.kyori.adventure.audience.Audience

internal object ServerArgumentType : CommandArgumentType<ServerData>() {
    
    private val servers: Set<ServerData>
        get() {
            val servers = SimpleMaintenancePlugin.instance.server.allServers
                .map { ServerData(it.serverInfo.name, it.serverInfo) }
                .toMutableSet()

            servers.add(ServerData(ServerData.proxyName, null))
            return servers.toSet()
        }
    
    override fun suggestType(value: String, sender: Audience): Boolean =
        servers.any { it.name.startsWith(value, true) }

    override suspend fun parse(value: String, sender: Audience): ServerData? =
        servers.find { it.name.equals(value, true) }

    override fun defaultChatSuggestions(provided: String, sender: Audience): List<String> =
        servers.filter { it.name.startsWith(provided, true) }.map { it.name }
}

data class ServerData(val name: String, val serverInfo: ServerInfo?) {
    companion object {
        var proxyName: String = "velocity"
    }
}