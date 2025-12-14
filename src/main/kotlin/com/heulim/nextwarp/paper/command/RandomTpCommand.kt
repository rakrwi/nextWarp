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
import kotlin.random.Random

class RandomTpCommand(private val plugin: NextWarpPaper) : CommandExecutor, TabCompleter {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        if (!sender.hasPermission("nextwarp.randomtp.use") && !sender.hasPermission("nextwarp.admin") && !sender.isOp) {
            sender.sendMessage(msg.noPermission)
            return true
        }

        // 쿨다운 체크
        val cooldownConfig = plugin.pluginConfig.cooldown.randomTp
        if (!sender.hasPermission(cooldownConfig.bypassPermission)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(
                sender.uniqueId,
                CooldownManager.RANDOM_TP,
                cooldownConfig
            )
            if (remaining > 0) {
                sender.sendMessage(msg.cooldown.replacePlaceholders("seconds" to remaining))
                return true
            }
        }

        val rtpConfig = plugin.pluginConfig.randomTp
        val currentServer = plugin.pluginConfig.serverName

        // 서버 지정 (없으면 현재 서버)
        val targetServer = if (args.isNotEmpty()) {
            args[0]
        } else {
            currentServer
        }

        // 쿨다운 설정
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownManager.RANDOM_TP)

        // 다른 서버인 경우
        if (targetServer != currentServer) {
            sender.sendMessage(msg.randomTp.teleporting)

            // Redis로 RTP 요청 전송
            val request = TeleportRequest(
                playerUuid = sender.uniqueId.toString(),
                playerName = sender.name,
                type = TeleportType.RANDOM_TP,
                targetServer = targetServer,
                world = null,
                x = null,
                y = null,
                z = null,
                yaw = null,
                pitch = null
            )
            plugin.redisManager?.publishTeleportRequest(request)

            // Velocity에 서버 이동 요청
            plugin.redisManager?.publishServerTransfer(sender.uniqueId.toString(), targetServer)
            return true
        }

        // 현재 서버에서 RTP
        val world = Bukkit.getWorld(rtpConfig.defaultWorld)
        if (world == null) {
            sender.sendMessage(msg.worldNotFound)
            return true
        }

        val randomX = Random.nextInt(rtpConfig.minX, rtpConfig.maxX)
        val randomZ = Random.nextInt(rtpConfig.minZ, rtpConfig.maxZ)

        sender.sendMessage(msg.randomTp.teleporting)
        sender.sendMessage(msg.randomTp.coordinates.replacePlaceholders("x" to randomX, "z" to randomZ))
        sender.sendMessage(msg.randomTp.searching)

        // 비동기로 청크 로드 후 안전한 위치 찾기
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            world.getChunkAtAsync(randomX shr 4, randomZ shr 4).thenAccept { chunk ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val safeY = findSafeY(world, randomX, randomZ)
                    if (safeY != null) {
                        val location = Location(world, randomX.toDouble() + 0.5, safeY.toDouble(), randomZ.toDouble() + 0.5)
                        sender.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                sender.sendMessage(msg.randomTp.completed)
                                sender.sendMessage(msg.randomTp.location.replacePlaceholders(
                                    "world" to world.name,
                                    "x" to randomX,
                                    "y" to safeY,
                                    "z" to randomZ
                                ))
                            }
                        }
                    } else {
                        sender.sendMessage(msg.randomTp.noSafeLocation)
                    }
                })
            }
        })

        return true
    }

    private fun findSafeY(world: org.bukkit.World, x: Int, z: Int): Int? {
        val highestY = world.getHighestBlockYAt(x, z)
        val safeBlockNames = plugin.pluginConfig.randomTp.safeBlocks.map { it.uppercase() }.toSet()

        for (y in highestY downTo world.minHeight) {
            val block = world.getBlockAt(x, y, z)
            val above1 = world.getBlockAt(x, y + 1, z)
            val above2 = world.getBlockAt(x, y + 2, z)

            val blockTypeName = block.type.name
            if (safeBlockNames.contains(blockTypeName) &&
                !block.isLiquid &&
                above1.type.isAir && above2.type.isAir) {
                return y + 1
            }
        }

        return null
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            // Redis에서 프록시의 서버 목록 가져오기
            val servers = plugin.redisManager?.getServers() ?: emptySet()
            return servers.filter { it.lowercase().startsWith(args[0].lowercase()) }.toList()
        }
        return emptyList()
    }
}
