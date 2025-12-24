package dev.rakrwi.nextwarp.paper.command

import dev.rakrwi.nextwarp.common.config.replacePlaceholders
import dev.rakrwi.nextwarp.common.data.TeleportRequest
import dev.rakrwi.nextwarp.common.data.TeleportType
import dev.rakrwi.nextwarp.paper.CooldownManager
import dev.rakrwi.nextwarp.paper.NextWarpPaper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SpawnCommand(private val plugin: NextWarpPaper) : CommandExecutor {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        // 쿨다운 체크
        val cooldownConfig = plugin.pluginConfig.cooldown.spawn
        if (!sender.hasPermission(cooldownConfig.bypassPermission)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(
                sender.uniqueId,
                CooldownManager.SPAWN,
                cooldownConfig
            )
            if (remaining > 0) {
                sender.sendMessage(msg.cooldown.replacePlaceholders("seconds" to remaining))
                return true
            }
        }

        val spawn = plugin.databaseManager?.getSpawn()
        if (spawn == null) {
            sender.sendMessage(msg.spawn.notSet)
            return true
        }

        // 쿨다운 설정
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownManager.SPAWN)

        // 같은 서버인 경우 바로 텔레포트
        if (spawn.server == plugin.pluginConfig.serverName) {
            val world = Bukkit.getWorld(spawn.world)
            if (world != null) {
                val location = Location(
                    world,
                    spawn.x,
                    spawn.y,
                    spawn.z,
                    spawn.yaw,
                    spawn.pitch
                )
                sender.teleportAsync(location).thenAccept { success ->
                    if (success) {
                        sender.sendMessage(msg.spawn.teleported)
                    }
                }
            } else {
                sender.sendMessage(msg.worldNotFound)
            }
        } else {
            // 다른 서버인 경우
            sender.sendMessage(msg.spawn.teleporting)

            // Redis로 텔레포트 요청 전송
            val request = TeleportRequest(
                playerUuid = sender.uniqueId.toString(),
                playerName = sender.name,
                type = TeleportType.SPAWN,
                targetServer = spawn.server,
                world = spawn.world,
                x = spawn.x,
                y = spawn.y,
                z = spawn.z,
                yaw = spawn.yaw,
                pitch = spawn.pitch
            )
            plugin.redisManager?.publishTeleportRequest(request)

            // Velocity에 서버 이동 요청
            plugin.redisManager?.publishServerTransfer(sender.uniqueId.toString(), spawn.server)
        }

        return true
    }
}
