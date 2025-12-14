package com.heulim.nextwarp.api

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * NextWarp API - 타 플러그인에서 워프 기능을 사용할 수 있는 API
 *
 * 사용 예시:
 * ```kotlin
 * val api = Bukkit.getServicesManager().getRegistration(NextWarpAPI::class.java)?.provider
 * api?.warpPlayer(player, "lobby")
 * ```
 *
 * Java 사용 예시:
 * ```java
 * NextWarpAPI api = Bukkit.getServicesManager().getRegistration(NextWarpAPI.class).getProvider();
 * api.warpPlayer(player, "lobby");
 * ```
 */
interface NextWarpAPI {

    // ==================== 워프 관련 ====================

    /**
     * 플레이어를 워프로 텔레포트합니다.
     * 다른 서버에 있는 워프인 경우 서버 이동 후 텔레포트됩니다.
     *
     * @param player 텔레포트할 플레이어
     * @param warpName 워프 이름
     * @return 워프가 존재하면 true, 없으면 false
     */
    fun warpPlayer(player: Player, warpName: String): Boolean

    /**
     * 현재 위치에 워프를 생성합니다.
     *
     * @param warpName 워프 이름
     * @param location 워프 위치
     * @return 성공 여부
     */
    fun createWarp(warpName: String, location: Location): Boolean

    /**
     * 워프를 삭제합니다.
     *
     * @param warpName 워프 이름
     * @return 삭제 성공 여부 (존재하지 않으면 false)
     */
    fun deleteWarp(warpName: String): Boolean

    /**
     * 워프가 존재하는지 확인합니다.
     *
     * @param warpName 워프 이름
     * @return 존재 여부
     */
    fun warpExists(warpName: String): Boolean

    /**
     * 워프 정보를 가져옵니다.
     *
     * @param warpName 워프 이름
     * @return 워프 정보 (없으면 null)
     */
    fun getWarp(warpName: String): WarpInfo?

    /**
     * 모든 워프 목록을 가져옵니다.
     *
     * @return 워프 목록
     */
    fun getAllWarps(): List<WarpInfo>

    /**
     * 특정 서버의 워프 목록을 가져옵니다.
     *
     * @param serverName 서버 이름
     * @return 해당 서버의 워프 목록
     */
    fun getWarps(serverName: String): List<WarpInfo>

    // ==================== 랜덤 텔레포트 관련 ====================

    /**
     * 플레이어를 현재 서버에서 랜덤 텔레포트합니다.
     *
     * @param player 텔레포트할 플레이어
     * @return 텔레포트 요청 성공 여부
     */
    fun randomTeleport(player: Player): Boolean

    /**
     * 플레이어를 특정 서버에서 랜덤 텔레포트합니다.
     *
     * @param player 텔레포트할 플레이어
     * @param serverName 서버 이름
     * @return 텔레포트 요청 성공 여부
     */
    fun randomTeleport(player: Player, serverName: String): Boolean

    /**
     * 플레이어를 특정 서버의 특정 월드에서 랜덤 텔레포트합니다.
     *
     * @param player 텔레포트할 플레이어
     * @param serverName 서버 이름
     * @param worldName 월드 이름
     * @return 텔레포트 요청 성공 여부
     */
    fun randomTeleport(player: Player, serverName: String, worldName: String): Boolean

    // ==================== 유틸리티 ====================

    /**
     * 현재 서버 이름을 가져옵니다.
     *
     * @return 서버 이름
     */
    fun getServerName(): String
}

/**
 * 워프 정보
 */
data class WarpInfo(
    val name: String,
    val server: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    /**
     * 같은 서버에 있을 경우 Location 객체로 변환합니다.
     * 다른 서버의 워프인 경우 null을 반환합니다.
     */
    fun toLocation(): Location? {
        val world = org.bukkit.Bukkit.getWorld(this.world) ?: return null
        return Location(world, x, y, z, yaw, pitch)
    }
}