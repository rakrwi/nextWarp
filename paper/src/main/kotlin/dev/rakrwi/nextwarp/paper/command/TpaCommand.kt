package dev.rakrwi.nextwarp.paper.command

import dev.rakrwi.nextwarp.common.config.replacePlaceholders
import dev.rakrwi.nextwarp.common.data.TeleportRequest
import dev.rakrwi.nextwarp.common.data.TeleportType
import dev.rakrwi.nextwarp.paper.CooldownManager
import dev.rakrwi.nextwarp.paper.NextWarpPaper
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaCommand(private val plugin: NextWarpPaper) : CommandExecutor, TabCompleter {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(msg.tpa.usage)
            return true
        }

        // 쿨다운 체크
        val cooldownConfig = plugin.pluginConfig.cooldown.tpa
        if (!sender.hasPermission(cooldownConfig.bypassPermission)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(
                sender.uniqueId,
                CooldownManager.TPA,
                cooldownConfig
            )
            if (remaining > 0) {
                sender.sendMessage(msg.cooldown.replacePlaceholders("seconds" to remaining))
                return true
            }
        }

        val targetName = args[0]

        if (targetName.equals(sender.name, ignoreCase = true)) {
            sender.sendMessage(msg.tpa.cannotSelf)
            return true
        }

        // Redis에서 플레이어 정보 조회
        val onlinePlayers = plugin.redisManager?.getOnlinePlayers() ?: emptyMap()
        val targetEntry = onlinePlayers.entries.firstOrNull { it.key.equals(targetName, ignoreCase = true) }

        if (targetEntry == null) {
            sender.sendMessage(msg.tpa.playerNotFound.replacePlaceholders("player" to targetName))
            return true
        }

        val exactTargetName = targetEntry.key
        val targetServer = targetEntry.value

        // TPA 요청 전송
        val request = TeleportRequest(
            playerUuid = sender.uniqueId.toString(),
            playerName = sender.name,
            type = TeleportType.TPA,
            targetServer = targetServer,
            world = null,
            x = null,
            y = null,
            z = null,
            yaw = null,
            pitch = null,
            targetPlayerName = exactTargetName,
            tpaType = "request"
        )

        // Redis로 요청 전송 (대상 서버에서 저장됨)
        plugin.redisManager?.publishTeleportRequest(request)

        // 쿨다운 설정
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownManager.TPA)

        sender.sendMessage(msg.tpa.sent.replacePlaceholders("player" to exactTargetName))
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val onlinePlayers = plugin.redisManager?.getOnlinePlayers()?.keys ?: emptySet()
            return onlinePlayers.filter {
                it.lowercase().startsWith(args[0].lowercase()) &&
                !it.equals((sender as? Player)?.name, ignoreCase = true)
            }.toList()
        }
        return emptyList()
    }
}
