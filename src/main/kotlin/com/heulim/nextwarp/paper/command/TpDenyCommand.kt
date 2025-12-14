package com.heulim.nextwarp.paper.command

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.common.data.TeleportRequest
import com.heulim.nextwarp.common.data.TeleportType
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TpDenyCommand(private val plugin: NextWarpPaper) : CommandExecutor {

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

        val (requesterName, _) = pendingRequest
        plugin.removeTpaRequest(sender.name)

        // Redis에서 요청자 정보 조회하여 거절 알림 전송
        val onlinePlayers = plugin.redisManager?.getOnlinePlayers() ?: emptyMap()
        val requesterEntry = onlinePlayers.entries.firstOrNull { it.key.equals(requesterName, ignoreCase = true) }

        if (requesterEntry != null) {
            val requesterServer = requesterEntry.value
            val request = TeleportRequest(
                playerUuid = "",
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
                tpaType = "deny"
            )
            plugin.redisManager?.publishTeleportRequest(request)
        }

        sender.sendMessage(msg.tpa.denied.replacePlaceholders("player" to requesterName))
        return true
    }
}
