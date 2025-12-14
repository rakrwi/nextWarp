package com.heulim.nextwarp.paper.command

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.common.data.TeleportRequest
import com.heulim.nextwarp.common.data.TeleportType
import com.heulim.nextwarp.paper.CooldownManager
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class WarpCommand(private val plugin: NextWarpPaper) : CommandExecutor, TabCompleter {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        // 쿨다운 체크
        val cooldownConfig = plugin.pluginConfig.cooldown.warp
        if (!sender.hasPermission(cooldownConfig.bypassPermission)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(
                sender.uniqueId,
                CooldownManager.WARP,
                cooldownConfig
            )
            if (remaining > 0) {
                sender.sendMessage(msg.cooldown.replacePlaceholders("seconds" to remaining))
                return true
            }
        }

        val warpName = args[0]
        val warp = plugin.databaseManager?.getWarp(warpName)

        if (warp == null) {
            sender.sendMessage(msg.warp.notFound.replacePlaceholders("name" to warpName))
            return true
        }

        // 쿨다운 설정
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownManager.WARP)

        // 같은 서버인 경우 바로 텔레포트
        if (warp.server == plugin.pluginConfig.serverName) {
            val world = Bukkit.getWorld(warp.world)
            if (world != null) {
                val location = Location(
                    world,
                    warp.x,
                    warp.y,
                    warp.z,
                    warp.yaw,
                    warp.pitch
                )
                sender.teleportAsync(location).thenAccept { success ->
                    if (success) {
                        sender.sendMessage(msg.warp.teleported)
                    }
                }
            } else {
                sender.sendMessage(msg.worldNotFound)
            }
        } else {
            // 다른 서버인 경우
            sender.sendMessage(msg.warp.teleporting.replacePlaceholders("name" to warpName))

            // Redis로 텔레포트 요청 전송
            val request = TeleportRequest(
                playerUuid = sender.uniqueId.toString(),
                playerName = sender.name,
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
            plugin.redisManager?.publishTeleportRequest(request)

            // Velocity에 서버 이동 요청
            plugin.redisManager?.publishServerTransfer(sender.uniqueId.toString(), warp.server)
        }

        return true
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(msg.warp.helpHeader)
        player.sendMessage(msg.warp.helpWarp)
        player.sendMessage(msg.warp.helpSet)
        player.sendMessage(msg.warp.helpDelete)
        player.sendMessage(msg.warp.helpList)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val warps = plugin.databaseManager?.getAllWarps() ?: emptyList()
            return warps.map { it.name }.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
