package com.heulim.nextwarp.paper.listener

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.common.data.TeleportRequest
import com.heulim.nextwarp.common.data.TeleportType
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent

class PlayerListener(private val plugin: NextWarpPaper) : Listener {

    private val msg get() = plugin.messages

    // 대소문자 구분 없이 플레이어 검색
    private fun getPlayerIgnoreCase(name: String): org.bukkit.entity.Player? {
        return Bukkit.getOnlinePlayers().firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    }

    /**
     * PlayerSpawnLocationEvent - 플레이어가 서버에 접속할 때 스폰 위치를 설정
     * PlayerJoinEvent보다 먼저 실행되어 딜레이 없이 즉시 워프 위치에 스폰됨
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerSpawnLocation(event: PlayerSpawnLocationEvent) {
        val player = event.player
        val pendingRequest = plugin.pendingTeleports[player.uniqueId] ?: return

        plugin.logger.info("[TPA-DEBUG] PlayerSpawnLocationEvent: player=${player.name}, type=${pendingRequest.type}, tpaType=${pendingRequest.tpaType}")

        when (pendingRequest.type) {
            TeleportType.WARP -> {
                if (pendingRequest.world != null && pendingRequest.x != null &&
                    pendingRequest.y != null && pendingRequest.z != null) {
                    val world = Bukkit.getWorld(pendingRequest.world)
                    if (world != null) {
                        val location = Location(
                            world,
                            pendingRequest.x,
                            pendingRequest.y,
                            pendingRequest.z,
                            pendingRequest.yaw ?: 0f,
                            pendingRequest.pitch ?: 0f
                        )
                        event.spawnLocation = location
                        // 처리 완료 표시 (PlayerJoinEvent에서 메시지만 보내도록)
                        plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(world = null)
                    }
                }
            }
            TeleportType.RANDOM_TP -> {
                val rtpConfig = plugin.pluginConfig.randomTp
                val world = if (pendingRequest.world != null) {
                    Bukkit.getWorld(pendingRequest.world)
                } else {
                    Bukkit.getWorld(rtpConfig.defaultWorld)
                }

                if (world != null) {
                    // 좌표가 없으면 랜덤 생성 (크로스서버 RTP)
                    val x = pendingRequest.x?.toInt() ?: kotlin.random.Random.nextInt(rtpConfig.minX, rtpConfig.maxX)
                    val z = pendingRequest.z?.toInt() ?: kotlin.random.Random.nextInt(rtpConfig.minZ, rtpConfig.maxZ)

                    // 동기적으로 청크 로드 (접속 시에만 사용)
                    world.getChunkAt(x shr 4, z shr 4).load(true)

                    val safeY = findSafeY(world, x, z)
                    if (safeY != null) {
                        val location = Location(world, x.toDouble() + 0.5, safeY.toDouble(), z.toDouble() + 0.5)
                        event.spawnLocation = location
                        // 처리 완료 표시
                        plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(world = null, x = x.toDouble(), z = z.toDouble(), y = safeY.toDouble())
                    }
                }
            }
            TeleportType.TPA -> {
                // WARP와 동일한 패턴: 좌표가 있으면 설정
                if (pendingRequest.world != null && pendingRequest.x != null &&
                    pendingRequest.y != null && pendingRequest.z != null) {
                    val world = Bukkit.getWorld(pendingRequest.world)
                    if (world != null) {
                        val location = Location(
                            world,
                            pendingRequest.x,
                            pendingRequest.y,
                            pendingRequest.z,
                            pendingRequest.yaw ?: 0f,
                            pendingRequest.pitch ?: 0f
                        )
                        plugin.logger.info("[TPA-DEBUG] TPA 스폰 위치 설정: ${world.name} (${location.x}, ${location.y}, ${location.z})")
                        event.spawnLocation = location
                        // 처리 완료 표시 (PlayerJoinEvent에서 메시지만 보내도록)
                        plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(world = null)
                    } else {
                        plugin.logger.warning("[TPA-DEBUG] 월드를 찾을 수 없음: ${pendingRequest.world}")
                    }
                } else if (!pendingRequest.tpaType.isNullOrEmpty()) {
                    // tpaType이 있는 경우 (하위 호환성)
                    when (pendingRequest.tpaType) {
                        "do_teleport" -> {
                            plugin.logger.info("[TPA-DEBUG] do_teleport 스폰 처리 (deprecated): player=${player.name}, targetPlayer=${pendingRequest.targetPlayerName}")
                            if (pendingRequest.targetPlayerName != null) {
                                val targetPlayer = getPlayerIgnoreCase(pendingRequest.targetPlayerName)
                                if (targetPlayer != null) {
                                    event.spawnLocation = targetPlayer.location
                                    plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(tpaType = "do_teleport_done")
                                }
                            }
                        }
                        "do_teleport_coords" -> {
                            plugin.logger.info("[TPA-DEBUG] do_teleport_coords 스폰 처리 (deprecated): player=${player.name}")
                            if (pendingRequest.world != null && pendingRequest.x != null &&
                                pendingRequest.y != null && pendingRequest.z != null) {
                                val world = Bukkit.getWorld(pendingRequest.world)
                                if (world != null) {
                                    val location = Location(
                                        world,
                                        pendingRequest.x,
                                        pendingRequest.y,
                                        pendingRequest.z,
                                        pendingRequest.yaw ?: 0f,
                                        pendingRequest.pitch ?: 0f
                                    )
                                    event.spawnLocation = location
                                    plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(tpaType = "do_teleport_done")
                                }
                            }
                        }
                    }
                }
            }
            TeleportType.SPAWN -> {
                if (pendingRequest.world != null && pendingRequest.x != null &&
                    pendingRequest.y != null && pendingRequest.z != null) {
                    val world = Bukkit.getWorld(pendingRequest.world)
                    if (world != null) {
                        val location = Location(
                            world,
                            pendingRequest.x,
                            pendingRequest.y,
                            pendingRequest.z,
                            pendingRequest.yaw ?: 0f,
                            pendingRequest.pitch ?: 0f
                        )
                        event.spawnLocation = location
                        plugin.pendingTeleports[player.uniqueId] = pendingRequest.copy(world = null)
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val pendingRequest = plugin.pendingTeleports.remove(player.uniqueId) ?: return

        plugin.logger.info("[TPA-DEBUG] PlayerJoinEvent: player=${player.name}, type=${pendingRequest.type}, tpaType=${pendingRequest.tpaType}")

        when (pendingRequest.type) {
            TeleportType.WARP -> {
                // world가 null이면 이미 PlayerSpawnLocationEvent에서 처리됨
                if (pendingRequest.world == null) {
                    player.sendMessage(msg.warp.teleported)
                }
            }
            TeleportType.RANDOM_TP -> {
                if (pendingRequest.world == null) {
                    player.sendMessage(msg.randomTp.completed)
                    player.sendMessage(msg.randomTp.location.replacePlaceholders(
                        "world" to player.world.name,
                        "x" to (pendingRequest.x?.toInt() ?: 0),
                        "y" to (pendingRequest.y?.toInt() ?: 0),
                        "z" to (pendingRequest.z?.toInt() ?: 0)
                    ))
                } else {
                    player.sendMessage(msg.randomTp.noSafeLocation)
                }
            }
            TeleportType.TPA -> {
                // world가 null이면 이미 PlayerSpawnLocationEvent에서 처리됨 (WARP와 동일한 패턴)
                if (pendingRequest.world == null || pendingRequest.tpaType == "do_teleport_done") {
                    plugin.logger.info("[TPA-DEBUG] TPA 텔레포트 완료 메시지 전송: ${player.name}")
                    player.sendMessage(msg.tpa.teleported)
                }
            }
            TeleportType.SPAWN -> {
                if (pendingRequest.world == null) {
                    player.sendMessage(msg.spawn.teleported)
                }
            }
        }
    }

    /**
     * PlayerRespawnEvent - 플레이어 사망 후 리스폰 시 스폰 위치로 이동
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        // 스폰 정보 조회
        val spawn = plugin.databaseManager?.getSpawn() ?: return

        // 같은 서버인 경우 바로 리스폰 위치 설정
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
                event.respawnLocation = location
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    player.sendMessage(msg.spawn.respawn)
                }, 1L)
            }
        } else {
            // 다른 서버인 경우 Redis를 통해 텔레포트 요청 전송 후 서버 이동
            val request = TeleportRequest(
                playerUuid = player.uniqueId.toString(),
                playerName = player.name,
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
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                plugin.redisManager?.publishServerTransfer(player.uniqueId.toString(), spawn.server)
            }, 1L)
        }
    }

    private fun findSafeY(world: org.bukkit.World, x: Int, z: Int): Int? {
        val highestY = world.getHighestBlockYAt(x, z)

        for (y in highestY downTo world.minHeight) {
            val block = world.getBlockAt(x, y, z)
            val above1 = world.getBlockAt(x, y + 1, z)
            val above2 = world.getBlockAt(x, y + 2, z)

            if (block.type.isSolid && !block.isLiquid &&
                above1.type.isAir && above2.type.isAir) {
                return y + 1
            }
        }

        return null
    }
}