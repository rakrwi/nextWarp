package dev.rakrwi.nextwarp.common.config

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.logging.Logger

object ConfigLoader {
    private val yaml = Yaml()

    fun loadConfig(dataFolder: File, logger: Logger? = null): PluginConfig {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val configFile = File(dataFolder, "config.yml")

        if (!configFile.exists()) {
            // 리소스에서 기본 config.yml 복사
            val inputStream: InputStream? = this::class.java.classLoader.getResourceAsStream("config.yml")
            if (inputStream != null) {
                configFile.writeBytes(inputStream.readBytes())
                inputStream.close()
                logger?.info("Created default config.yml - Please configure MySQL and Redis settings!")
            } else {
                // 리소스가 없으면 기본값으로 생성
                createDefaultConfig(configFile)
                logger?.info("Created default config.yml")
            }
        }

        return parseConfig(configFile, logger)
    }

    private fun createDefaultConfig(configFile: File) {
        val defaultContent = """
            # NextWarp Configuration

            # 서버 이름 (Velocity의 velocity.toml에서 설정한 서버 이름과 일치해야 합니다)
            server-name: "lobby"

            # MySQL 데이터베이스 설정
            mysql:
              host: "localhost"
              port: 3306
              database: "nextwarp"
              username: "root"
              password: ""

            # Redis 설정
            redis:
              host: "localhost"
              port: 6379
              password: ""

            # 랜덤 텔레포트 설정
            random-tp:
              min-x: -5000
              max-x: 5000
              min-z: -5000
              max-z: 5000
              default-world: "world"
              # 안전한 스폰 블럭 (이 블럭 위에서만 스폰)
              safe-blocks:
                - GRASS_BLOCK
                - DIRT
                - STONE
                - SAND
                - GRAVEL
                - PODZOL
                - MYCELIUM
                - SNOW_BLOCK
                - TERRACOTTA
                - RED_SAND
                - COARSE_DIRT
                - ROOTED_DIRT
                - MUD
                - MOSS_BLOCK
              # RTP 비활성화 서버 목록
              disabled-servers: []

            # TPA (플레이어간 텔레포트) 설정
            tpa:
              enabled: true
              expire-seconds: 60
              # TPA 비활성화 서버 목록
              disabled-servers: []

            # 쿨타임 설정 (초 단위)
            # 각 명령어별로 쿨타임을 설정할 수 있습니다.
            # bypass-permission: 해당 권한이 있으면 쿨타임 무시
            cooldown:
              # 워프 쿨타임
              warp:
                enabled: true
                seconds: 5
                bypass-permission: "nextwarp.cooldown.bypass.warp"
              # 랜덤 텔레포트 쿨타임
              random-tp:
                enabled: true
                seconds: 30
                bypass-permission: "nextwarp.cooldown.bypass.rtp"
              # TPA 쿨타임
              tpa:
                enabled: true
                seconds: 10
                bypass-permission: "nextwarp.cooldown.bypass.tpa"
        """.trimIndent()
        configFile.writeText(defaultContent)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseConfig(configFile: File, logger: Logger?): PluginConfig {
        val data: Map<String, Any> = FileInputStream(configFile).use { yaml.load(it) }

        val serverName = data["server-name"] as? String ?: "lobby"

        val mysqlData = data["mysql"] as? Map<String, Any> ?: emptyMap()
        val mysqlConfig = MySQLConfig(
            host = mysqlData["host"] as? String ?: "localhost",
            port = (mysqlData["port"] as? Number)?.toInt() ?: 3306,
            database = mysqlData["database"] as? String ?: "nextwarp",
            username = mysqlData["username"] as? String ?: "root",
            password = mysqlData["password"] as? String ?: ""
        )

        val redisData = data["redis"] as? Map<String, Any> ?: emptyMap()
        val redisConfig = RedisConfig(
            host = redisData["host"] as? String ?: "localhost",
            port = (redisData["port"] as? Number)?.toInt() ?: 6379,
            password = redisData["password"] as? String ?: ""
        )

        val rtpData = data["random-tp"] as? Map<String, Any> ?: emptyMap()
        val defaultSafeBlocks = listOf(
            "GRASS_BLOCK", "DIRT", "STONE", "SAND", "GRAVEL",
            "PODZOL", "MYCELIUM", "SNOW_BLOCK", "TERRACOTTA",
            "RED_SAND", "COARSE_DIRT", "ROOTED_DIRT", "MUD", "MOSS_BLOCK"
        )
        val randomTpConfig = RandomTpConfig(
            minX = (rtpData["min-x"] as? Number)?.toInt() ?: -5000,
            maxX = (rtpData["max-x"] as? Number)?.toInt() ?: 5000,
            minZ = (rtpData["min-z"] as? Number)?.toInt() ?: -5000,
            maxZ = (rtpData["max-z"] as? Number)?.toInt() ?: 5000,
            defaultWorld = rtpData["default-world"] as? String ?: "world",
            safeBlocks = (rtpData["safe-blocks"] as? List<*>)?.mapNotNull { it as? String } ?: defaultSafeBlocks,
            disabledServers = (rtpData["disabled-servers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )

        val tpaData = data["tpa"] as? Map<String, Any> ?: emptyMap()
        val tpaConfig = TpaConfig(
            enabled = tpaData["enabled"] as? Boolean ?: true,
            expireSeconds = (tpaData["expire-seconds"] as? Number)?.toInt() ?: 60,
            disabledServers = (tpaData["disabled-servers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )

        // 쿨타임 설정 파싱
        val cooldownData = data["cooldown"] as? Map<String, Any> ?: emptyMap()
        val cooldownConfig = parseCooldownConfig(cooldownData)

        logger?.info("Loaded config.yml (serverName: $serverName)")

        return PluginConfig(
            serverName = serverName,
            mysql = mysqlConfig,
            redis = redisConfig,
            randomTp = randomTpConfig,
            tpa = tpaConfig,
            cooldown = cooldownConfig
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCooldownConfig(data: Map<String, Any>): CooldownConfig {
        val warpData = data["warp"] as? Map<String, Any> ?: emptyMap()
        val rtpData = data["random-tp"] as? Map<String, Any> ?: emptyMap()
        val tpaData = data["tpa"] as? Map<String, Any> ?: emptyMap()

        return CooldownConfig(
            warp = parseCommandCooldown(warpData, "nextwarp.cooldown.bypass.warp", 5),
            randomTp = parseCommandCooldown(rtpData, "nextwarp.cooldown.bypass.rtp", 30),
            tpa = parseCommandCooldown(tpaData, "nextwarp.cooldown.bypass.tpa", 10)
        )
    }

    private fun parseCommandCooldown(
        data: Map<String, Any>,
        defaultBypassPermission: String,
        defaultSeconds: Int
    ): CommandCooldownConfig {
        return CommandCooldownConfig(
            enabled = data["enabled"] as? Boolean ?: true,
            seconds = (data["seconds"] as? Number)?.toInt() ?: defaultSeconds,
            bypassPermission = data["bypass-permission"] as? String ?: defaultBypassPermission
        )
    }
}