package com.heulim.nextwarp.common.config

data class PluginConfig(
    val serverName: String = "lobby",
    val mysql: MySQLConfig = MySQLConfig(),
    val redis: RedisConfig = RedisConfig(),
    val randomTp: RandomTpConfig = RandomTpConfig(),
    val tpa: TpaConfig = TpaConfig(),
    val cooldown: CooldownConfig = CooldownConfig()
)

data class MySQLConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "nextwarp",
    val username: String = "root",
    val password: String = ""
)

data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = ""
)

data class RandomTpConfig(
    val minX: Int = -5000,
    val maxX: Int = 5000,
    val minZ: Int = -5000,
    val maxZ: Int = 5000,
    val defaultWorld: String = "world",
    val safeBlocks: List<String> = listOf(
        "GRASS_BLOCK", "DIRT", "STONE", "SAND", "GRAVEL",
        "PODZOL", "MYCELIUM", "SNOW_BLOCK", "TERRACOTTA",
        "RED_SAND", "COARSE_DIRT", "ROOTED_DIRT", "MUD", "MOSS_BLOCK"
    ),
    val disabledServers: List<String> = emptyList()
)

data class TpaConfig(
    val enabled: Boolean = true,
    val expireSeconds: Int = 60,
    val disabledServers: List<String> = emptyList()
)

data class CooldownConfig(
    val warp: CommandCooldownConfig = CommandCooldownConfig(
        enabled = true,
        seconds = 5,
        bypassPermission = "nextwarp.cooldown.bypass.warp"
    ),
    val randomTp: CommandCooldownConfig = CommandCooldownConfig(
        enabled = true,
        seconds = 30,
        bypassPermission = "nextwarp.cooldown.bypass.rtp"
    ),
    val tpa: CommandCooldownConfig = CommandCooldownConfig(
        enabled = true,
        seconds = 10,
        bypassPermission = "nextwarp.cooldown.bypass.tpa"
    ),
    val spawn: CommandCooldownConfig = CommandCooldownConfig(
        enabled = true,
        seconds = 3,
        bypassPermission = "nextwarp.cooldown.bypass.spawn"
    )
)

data class CommandCooldownConfig(
    val enabled: Boolean = true,
    val seconds: Int = 5,
    val bypassPermission: String = ""
)