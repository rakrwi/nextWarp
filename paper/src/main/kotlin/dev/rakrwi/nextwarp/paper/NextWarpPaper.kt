package dev.rakrwi.nextwarp.paper

import dev.rakrwi.nextwarp.api.NextWarpAPI
import dev.rakrwi.nextwarp.common.config.ConfigLoader
import dev.rakrwi.nextwarp.common.config.MessageConfig
import dev.rakrwi.nextwarp.common.config.MessageLoader
import dev.rakrwi.nextwarp.common.config.PluginConfig
import dev.rakrwi.nextwarp.common.config.replacePlaceholders
import dev.rakrwi.nextwarp.common.data.TeleportRequest
import dev.rakrwi.nextwarp.common.data.TeleportType
import dev.rakrwi.nextwarp.common.data.WarpLocation
import dev.rakrwi.nextwarp.common.database.DatabaseManager
import dev.rakrwi.nextwarp.common.redis.RedisManager
import dev.rakrwi.nextwarp.paper.command.*
import dev.rakrwi.nextwarp.paper.listener.PlayerListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import redis.clients.jedis.JedisPubSub
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NextWarpPaper : JavaPlugin() {
    lateinit var pluginConfig: PluginConfig
    lateinit var messages: MessageConfig
    var databaseManager: DatabaseManager? = null
    var redisManager: RedisManager? = null
    private var pubSub: JedisPubSub? = null
    val cooldownManager = CooldownManager()

    // 서버 이동 후 텔레포트 대기 중인 플레이어
    val pendingTeleports = ConcurrentHashMap<UUID, TeleportRequest>()

    // TPA 요청 저장: targetPlayer -> TpaRequestData
    data class TpaRequestData(
        val requesterName: String,
        val requesterUuid: String,
        val isHereRequest: Boolean,
        val expireTime: Long
    )
    private val pendingTpaRequests = ConcurrentHashMap<String, TpaRequestData>()

    override fun onEnable() {
        loadConfig()
        loadMessages()
        try {
            initializeManagers()
            subscribeToRedis()
            registerListeners()
            registerChannels()
            registerCommands()
            registerAPI()
            startTpaExpirationTask()
            logger.info("NextWarp Paper has been enabled!")
        } catch (e: Exception) {
            logger.severe("Failed to initialize NextWarp: ${e.message}")
            logger.severe("Please check your config.yml settings (MySQL/Redis)")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        pubSub?.unsubscribe()
        redisManager?.close()
        databaseManager?.close()
        logger.info("NextWarp Paper has been disabled!")
    }

    private fun loadConfig() {
        pluginConfig = ConfigLoader.loadConfig(dataFolder, logger)
    }

    private fun loadMessages() {
        messages = MessageLoader.loadMessages(dataFolder, logger)
    }

    private fun initializeManagers() {
        databaseManager = DatabaseManager(
            host = pluginConfig.mysql.host,
            port = pluginConfig.mysql.port,
            database = pluginConfig.mysql.database,
            username = pluginConfig.mysql.username,
            password = pluginConfig.mysql.password
        )
        logger.info("Connected to MySQL database")

        redisManager = RedisManager(
            host = pluginConfig.redis.host,
            port = pluginConfig.redis.port,
            password = pluginConfig.redis.password.ifEmpty { null }
        )
        logger.info("Connected to Redis")
    }

    private fun subscribeToRedis() {
        val redis = redisManager ?: return
        pubSub = redis.subscribe(RedisManager.CHANNEL_TELEPORT) { _, message ->
            try {
                val request = redis.parseTeleportRequest(message)

                logger.info("[TPA-DEBUG] Redis 메시지 수신: type=${request.type}, tpaType=${request.tpaType}, targetServer=${request.targetServer}, player=${request.playerName}, targetPlayer=${request.targetPlayerName}")

                // 이 서버가 대상 서버인 경우에만 처리
                if (request.targetServer == pluginConfig.serverName) {
                    logger.info("[TPA-DEBUG] 이 서버(${pluginConfig.serverName})가 대상 서버임 - 처리 시작")
                    Bukkit.getScheduler().runTask(this, Runnable {
                        handleTeleportRequest(request)
                    })
                } else {
                    logger.info("[TPA-DEBUG] 다른 서버(${request.targetServer})가 대상 - 무시")
                }
            } catch (e: Exception) {
                logger.warning("Failed to parse teleport request: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun handleTeleportRequest(request: TeleportRequest) {
        logger.info("[TPA-DEBUG] handleTeleportRequest 시작: tpaType=${request.tpaType}")

        // prepare_teleport 타입은 플레이어 검색 불필요 (아직 서버에 없음)
        if (request.tpaType == "prepare_teleport" || request.tpaType == "prepare_teleport_coords") {
            logger.info("[TPA-DEBUG] prepare 타입 - 플레이어 검색 생략")
            handleTpa(null, request)
            return
        }

        // TPA의 경우: request/here_request는 targetPlayerName, accept/deny 등은 playerName으로 검색
        val player = if (request.type == TeleportType.TPA) {
            when (request.tpaType) {
                "request", "here_request" -> {
                    // 요청 알림은 대상 플레이어에게 보내야 함
                    logger.info("[TPA-DEBUG] 대상 플레이어 검색: ${request.targetPlayerName}")
                    val found = request.targetPlayerName?.let { getPlayerIgnoreCase(it) }
                    logger.info("[TPA-DEBUG] 플레이어 검색 결과: ${found?.name ?: "null"}")
                    found
                }
                else -> {
                    // accept, deny 등은 요청자에게 보내야 함
                    logger.info("[TPA-DEBUG] 요청자 검색: ${request.playerName}")
                    val found = getPlayerIgnoreCase(request.playerName)
                    logger.info("[TPA-DEBUG] 플레이어 검색 결과: ${found?.name ?: "null"}")
                    found
                }
            }
        } else {
            if (request.playerUuid.isNotEmpty()) {
                Bukkit.getPlayer(UUID.fromString(request.playerUuid))
            } else {
                getPlayerIgnoreCase(request.playerName)
            }
        }

        // 워프 설정 요청 처리
        val warpName = request.warpName
        if (warpName != null && warpName.startsWith("SET:")) {
            handleSetWarp(request)
            return
        }

        // 플레이어가 아직 접속하지 않은 경우 대기열에 추가
        if (player == null) {
            // TPA는 좌표가 있을 때만 대기열에 추가 (WARP와 동일한 패턴)
            if (request.type == TeleportType.TPA) {
                if (request.tpaType.isNullOrEmpty() && request.world != null &&
                    request.x != null && request.y != null && request.z != null &&
                    request.playerUuid.isNotEmpty()) {
                    logger.info("[TPA-DEBUG] TPA 좌표 요청을 pendingTeleports에 저장: player=${request.playerName}, world=${request.world}")
                    pendingTeleports[UUID.fromString(request.playerUuid)] = request
                } else if (!request.tpaType.isNullOrEmpty()) {
                    // tpaType이 있으면 handleTpa로 처리 (prepare 메시지 등)
                    handleTpa(null, request)
                }
            } else {
                // WARP, RANDOM_TP 등은 항상 대기열에 추가
                if (request.playerUuid.isNotEmpty()) {
                    pendingTeleports[UUID.fromString(request.playerUuid)] = request
                }
            }
            return
        }

        // 텔레포트 실행
        when (request.type) {
            TeleportType.WARP -> {
                val worldName = request.world
                val x = request.x
                val y = request.y
                val z = request.z
                if (worldName != null && x != null && y != null && z != null) {
                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        val location = Location(world, x, y, z, request.yaw ?: 0f, request.pitch ?: 0f)
                        player.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                player.sendMessage(messages.warp.teleported)
                            }
                        }
                    } else {
                        player.sendMessage(messages.worldNotFound)
                    }
                }
            }
            TeleportType.RANDOM_TP -> {
                handleRandomTp(player, request)
            }
            TeleportType.TPA -> {
                val worldName = request.world
                val x = request.x
                val y = request.y
                val z = request.z
                // WARP와 동일한 패턴: 좌표가 있고 tpaType이 없으면 바로 텔레포트
                if (request.tpaType.isNullOrEmpty() && worldName != null && x != null && y != null && z != null) {
                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        val location = Location(world, x, y, z, request.yaw ?: 0f, request.pitch ?: 0f)
                        logger.info("[TPA-DEBUG] 플레이어가 이미 서버에 있음 - 바로 텔레포트: ${location.world.name} (${location.x}, ${location.y}, ${location.z})")
                        player.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                player.sendMessage(messages.tpa.teleported)
                            }
                        }
                    } else {
                        player.sendMessage(messages.worldNotFound)
                    }
                } else {
                    // tpaType이 있으면 handleTpa로 처리
                    handleTpa(player, request)
                }
            }
            TeleportType.SPAWN -> {
                val worldName = request.world
                val x = request.x
                val y = request.y
                val z = request.z
                if (worldName != null && x != null && y != null && z != null) {
                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        val location = Location(world, x, y, z, request.yaw ?: 0f, request.pitch ?: 0f)
                        player.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                player.sendMessage(messages.spawn.teleported)
                            }
                        }
                    } else {
                        player.sendMessage(messages.worldNotFound)
                    }
                }
            }
        }
    }

    private val legacySerializer = LegacyComponentSerializer.legacySection()

    private fun text(message: String): Component = legacySerializer.deserialize(message)

    // 대소문자 구분 없이 플레이어 검색
    private fun getPlayerIgnoreCase(name: String): org.bukkit.entity.Player? {
        return Bukkit.getOnlinePlayers().firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    }

    private fun createTpaButtons(): Component {
        val tpa = messages.tpa

        val acceptButton = text(tpa.buttonAcceptText)
            .clickEvent(ClickEvent.runCommand("/tpaccept"))
            .hoverEvent(HoverEvent.showText(text(tpa.buttonAcceptHover)))

        val separator = text(tpa.buttonSeparator)

        val denyButton = text(tpa.buttonDenyText)
            .clickEvent(ClickEvent.runCommand("/tpdeny"))
            .hoverEvent(HoverEvent.showText(text(tpa.buttonDenyHover)))

        return acceptButton.append(separator).append(denyButton)
    }

    private fun handleTpa(player: org.bukkit.entity.Player?, request: TeleportRequest) {
        logger.info("[TPA-DEBUG] handleTpa 시작: player=${player?.name ?: "null"}, tpaType=${request.tpaType}")

        when (request.tpaType) {
            "request" -> {
                if (player == null) return
                logger.info("[TPA-DEBUG] TPA 요청 알림: ${request.playerName} -> ${player.name}")
                // 이 서버에 TPA 요청 저장 (수락/거절 시 사용) - UUID 포함
                addTpaRequest(request.playerName, request.playerUuid, player.name, false)
                // TPA 요청 알림 + 클릭 가능한 버튼
                player.sendMessage(text(messages.tpa.received.replacePlaceholders("player" to request.playerName)))
                player.sendMessage(createTpaButtons())
            }
            "here_request" -> {
                if (player == null) return
                logger.info("[TPA-DEBUG] TPA-HERE 요청 알림: ${request.playerName} <- ${player.name}")
                // 이 서버에 TPA 요청 저장 (수락/거절 시 사용) - UUID 포함
                addTpaRequest(request.playerName, request.playerUuid, player.name, true)
                // TPA Here 요청 알림 + 클릭 가능한 버튼
                player.sendMessage(text(messages.tpa.hereReceived.replacePlaceholders("player" to request.playerName)))
                player.sendMessage(createTpaButtons())
            }
            "accept_get_location" -> {
                if (player == null) return
                logger.info("[TPA-DEBUG] accept_get_location: 수락자=${player.name}, 요청자=${request.targetPlayerName}")
                // 수락자(player)의 위치를 가져와서 요청자에게 전달
                // player = 수락자, targetPlayerName = 요청자, targetPlayerServer = 요청자 서버
                val location = player.location
                val requesterServer = request.targetPlayerServer ?: return

                logger.info("[TPA-DEBUG] 수락자 위치: ${location.world.name} (${location.x}, ${location.y}, ${location.z}), 요청자 서버: $requesterServer")

                if (requesterServer == pluginConfig.serverName) {
                    logger.info("[TPA-DEBUG] 같은 서버 - 바로 텔레포트")
                    // 요청자가 같은 서버에 있음 - 바로 텔레포트
                    val requester = request.targetPlayerName?.let { getPlayerIgnoreCase(it) }
                    if (requester != null) {
                        requester.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                requester.sendMessage(messages.tpa.teleported)
                            }
                        }
                    }
                } else {
                    // 크로스 서버 TPA - 요청자를 이 서버로 이동시킨 후 텔레포트
                    logger.info("[TPA-DEBUG] 크로스 서버 TPA: ${location.world.name} (${location.x}, ${location.y}, ${location.z})")

                    val requesterUuid = request.targetPlayerUuid ?: ""

                    // 1. 좌표를 pendingTeleports에 미리 저장 (요청자가 도착하면 텔레포트)
                    if (requesterUuid.isNotEmpty()) {
                        val tpaRequest = TeleportRequest(
                            playerUuid = requesterUuid,
                            playerName = request.targetPlayerName ?: "",
                            type = TeleportType.TPA,
                            targetServer = pluginConfig.serverName,
                            world = location.world.name,
                            x = location.x,
                            y = location.y,
                            z = location.z,
                            yaw = location.yaw,
                            pitch = location.pitch
                        )
                        pendingTeleports[UUID.fromString(requesterUuid)] = tpaRequest
                        logger.info("[TPA-DEBUG] pendingTeleports에 저장: UUID=$requesterUuid")

                        // 2. Velocity에 요청자 서버 이동 요청
                        redisManager?.publishServerTransfer(requesterUuid, pluginConfig.serverName)
                        logger.info("[TPA-DEBUG] 서버 이동 요청: $requesterUuid -> ${pluginConfig.serverName}")
                    }
                }
            }
            "move_player" -> {
                if (player == null) return
                logger.info("[TPA-DEBUG] move_player: player=${player.name}, targetServer=${request.targetPlayerServer}")
                // 플레이어를 목적지 서버로 이동 (좌표는 이미 목적지 서버에 전송됨)
                val targetServer = request.targetPlayerServer ?: return

                logger.info("[TPA-DEBUG] 서버 이동: ${player.name} -> $targetServer")
                // BungeeCord로 서버 이동 (WARP와 동일한 방식)
                val out = java.io.ByteArrayOutputStream()
                val dataOut = java.io.DataOutputStream(out)
                dataOut.writeUTF("Connect")
                dataOut.writeUTF(targetServer)
                player.sendPluginMessage(this, "BungeeCord", out.toByteArray())
            }
            "do_teleport" -> {
                // 하위 호환성을 위해 유지 - move_player로 리다이렉트
                logger.info("[TPA-DEBUG] do_teleport (deprecated) -> move_player 리다이렉트")
                handleTpa(player, request.copy(tpaType = "move_player"))
            }
            "prepare_teleport" -> {
                // 크로스 서버 텔레포트 준비 - pendingTeleports에 미리 저장 (플레이어 이름)
                logger.info("[TPA-DEBUG] prepare_teleport: player=${request.playerName}, targetPlayer=${request.targetPlayerName}")
                val playerUuid = UUID.fromString(request.playerUuid)
                val tpaRequest = TeleportRequest(
                    playerUuid = request.playerUuid,
                    playerName = request.playerName,
                    type = TeleportType.TPA,
                    targetServer = pluginConfig.serverName,
                    world = null,
                    x = null,
                    y = null,
                    z = null,
                    yaw = null,
                    pitch = null,
                    targetPlayerName = request.targetPlayerName,
                    tpaType = "do_teleport"
                )
                pendingTeleports[playerUuid] = tpaRequest
                logger.info("[TPA-DEBUG] pendingTeleports에 저장 완료: UUID=$playerUuid, targetPlayer=${request.targetPlayerName}")
            }
            "prepare_teleport_coords" -> {
                // 크로스 서버 텔레포트 준비 - pendingTeleports에 좌표와 함께 저장
                logger.info("[TPA-DEBUG] prepare_teleport_coords: player=${request.playerName}, 좌표=${request.world} (${request.x}, ${request.y}, ${request.z})")
                val playerUuid = UUID.fromString(request.playerUuid)
                val tpaRequest = TeleportRequest(
                    playerUuid = request.playerUuid,
                    playerName = request.playerName,
                    type = TeleportType.TPA,
                    targetServer = pluginConfig.serverName,
                    world = request.world,
                    x = request.x,
                    y = request.y,
                    z = request.z,
                    yaw = request.yaw,
                    pitch = request.pitch,
                    targetPlayerName = request.targetPlayerName,
                    tpaType = "do_teleport_coords"
                )
                pendingTeleports[playerUuid] = tpaRequest
                logger.info("[TPA-DEBUG] pendingTeleports에 좌표와 함께 저장 완료: UUID=$playerUuid")
            }
            "accept" -> {
                // (더 이상 사용되지 않음 - do_teleport로 대체됨)
                // 하위 호환성을 위해 유지
            }
            "accept_here_get_location" -> {
                if (player == null) return
                // 요청자(player)의 위치를 가져와서 수락자에게 전달
                // player = 요청자, targetPlayerName = 수락자, targetPlayerUuid = 수락자 UUID
                val location = player.location
                val accepterServer = request.targetPlayerServer ?: return
                val accepterUuid = request.targetPlayerUuid ?: ""

                logger.info("[TPA-DEBUG] accept_here_get_location: 요청자=${player.name}, 수락자=${request.targetPlayerName}, 수락자서버=$accepterServer")

                if (accepterServer == pluginConfig.serverName) {
                    // 수락자가 같은 서버에 있음 - 바로 텔레포트
                    val accepter = request.targetPlayerName?.let { getPlayerIgnoreCase(it) }
                    if (accepter != null) {
                        accepter.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                accepter.sendMessage(messages.tpa.teleported)
                            }
                        }
                    }
                } else {
                    // 크로스 서버 TPA-HERE - 수락자가 이 서버로 이동 중
                    // TpAcceptCommand에서 이미 서버 이동을 요청했으므로, 좌표만 저장
                    logger.info("[TPA-DEBUG] 크로스 서버 TPA-HERE: ${location.world.name} (${location.x}, ${location.y}, ${location.z})")

                    if (accepterUuid.isNotEmpty()) {
                        val tpaRequest = TeleportRequest(
                            playerUuid = accepterUuid,
                            playerName = request.targetPlayerName ?: "",
                            type = TeleportType.TPA,
                            targetServer = pluginConfig.serverName,
                            world = location.world.name,
                            x = location.x,
                            y = location.y,
                            z = location.z,
                            yaw = location.yaw,
                            pitch = location.pitch
                        )
                        pendingTeleports[UUID.fromString(accepterUuid)] = tpaRequest
                        logger.info("[TPA-DEBUG] pendingTeleports에 저장 (수락자 도착 대기): UUID=$accepterUuid")
                    }
                }
            }
            "accept_here" -> {
                // (더 이상 사용되지 않음 - do_teleport로 대체됨)
                // 하위 호환성을 위해 유지
            }
            "deny" -> {
                if (player == null) return
                // TPA 거절됨
                player.sendMessage(messages.tpa.requestDenied.replacePlaceholders("player" to (request.targetPlayerName ?: "")))
            }
        }
    }

    private fun hasPermission(player: org.bukkit.entity.Player, permission: String): Boolean {
        return player.isOp || player.hasPermission("nextwarp.admin") || player.hasPermission(permission)
    }

    private fun handleSetWarp(request: TeleportRequest) {
        val playerUuid = UUID.fromString(request.playerUuid)
        val player = Bukkit.getPlayer(playerUuid) ?: return

        val warpName = request.warpName!!.removePrefix("SET:")
        val location = player.location

        // 스폰 설정인 경우
        if (warpName == "_spawn") {
            if (!hasPermission(player, "nextwarp.spawn.set")) {
                player.sendMessage(messages.noPermission)
                return
            }

            databaseManager?.saveSpawn(
                server = pluginConfig.serverName,
                world = location.world.name,
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            )
            player.sendMessage(messages.spawn.set)
            player.sendMessage(messages.spawn.setLocation.replacePlaceholders(
                "world" to location.world.name,
                "x" to location.blockX,
                "y" to location.blockY,
                "z" to location.blockZ
            ))
            return
        }

        // 일반 워프 설정
        if (!hasPermission(player, "nextwarp.warp.set")) {
            player.sendMessage(messages.noPermission)
            return
        }

        val warp = WarpLocation(
            name = warpName,
            server = pluginConfig.serverName,
            world = location.world.name,
            x = location.x,
            y = location.y,
            z = location.z,
            yaw = location.yaw,
            pitch = location.pitch
        )

        databaseManager?.saveWarp(warp)
        player.sendMessage(messages.warp.set.replacePlaceholders("name" to warpName))
        player.sendMessage(messages.warp.setLocation.replacePlaceholders(
            "world" to location.world.name,
            "x" to location.blockX,
            "y" to location.blockY,
            "z" to location.blockZ
        ))
    }

    private fun handleRandomTp(player: org.bukkit.entity.Player, request: TeleportRequest) {
        val worldName = request.world
        val world = if (worldName != null) {
            Bukkit.getWorld(worldName)
        } else {
            Bukkit.getWorld(pluginConfig.randomTp.defaultWorld)
        }

        if (world == null) {
            player.sendMessage(messages.worldNotFound)
            return
        }

        val x = request.x?.toInt() ?: return
        val z = request.z?.toInt() ?: return

        // 안전한 Y좌표 찾기
        player.sendMessage(messages.randomTp.searching)

        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            world.getChunkAtAsync(x shr 4, z shr 4).thenAccept { chunk ->
                Bukkit.getScheduler().runTask(this, Runnable {
                    val safeY = findSafeY(world, x, z)
                    if (safeY != null) {
                        val location = Location(world, x.toDouble() + 0.5, safeY.toDouble(), z.toDouble() + 0.5)
                        player.teleportAsync(location).thenAccept { success ->
                            if (success) {
                                player.sendMessage(messages.randomTp.completed)
                                player.sendMessage(messages.randomTp.location.replacePlaceholders(
                                    "world" to world.name,
                                    "x" to x,
                                    "y" to safeY,
                                    "z" to z
                                ))
                            }
                        }
                    } else {
                        player.sendMessage(messages.randomTp.noSafeLocation)
                    }
                })
            }
        })
    }

    private fun findSafeY(world: org.bukkit.World, x: Int, z: Int): Int? {
        val highestY = world.getHighestBlockYAt(x, z)
        val safeBlockNames = pluginConfig.randomTp.safeBlocks.map { it.uppercase() }.toSet()

        for (y in highestY downTo world.minHeight) {
            val block = world.getBlockAt(x, y, z)
            val above1 = world.getBlockAt(x, y + 1, z)
            val above2 = world.getBlockAt(x, y + 2, z)

            // 설정된 안전 블럭 위에서만 스폰
            val blockTypeName = block.type.name
            if (safeBlockNames.contains(blockTypeName) &&
                !block.isLiquid &&
                above1.type.isAir && above2.type.isAir) {
                return y + 1
            }
        }

        return null
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(this), this)
    }

    private fun registerCommands() {
        // Spawn 명령어
        getCommand("spawn")?.setExecutor(SpawnCommand(this))
        getCommand("setspawn")?.setExecutor(SetSpawnCommand(this))

        // Warp 명령어
        val warpCmd = WarpCommand(this)
        getCommand("warp")?.setExecutor(warpCmd)
        getCommand("warp")?.tabCompleter = warpCmd

        getCommand("setwarp")?.setExecutor(SetWarpCommand(this))

        val delWarpCmd = DelWarpCommand(this)
        getCommand("delwarp")?.setExecutor(delWarpCmd)
        getCommand("delwarp")?.tabCompleter = delWarpCmd

        getCommand("warps")?.setExecutor(WarpListCommand(this))

        // RandomTP 명령어
        getCommand("randomtp")?.setExecutor(RandomTpCommand(this))

        // TPA 명령어
        val tpaCmd = TpaCommand(this)
        getCommand("tpa")?.setExecutor(tpaCmd)
        getCommand("tpa")?.tabCompleter = tpaCmd

        val tpaHereCmd = TpaHereCommand(this)
        getCommand("tpahere")?.setExecutor(tpaHereCmd)
        getCommand("tpahere")?.tabCompleter = tpaHereCmd

        getCommand("tpaccept")?.setExecutor(TpAcceptCommand(this))
        getCommand("tpdeny")?.setExecutor(TpDenyCommand(this))

        logger.info("Commands registered!")
    }

    private fun startTpaExpirationTask() {
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            val now = System.currentTimeMillis()
            pendingTpaRequests.entries.removeIf { (_, value) ->
                value.expireTime < now
            }
        }, 200L, 200L) // 10초마다
    }

    // TPA 요청 관리
    fun addTpaRequest(requester: String, requesterUuid: String, target: String, isHereRequest: Boolean = false) {
        val expireTime = System.currentTimeMillis() + (pluginConfig.tpa.expireSeconds * 1000L)
        pendingTpaRequests[target.lowercase()] = TpaRequestData(requester, requesterUuid, isHereRequest, expireTime)
    }

    fun getPendingTpaRequest(target: String): TpaRequestData? {
        return pendingTpaRequests[target.lowercase()]
    }

    fun removeTpaRequest(target: String) {
        pendingTpaRequests.remove(target.lowercase())
    }

    private fun registerChannels() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
    }

    private fun registerAPI() {
        server.servicesManager.register(
            NextWarpAPI::class.java,
            NextWarpAPIImpl(this),
            this,
            ServicePriority.Normal
        )
        logger.info("NextWarp API registered!")
    }
}