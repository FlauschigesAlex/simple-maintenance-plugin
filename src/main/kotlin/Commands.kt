package at.flauschigesalex.maintenance

import at.flauschigesalex.lib.base.time.countdown.Countdown
import at.flauschigesalex.lib.minecraft.brigadier.CommandBuilder
import at.flauschigesalex.lib.minecraft.brigadier.types.internal.GreedyArgumentType
import at.flauschigesalex.lib.minecraft.brigadier.types.internal.LiteralArgumentType
import at.flauschigesalex.lib.minecraft.brigadier.types.primitive.StringArgumentType
import at.flauschigesalex.maintenance.utils.*
import at.flauschigesalex.maintenance.utils.command.DurationArgumentType
import at.flauschigesalex.maintenance.utils.command.ServerArgumentType
import at.flauschigesalex.maintenance.utils.command.ServerData
import at.flauschigesalex.maintenance.utils.command.StatusArgumentType
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Duration
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

internal object Commands {
    init {
        CommandBuilder("maintenance") {
            this.permission(Permissions.MAINTENANCE_COMMAND)

            this.execute { context ->
                val sender = context.sender
                val activeMaintenances = SimpleMaintenanceConfig.maintenance
                    .filter { it.isEnabled }
                    .sortedBy { it.serverName }
                    .map { if (it.serverName == null) ServerData.proxyName else it.serverName }

                if (activeMaintenances.isEmpty()) {
                    val message = Translation.translate("maintenance.command.list.none", sender.locale)
                    return@execute sender.sendMiniMessage(message)
                }

                val message = Translation.translate("maintenance.command.list.current", sender.locale)
                    .format(activeMaintenances.size, activeMaintenances.joinToString("<dark_gray>, ") { "<red>$it" })
                sender.sendMiniMessage(message)
            }

            this.argument("server", ServerArgumentType) {
                this.argument("status", LiteralArgumentType.literal()) {
                    this.optional()
                    
                    this.argument("status", StatusArgumentType) {
                        this.execute { context ->
                            val sender = context.sender
                            val data = context.arguments
                            val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                            val status: Boolean = data.byType<Boolean>("status")?.value ?: false

                            val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                            if (maintenance.isEnabled == status) {
                                val statusTranslated = Translation.translate("maintenance.command.status.$status", sender.locale)
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.status.fail", sender.locale).format(statusTranslated, server.name)
                                return@execute sender.sendMiniMessage(message)
                            }

                            val newMaintenance = maintenance.copy(isEnabled = status)

                            SimpleMaintenanceConfig.modifyMaintenance(sender, newMaintenance)
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val statusTranslated = Translation.translate("maintenance.command.status.$status", permitted.locale)
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.status${if (permitted != sender) ".other" else ""}", permitted.locale).format(statusTranslated, server.name, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }
                    }
                    
                    this.execute { context ->
                        val sender = context.sender
                        val data = context.arguments
                        val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                        
                        val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                        val statusTranslated = Translation.translate("maintenance.command.status.${maintenance.isEnabled}", sender.locale)
                        
                        sender.sendMiniMessage(Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.status.current", sender.locale).format(server.name, statusTranslated))
                    }
                }

                this.argument("message", LiteralArgumentType.literal()) {
                    this.argument("message", GreedyArgumentType.greedy(StringArgumentType.string())) {
                        this.execute { context ->
                            val sender = context.sender
                            val data = context.arguments
                            val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                            val message: String? = data.greedyByType<String>("message")?.value?.joinToString(" ")

                            val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                            val newMaintenance = maintenance.copy(customMessage = message)

                            SimpleMaintenanceConfig.modifyMaintenance(sender, newMaintenance)
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.message.set${if (permitted != sender) ".other" else ""}", permitted.locale).format("<dark_gray>'<red><i>${message}<reset><dark_gray>'<gray>", server.name, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }
                    }

                    this.argument("clear", LiteralArgumentType.literal()) {
                        this.execute { context ->
                            val sender = context.sender
                            val data = context.arguments
                            val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute

                            val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                            val newMaintenance = maintenance.copy(customMessage = null)

                            SimpleMaintenanceConfig.modifyMaintenance(sender, newMaintenance)
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.message.unset${if (permitted != sender) ".other" else ""}", permitted.locale).format(server.name, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }
                    }
                    
                    this.execute { context ->
                        val sender = context.sender
                        val data = context.arguments
                        val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                        
                        val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                        if (maintenance.customMessage == null) {
                            val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.message.current.none", sender.locale).format(server.name)
                            sender.sendMiniMessage(message)
                            return@execute
                        }
                        
                        val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.message.current", sender.locale).format(server.name, "<dark_gray>'<red><i>${maintenance.customMessage}<reset><dark_gray>'<gray>")
                        sender.sendMiniMessage(message)
                    }
                }

                this.argument("duration", LiteralArgumentType.literal()) {
                    this.argument("duration", DurationArgumentType) {
                        this.execute { context ->
                            val sender = context.sender
                            val data = context.arguments
                            val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                            val duration: Duration = data.byType<Duration>("duration")?.value ?: return@execute

                            val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                            val newMaintenance = maintenance.copy(expireLong = Instant.now().plus(duration).toEpochMilli())

                            val displayDuration = Countdown.create(duration)
                            SimpleMaintenanceConfig.modifyMaintenance(sender, newMaintenance)
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.duration.set${if (permitted != sender) ".other" else ""}", permitted.locale).format(server.name, displayDuration, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }
                    }
                    this.argument("indefinite", LiteralArgumentType.literal()) {
                        this.execute { context ->
                            val sender = context.sender
                            val data = context.arguments
                            val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute

                            val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                            val newMaintenance = maintenance.copy(expireLong = null)

                            SimpleMaintenanceConfig.modifyMaintenance(sender, newMaintenance)
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.duration.unset${if (permitted != sender) ".other" else ""}", permitted.locale).format(server.name, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }
                    }
                    
                    this.execute { context ->
                        val sender = context.sender
                        val data = context.arguments
                        val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                        
                        val maintenance = SimpleMaintenanceConfig.findMaintenance(server)
                        if (maintenance.canExpire.not()) {
                            val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.duration.current.indefinite", sender.locale).format(server.name)
                            sender.sendMiniMessage(message)
                            return@execute
                        }
                        
                        val displayDuration = Countdown.create(maintenance.remaining ?: Duration.ZERO)
                        val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.duration.current", sender.locale).format(server.name, displayDuration)
                        sender.sendMiniMessage(message)
                    }
                }

                this.argument("enforce", LiteralArgumentType.literal()) {
                    this.execute { context ->
                        val sender = context.sender
                        val data = context.arguments

                        val server: ServerData = data.byType<ServerData>("server")?.value ?: return@execute
                        val maintenance = SimpleMaintenanceConfig.findMaintenance(server)

                        if (maintenance.isEnabled.not()) {
                            val message = Translation.translate("maintenance.command.enforce.fail", sender.locale)
                            return@execute sender.sendMiniMessage(message)
                        }

                        val toDisconnect = SimpleMaintenancePlugin.instance.server.allPlayers.toMutableList()
                        toDisconnect.removeIf { maintenance.serverName != it.currentServer.getOrNull()?.serverInfo?.name }
                        toDisconnect.removeAll(SimpleMaintenancePlugin.instance.bypassPermitted)

                        fun broadcastMessage() {
                            SimpleMaintenancePlugin.instance.commandPermitted.forEach { permitted ->
                                val message = Translation.translate("maintenance.command.${if (maintenance.isVelocity) "velocity" else "paper"}.enforce${if (permitted != sender) ".other" else ""}", permitted.locale).format(server.name, sender.username(permitted.locale))

                                permitted.sendMiniMessage(message)
                            }
                        }

                        fun Player.disconnect() {
                            this.disconnect(MiniMessage.miniMessage().deserialize(
                                SimpleMaintenancePlugin.instance.message(sender.locale, server.serverInfo, maintenance)
                            ))
                        }

                        if (maintenance.isVelocity) {
                            broadcastMessage()
                            return@execute toDisconnect.forEach { player -> player.disconnect() }
                        }

                        toDisconnect.forEach { player ->
                            val servers = SimpleMaintenancePlugin.instance.server.configuration.attemptConnectionOrder.mapNotNull {
                                SimpleMaintenancePlugin.instance.server.getServer(it).orElse(null)
                            }.filterNot { it.isMaintenance() }.filter { it.serverInfo.name.equals(server.name, true) }.toMutableList()

                            player.createConnectionRequests(servers) ?: return@forEach run {
                                player.disconnect()
                            }

                            player.disconnect()
                        }

                        broadcastMessage()
                    }
                }
            }
        }
    }
}
