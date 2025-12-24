package dev.rakrwi.nextwarp.paper

import dev.rakrwi.nextwarp.api.NextWarpAPI
import dev.rakrwi.nextwarp.api.WarpInfo
import dev.rakrwi.nextwarp.common.data.TeleportRequest
import dev.rakrwi.nextwarp.common.data.TeleportType
import dev.rakrwi.nextwarp.common.data.WarpLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.random.Random

class NextWarpAPIImpl(private val plugin: NextWarpPaper) : NextWarpAPI {

    // ==================== 워프 관련 ====================

    override fun warpPlayer(player: Player, warpName: String): Boolean {
        val warp = plugin.databaseManager?.getWarp(warpName) ?: return false

        val request = TeleportRequest(
            playerUuid = player.uniqueId.toString(),
            playerName = player.name,
            type = TeleportType.WARP,
            targetServer = warp.server,
            world = warp.world,
            x = warp.x,
            y = warp.y,
            z = warp.z,
            yaw = warp.yaw,
            pitch = warp.pitch,
            warpName = warpName
        )

        // 같은 서버면 직접 텔레포트
        if (warp.server == plugin.pluginConfig.serverName) {
            val world = Bukkit.getWorld(warp.world) ?: return false
            val location = Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch)
            player.teleportAsync(location)
        } else {
            // 다른 서버면 Redis로 요청 후 서버 이동
            plugin.redisManager?.publishTeleportRequest(request)
            sendToServer(player, warp.server)
        }

        return true
    }

    override fun createWarp(warpName: String, location: Location): Boolean {
        val warp = WarpLocation(
            name = warpName,
            server = plugin.pluginConfig.serverName,
            world = location.world.name,
            x = location.x,
            y = location.y,
            z = location.z,
            yaw = location.yaw,
            pitch = location.pitch
        )
        plugin.databaseManager?.saveWarp(warp)
        return true
    }

    override fun deleteWarp(warpName: String): Boolean {
        return plugin.databaseManager?.deleteWarp(warpName) ?: false
    }

    override fun warpExists(warpName: String): Boolean {
        return plugin.databaseManager?.getWarp(warpName) != null
    }

    override fun getWarp(warpName: String): WarpInfo? {
        val warp = plugin.databaseManager?.getWarp(warpName) ?: return null
        return WarpInfo(warp.name, warp.server, warp.world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch)
    }

    override fun getAllWarps(): List<WarpInfo> {
        return plugin.databaseManager?.getAllWarps()?.map {
            WarpInfo(it.name, it.server, it.world, it.x, it.y, it.z, it.yaw, it.pitch)
        } ?: emptyList()
    }

    override fun getWarps(serverName: String): List<WarpInfo> {
        return getAllWarps().filter { it.server == serverName }
    }

    // ==================== 랜덤 텔레포트 관련 ====================

    override fun randomTeleport(player: Player): Boolean {
        return randomTeleport(player, plugin.pluginConfig.serverName)
    }

    override fun randomTeleport(player: Player, serverName: String): Boolean {
        return randomTeleport(player, serverName, plugin.pluginConfig.randomTp.defaultWorld)
    }

    override fun randomTeleport(player: Player, serverName: String, worldName: String): Boolean {
        val rtpConfig = plugin.pluginConfig.randomTp
        val randomX = Random.nextInt(rtpConfig.minX, rtpConfig.maxX)
        val randomZ = Random.nextInt(rtpConfig.minZ, rtpConfig.maxZ)

        val request = TeleportRequest(
            playerUuid = player.uniqueId.toString(),
            playerName = player.name,
            type = TeleportType.RANDOM_TP,
            targetServer = serverName,
            world = worldName,
            x = randomX.toDouble(),
            y = null,
            z = randomZ.toDouble(),
            yaw = 0f,
            pitch = 0f
        )

        // 같은 서버면 직접 처리
        if (serverName == plugin.pluginConfig.serverName) {
            plugin.redisManager?.publishTeleportRequest(request)
        } else {
            // 다른 서버면 Redis로 요청 후 서버 이동
            plugin.redisManager?.publishTeleportRequest(request)
            sendToServer(player, serverName)
        }

        return true
    }

    // ==================== 유틸리티 ====================

    override fun getServerName(): String {
        return plugin.pluginConfig.serverName
    }

    private fun sendToServer(player: Player, serverName: String) {
        val out = java.io.ByteArrayOutputStream()
        val dataOut = java.io.DataOutputStream(out)
        dataOut.writeUTF("Connect")
        dataOut.writeUTF(serverName)
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }
}