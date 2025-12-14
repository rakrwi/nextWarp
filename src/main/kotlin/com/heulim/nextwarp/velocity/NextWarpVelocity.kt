package com.heulim.nextwarp.velocity

import com.google.inject.Inject
import com.heulim.nextwarp.common.config.ConfigLoader
import com.heulim.nextwarp.common.config.MessageConfig
import com.heulim.nextwarp.common.config.MessageLoader
import com.heulim.nextwarp.common.config.PluginConfig
import com.heulim.nextwarp.common.redis.RedisManager
import com.heulim.nextwarp.velocity.listener.PlayerConnectionListener
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.nio.file.Path
import java.util.logging.Logger as JLogger

@Plugin(
    id = "nextwarp",
    name = "NextWarp",
    version = "1.0.0",
    description = "Cross-server warp, spawn and random teleport plugin",
    authors = ["Heulim"]
)
class NextWarpVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path
) {
    lateinit var config: PluginConfig
    lateinit var messages: MessageConfig
    var redisManager: RedisManager? = null

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        loadConfig()
        loadMessages()
        try {
            initializeManagers()
            registerListeners()
            syncServers()
            syncOnlinePlayers()
            logger.info("NextWarp Velocity has been enabled!")
        } catch (e: Exception) {
            logger.error("Failed to initialize NextWarp: ${e.message}")
            logger.error("Please check your config.yml settings (MySQL/Redis)")
            e.printStackTrace()
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        // 온라인 플레이어 목록 초기화
        redisManager?.setOnlinePlayers(emptyMap())
        redisManager?.close()
        logger.info("NextWarp Velocity has been disabled!")
    }

    private fun registerListeners() {
        server.eventManager.register(this, PlayerConnectionListener(this))
    }

    private fun syncServers() {
        // Velocity에 등록된 모든 서버 목록을 Redis에 동기화
        val servers = server.allServers.map { it.serverInfo.name }.toSet()
        redisManager?.setServers(servers)
        logger.info("Synced ${servers.size} servers to Redis: $servers")
    }

    private fun syncOnlinePlayers() {
        // 현재 접속 중인 플레이어 목록을 Redis에 동기화
        val players = mutableMapOf<String, String>()
        for (player in server.allPlayers) {
            val serverName = player.currentServer.orElse(null)?.serverInfo?.name ?: continue
            players[player.username] = serverName
        }
        redisManager?.setOnlinePlayers(players)
        logger.info("Synced ${players.size} online players to Redis")
    }

    private fun loadConfig() {
        val jLogger = JLogger.getLogger("NextWarp")
        config = ConfigLoader.loadConfig(dataDirectory.toFile(), jLogger)
    }

    private fun loadMessages() {
        val jLogger = JLogger.getLogger("NextWarp")
        messages = MessageLoader.loadMessages(dataDirectory.toFile(), jLogger)
    }

    private fun initializeManagers() {
        redisManager = RedisManager(
            host = config.redis.host,
            port = config.redis.port,
            password = config.redis.password.ifEmpty { null }
        )
        logger.info("Connected to Redis")

        // Redis에서 서버 이동 요청 구독
        redisManager?.subscribeToServerTransfer { playerUuid, targetServer ->
            transferPlayer(playerUuid, targetServer)
        }
    }

    fun transferPlayer(playerUuid: String, serverName: String) {
        val player = server.getPlayer(java.util.UUID.fromString(playerUuid)).orElse(null)
        if (player != null) {
            val targetServer = server.getServer(serverName).orElse(null)
            if (targetServer != null) {
                player.createConnectionRequest(targetServer).fireAndForget()
            }
        }
    }
}