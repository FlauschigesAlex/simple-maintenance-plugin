@file:Suppress("unused")

package at.flauschigesalex.maintenance

import at.flauschigesalex.lib.base.file.FileManager
import at.flauschigesalex.lib.base.file.JsonManager
import at.flauschigesalex.lib.base.file.readJson
import at.flauschigesalex.maintenance.utils.command.ServerData
import at.flauschigesalex.maintenance.utils.scheduleAsync
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import net.kyori.adventure.audience.Audience
import java.time.Duration
import java.time.Instant
import java.util.*

object SimpleMaintenanceConfig {
    private val file = FileManager(SimpleMaintenancePlugin.instance.info.dataFolder, "config.json").apply { 
        this.createFile()
    }
    private var json: JsonManager = file.readJson() ?: JsonManager()
    
    var maintenance: Set<MaintenanceData> = json.getJsonList("data").mapNotNull { MaintenanceData(it) }.toSet()
        get() {
            field = field.filterNot { it.isExpired }.toSet()
            json["data"] = field.map { it.toJson() }
            return field
        }
        private set(value) {
            field = value
            json["data"] = field.map { it.toJson() }
        }
    
    var defaultLocale: Locale = json.getString("locale.default")?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
        private set (value) {
            field = value
            json["locale.default"] = value.toLanguageTag()
            saveConfig(true)
        }
    
    fun findMaintenance(server: ServerData): MaintenanceData =
        maintenance.find { it.serverName.equals(server.serverInfo?.name, true) } ?: MaintenanceData.default(server)
    
    fun modifyMaintenance(sender: Audience, data: MaintenanceData) {
        maintenance -= data
        maintenance += data
        saveConfig(true)
    }
    
    internal fun saveConfig(async: Boolean) {
        if (async) scheduleAsync { saveConfig(false) }
        
        file.createFile()
        file.write(json)
    }
}

@Serializable
data class MaintenanceData(val serverName: String?, val customMessage: String?, val expireLong: Long?, var isEnabled: Boolean) {
    companion object {
        internal operator fun invoke(json: JsonManager?): MaintenanceData? {
            json ?: return null
            
            val serverName = json.getString("serverName")
            val message = json.getString("customMessage")
            val expire = json.getLong("expireLong")
            val isEnabled = json.getBoolean("isEnabled") ?: false
            
            return MaintenanceData(serverName, message, expire, isEnabled)
        }
        internal fun default(data: ServerData) = MaintenanceData(data, null, null, false)
    }
    
    internal constructor(serverData: ServerData, customMessage: String?, expire: Long?, isEnabled: Boolean):
            this(serverData.serverInfo?.name, customMessage, expire, isEnabled)
    
    @Transient
    val expire: Instant? = expireLong?.let { Instant.ofEpochMilli(it) }
    
    val isVelocity = serverName == null
    val canExpire get() = expire != null
    val isExpired get() = expire != null && Instant.now().isAfter(expire)
    
    val remaining: Duration? get() = expire?.let { Duration.between(Instant.now(), it) }

    override fun equals(other: Any?): Boolean = other is MaintenanceData && serverName == other.serverName
    override fun hashCode(): Int = serverName.hashCode()
    
    fun toJson(): JsonManager = Json.encodeToString(this).let { JsonManager(it)!! }
}