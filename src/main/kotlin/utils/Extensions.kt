package at.flauschigesalex.maintenance.utils

import at.flauschigesalex.maintenance.SimpleMaintenanceConfig
import com.velocitypowered.api.proxy.ConnectionRequestBuilder
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal val Audience.locale: Locale get() {
    val locale = (this as? Player)?.playerSettings?.locale
    return locale ?: SimpleMaintenanceConfig.defaultLocale
}

internal fun Audience.sendMiniMessage(message: String) = sendMessage(
    MiniMessage.miniMessage().deserialize("<red>${Translation.translate("maintenance.title", locale)} <dark_gray>» <gray>$message")
)

internal fun Audience.username(locale: Locale): String = when (this) {
        is Player -> this.username
        else -> Translation.translate("console.name", locale)
    }

internal fun RegisteredServer.isMaintenance(): Boolean =
    SimpleMaintenanceConfig.maintenance.any { it.serverName.equals(this.serverInfo.name, true) }

internal fun Player.createConnectionRequests(
    servers: Collection<RegisteredServer>
): CompletableFuture<RegisteredServer>? {
    
    if (servers.isEmpty()) return null
    val server = servers.firstOrNull() ?: return null

    return this.createConnectionRequest(server).connect().thenCompose { result ->
        when (result.status) {
            ConnectionRequestBuilder.Status.SUCCESS,
            ConnectionRequestBuilder.Status.ALREADY_CONNECTED -> {
                CompletableFuture.completedFuture(server)
            }

            ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS,
            ConnectionRequestBuilder.Status.CONNECTION_CANCELLED,
            ConnectionRequestBuilder.Status.SERVER_DISCONNECTED -> {
                this.createConnectionRequests(servers.drop(1))
            }
        }
    }
}