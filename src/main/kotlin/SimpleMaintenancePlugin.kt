package at.flauschigesalex.maintenance

import at.flauschigesalex.lib.base.time.countdown.Countdown
import at.flauschigesalex.lib.minecraft.velocity.base.FlauschigeLibraryVelocity
import at.flauschigesalex.lib.minecraft.velocity.base.InternalPluginData
import at.flauschigesalex.maintenance.utils.Permissions
import at.flauschigesalex.maintenance.utils.Translation
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.Scheduler
import org.bstats.charts.CustomChart
import org.bstats.charts.SimplePie
import org.bstats.velocity.Metrics
import java.time.Duration
import java.util.*
import java.util.logging.Logger

@Suppress("unused", "UNUSED_EXPRESSION")
@Plugin(id = "simple-maintenance-plugin")
class SimpleMaintenancePlugin @Inject constructor(val server: ProxyServer,
                                                  internal val logger: Logger,
                                                  private val bStats: Metrics.Factory
) {
    
    companion object {
        internal lateinit var instance: SimpleMaintenancePlugin
            private set
    }
    
    lateinit var info: InternalPluginData
        private set
    
    @Subscribe
    private fun onInitialize(event: ProxyInitializeEvent) {
        instance = this
        info = FlauschigeLibraryVelocity.init(this, server, javaClass.packageName)

        Commands // REGISTER COMMANDS
        val metrics = bStats.make(this, 30818)
        
        metrics.addCustomChart(SimplePie("server_brand") { server.version.name })
        metrics.addCustomChart(SimplePie("server_version") { server.version.version })
    }

    internal fun schedule(consumer: ProxyServer.(ScheduledTask) -> Unit): Scheduler.TaskBuilder =
        server.scheduler.buildTask(this) { it ->
            consumer.invoke(server, it)
        }
    
    internal val commandPermitted: List<Player> get() = server.allPlayers.filter { it.hasPermission(Permissions.MAINTENANCE_COMMAND) }
    internal val bypassPermitted: List<Player> get() = server.allPlayers.filter { it.hasPermission(Permissions.MAINTENANCE_BYPASS) }

    internal fun message(locale: Locale, server: ServerInfo?, maintenance: MaintenanceData): String {
        var title = Translation.translate("maintenance.disconnect.title", locale)
        if (server != null) title = Translation.translate("maintenance.disconnect.title.paper", locale).format(server.name)

        val remainingDuration = maintenance.remaining
        val countdown = Countdown.create(remainingDuration ?: Duration.ZERO)
        val remaining = Translation.translate("maintenance.disconnect.remaining", locale).format("<yellow>$countdown")

        val customMessage = maintenance.customMessage?.let { "<red>${Translation.translate("maintenance.disconnect.messageFromServer", locale).format("<i>$it<reset>")}" }

        var message = "<red>$title"
        if (remainingDuration != null) message += "\n<red>$remaining"
        if (customMessage != null) message += "\n<red>$customMessage"

        return message
    }
}