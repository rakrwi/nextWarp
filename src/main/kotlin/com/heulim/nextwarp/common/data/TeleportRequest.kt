package com.heulim.nextwarp.common.data

enum class TeleportType {
    WARP,
    RANDOM_TP,
    TPA,
    SPAWN
}

data class TeleportRequest(
    val playerUuid: String,
    val playerName: String,
    val type: TeleportType,
    val targetServer: String,
    val world: String?,
    val x: Double?,
    val y: Double?,
    val z: Double?,
    val yaw: Float?,
    val pitch: Float?,
    val warpName: String? = null,
    // TPA 관련 필드
    val targetPlayerUuid: String? = null,
    val targetPlayerName: String? = null,
    val targetPlayerServer: String? = null, // 대상 플레이어 서버 (크로스 서버 TPA용)
    val tpaType: String? = null // "request", "accept", "deny", "here_request", "accept_here"
)