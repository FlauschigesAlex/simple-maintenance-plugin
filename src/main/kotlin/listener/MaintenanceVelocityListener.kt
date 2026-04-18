package at.flauschigesalex.maintenance.listener

import at.flauschigesalex.lib.base.time.countdown.Countdown
import at.flauschigesalex.lib.minecraft.velocity.base.internal.VelocityListener
import at.flauschigesalex.maintenance.SimpleMaintenanceConfig
import at.flauschigesalex.maintenance.SimpleMaintenancePlugin
import at.flauschigesalex.maintenance.utils.Permissions
import at.flauschigesalex.maintenance.utils.Translation
import at.flauschigesalex.maintenance.utils.sendMiniMessage
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Duration
import java.time.Instant
import java.util.*

@Suppress("unused")
private class MaintenanceVelocityListener : VelocityListener() {
    
    @Subscribe
    private fun onLogin(event: LoginEvent) {
        val player = event.player
        val locale = event.player.effectiveLocale ?: SimpleMaintenanceConfig.defaultLocale
        
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.isVelocity } ?: return
        if (maintenance.isEnabled.not()) return
        
        if (player.hasPermission(Permissions.MAINTENANCE_BYPASS)) return

        val message = SimpleMaintenancePlugin.instance.message(locale, null, maintenance)
        
        event.result = ResultedEvent.ComponentResult.denied(MiniMessage.miniMessage().deserialize(message))
    }
    
    @Subscribe
    private fun onJoin(event: PostLoginEvent) {
        val player = event.player
        val locale = event.player.effectiveLocale ?: SimpleMaintenanceConfig.defaultLocale
        
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.isVelocity } ?: return
        if (maintenance.isEnabled.not()) return
        
        if (player.hasPermission(Permissions.MAINTENANCE_BYPASS).not()) return
    
        player.sendMiniMessage("<red><i>"+Translation.translate("maintenance.disconnect.bypass", locale))
    }
    
    @Subscribe
    private fun onPing(event: ProxyPingEvent) {
        val maintenance = SimpleMaintenanceConfig.maintenance.find { it.isVelocity } ?: return
        if (maintenance.isEnabled.not()) return
        val locale = SimpleMaintenanceConfig.defaultLocale

        val lastDigit = Instant.now().epochSecond % 10
        val remainingDuration = maintenance.remaining
        val countdown = Countdown.create(remainingDuration ?: Duration.ZERO)
        
        val title = Translation.translate("maintenance.disconnect.title", locale)
        var subTitle = maintenance.customMessage?.let { "<red>${Translation.translate("maintenance.disconnect.messageFromServer", locale).format("<i>$it")}" }
        if (subTitle == null || lastDigit in 1..5) subTitle = Translation.translate("maintenance.disconnect.remaining${if (remainingDuration == null) ".generic" else ""}", locale).format("<yellow>$countdown")
        
        val message = "<red>$title\n<red>$subTitle"
        
        event.ping = event.ping.asBuilder().let { builder ->
            builder.version(ServerPing.Version(-1, "§4Maintenance"))
            builder.samplePlayers(ServerPing.SamplePlayer("§cMaintenance", UUID.randomUUID()))
            builder.description(MiniMessage.miniMessage().deserialize(message))
            return@let builder.build()
        }
    }
}