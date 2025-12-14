package com.heulim.nextwarp.paper

import com.heulim.nextwarp.common.config.CommandCooldownConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CooldownManager {

    // commandType -> (playerUUID -> lastUseTime)
    private val cooldowns = ConcurrentHashMap<String, ConcurrentHashMap<UUID, Long>>()

    /**
     * 쿨타임을 체크합니다.
     * @return 남은 쿨타임(초). 0이면 쿨타임이 없음
     */
    fun getRemainingCooldown(playerUuid: UUID, commandType: String, config: CommandCooldownConfig): Int {
        if (!config.enabled) return 0

        val commandCooldowns = cooldowns[commandType] ?: return 0
        val lastUse = commandCooldowns[playerUuid] ?: return 0

        val elapsed = (System.currentTimeMillis() - lastUse) / 1000
        val remaining = config.seconds - elapsed.toInt()

        return if (remaining > 0) remaining else 0
    }

    /**
     * 쿨타임을 설정합니다.
     */
    fun setCooldown(playerUuid: UUID, commandType: String) {
        cooldowns.computeIfAbsent(commandType) { ConcurrentHashMap() }[playerUuid] = System.currentTimeMillis()
    }

    /**
     * 플레이어의 모든 쿨타임을 제거합니다.
     */
    fun clearPlayerCooldowns(playerUuid: UUID) {
        cooldowns.values.forEach { it.remove(playerUuid) }
    }

    companion object {
        const val WARP = "warp"
        const val SPAWN = "spawn"
        const val RANDOM_TP = "rtp"
        const val TPA = "tpa"
    }
}
