package com.heulim.nextwarp.paper.command

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.common.data.TeleportRequest
import com.heulim.nextwarp.common.data.TeleportType
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TpAcceptCommand(private val plugin: NextWarpPaper) : CommandExecutor {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        val pendingRequest = plugin.getPendingTpaRequest(sender.name)
        if (pendingRequest == null) {
            sender.sendMessage(msg.tpa.noPending)
            return true
        }

        val requesterName = pendingRequest.requesterName
        val requesterUuid = pendingRequest.requesterUuid
        val isHereRequest = pendingRequest.isHereRequest
        plugin.removeTpaRequest(sender.name)

        // Redis에서 요청자 정보 조회
        val onlinePlayers = plugin.redisManager?.getOnlinePlayers() ?: emptyMap()
        val requesterEntry = onlinePlayers.entries.firstOrNull { it.key.equals(requesterName, ignoreCase = true) }

        if (requesterEntry == null) {
            sender.sendMessage(msg.tpa.requesterOffline.replacePlaceholders("player" to requesterName))
            return true
        }

        val requesterServer = requesterEntry.value
        val currentServer = plugin.pluginConfig.serverName

        if (isHereRequest) {
            // TPA Here: 수락자가 요청자에게 이동
            val request = TeleportRequest(
                playerUuid = sender.uniqueId.toString(),
                playerName = requesterName,
                type = TeleportType.TPA,
                targetServer = requesterServer,
                world = null,
                x = null,
                y = null,
                z = null,
                yaw = null,
                pitch = null,
                targetPlayerName = sender.name,
                targetPlayerUuid = sender.uniqueId.toString(),
                targetPlayerServer = currentServer,
                tpaType = "accept_here_get_location"
            )
            plugin.redisManager?.publishTeleportRequest(request)

            // 다른 서버면 Velocity에 서버 이동 요청
            if (currentServer != requesterServer) {
                plugin.redisManager?.publishServerTransfer(sender.uniqueId.toString(), requesterServer)
            }
        } else {
            // TPA: 요청자가 수락자에게 이동
            val request = TeleportRequest(
                playerUuid = sender.uniqueId.toString(),
                playerName = sender.name,
                type = TeleportType.TPA,
                targetServer = currentServer,
                world = null,
                x = null,
                y = null,
                z = null,
                yaw = null,
                pitch = null,
                targetPlayerName = requesterName,
                targetPlayerUuid = requesterUuid,
                targetPlayerServer = requesterServer,
                tpaType = "accept_get_location"
            )
            plugin.redisManager?.publishTeleportRequest(request)
        }

        sender.sendMessage(msg.tpa.accepted.replacePlaceholders("player" to requesterName))
        return true
    }
}
