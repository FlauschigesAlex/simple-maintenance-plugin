package at.flauschigesalex.maintenance.listener

import at.flauschigesalex.lib.minecraft.velocity.base.internal.VelocityListener
import at.flauschigesalex.maintenance.SimpleMaintenanceConfig
import at.flauschigesalex.maintenance.SimpleMaintenancePlugin
import at.flauschigesalex.maintenance.utils.Permissions
import at.flauschigesalex.maintenance.utils.Translation
import at.flauschigesalex.maintenance.utils.isMaintenance
import at.flauschigesalex.maintenance.utils.sendMiniMessage
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import kotlin.jvm.optionals.getOrNull

@Suppress("unused")
private class MaintenancePaperListener : VelocityListener() {
    
    @Subscribe
    private fun onInitialServerChose(event: PlayerChooseInitialServerEvent) {
        val player = event.player
        val locale = event.player.effectiveLocale ?: SimpleMaintenanceConfig.defaultLocale
        
        val server = event.initialServer.getOrNull()?.serverInfo ?: return
        
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.serverName.equals(server.name, true) } ?: return
        if (maintenance.isEnabled.not()) return
        
        if (player.hasPermission(Permissions.MAINTENANCE_BYPASS)) return
        
        val message = SimpleMaintenancePlugin.instance.message(locale, server, maintenance)
        
        player.sendRichMessage(message)
        val servers = SimpleMaintenancePlugin.instance.server.configuration.attemptConnectionOrder.mapNotNull {
            SimpleMaintenancePlugin.instance.server.getServer(it).orElse(null)
        }.filterNot { it.isMaintenance() }.toMutableList()

        event.setInitialServer(servers.firstOrNull())
    }
    
    @Subscribe
    private fun onLogin(event: ServerPreConnectEvent) {
        val player = event.player
        val locale = event.player.effectiveLocale ?: SimpleMaintenanceConfig.defaultLocale
        
        val server = event.originalServer.serverInfo ?: return
        
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.serverName.equals(server.name, true) } ?: return
        if (maintenance.isEnabled.not()) return
        
        if (player.hasPermission(Permissions.MAINTENANCE_BYPASS)) return
        
        val message = SimpleMaintenancePlugin.instance.message(locale, server, maintenance)
        
        player.sendRichMessage(message)
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }
    
    @Subscribe
    private fun onJoin(event: ServerPostConnectEvent) {
        val player = event.player
        val locale = event.player.effectiveLocale ?: SimpleMaintenanceConfig.defaultLocale

        val server = event.player.currentServer.getOrNull()?.serverInfo ?: return
        
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.serverName.equals(server.name, true) } ?: return
        if (maintenance.isEnabled.not()) return
        
        if (player.hasPermission(Permissions.MAINTENANCE_BYPASS).not()) return
    
        player.sendMiniMessage("<red><i>"+Translation.translate("maintenance.disconnect.bypass", locale))
    }
}