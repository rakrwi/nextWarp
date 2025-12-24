package dev.rakrwi.nextwarp.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * NextWarp API - 다른 플러그인에서 워프 기능을 사용하기 위한 API
 *
 * <p>사용 예시 (Java):</p>
 * <pre>
 * NextWarpAPI api = Bukkit.getServicesManager().getRegistration(NextWarpAPI.class).getProvider();
 * api.warpPlayer(player, "lobby");
 * </pre>
 *
 * <p>사용 예시 (Kotlin):</p>
 * <pre>
 * val api = Bukkit.getServicesManager().getRegistration(NextWarpAPI::class.java)?.provider
 * api?.warpPlayer(player, "lobby")
 * </pre>
 */
public interface NextWarpAPI {

    // ==================== 워프 메서드 ====================

    /**
     * 플레이어를 워프로 텔레포트합니다.
     * 워프가 다른 서버에 있는 경우, 플레이어를 해당 서버로 이동시킨 후 텔레포트합니다.
     *
     * @param player   텔레포트할 플레이어
     * @param warpName 워프 이름
     * @return 워프가 존재하면 true, 아니면 false
     */
    boolean warpPlayer(@NotNull Player player, @NotNull String warpName);

    /**
     * 지정된 위치에 워프를 생성합니다.
     *
     * @param warpName 워프 이름
     * @param location 워프 위치
     * @return 성공하면 true
     */
    boolean createWarp(@NotNull String warpName, @NotNull Location location);

    /**
     * 워프를 삭제합니다.
     *
     * @param warpName 워프 이름
     * @return 삭제 성공하면 true, 워프를 찾을 수 없으면 false
     */
    boolean deleteWarp(@NotNull String warpName);

    /**
     * 워프가 존재하는지 확인합니다.
     *
     * @param warpName 워프 이름
     * @return 워프가 존재하면 true
     */
    boolean warpExists(@NotNull String warpName);

    /**
     * 워프 정보를 가져옵니다.
     *
     * @param warpName 워프 이름
     * @return 워프 정보, 워프를 찾을 수 없으면 null
     */
    @Nullable
    WarpInfo getWarp(@NotNull String warpName);

    /**
     * 모든 워프 목록을 가져옵니다.
     *
     * @return 모든 워프 목록
     */
    @NotNull
    List<WarpInfo> getAllWarps();

    /**
     * 특정 서버의 워프 목록을 가져옵니다.
     *
     * @param serverName 서버 이름
     * @return 해당 서버의 워프 목록
     */
    @NotNull
    List<WarpInfo> getWarps(@NotNull String serverName);

    // ==================== 랜덤 텔레포트 메서드 ====================

    /**
     * 현재 서버에서 플레이어를 랜덤 텔레포트합니다.
     *
     * @param player 텔레포트할 플레이어
     * @return 요청이 성공하면 true
     */
    boolean randomTeleport(@NotNull Player player);

    /**
     * 특정 서버에서 플레이어를 랜덤 텔레포트합니다.
     *
     * @param player     텔레포트할 플레이어
     * @param serverName 서버 이름
     * @return 요청이 성공하면 true
     */
    boolean randomTeleport(@NotNull Player player, @NotNull String serverName);

    /**
     * 특정 서버의 특정 월드에서 플레이어를 랜덤 텔레포트합니다.
     *
     * @param player     텔레포트할 플레이어
     * @param serverName 서버 이름
     * @param worldName  월드 이름
     * @return 요청이 성공하면 true
     */
    boolean randomTeleport(@NotNull Player player, @NotNull String serverName, @NotNull String worldName);

    // ==================== 유틸리티 메서드 ====================

    /**
     * 현재 서버 이름을 가져옵니다.
     *
     * @return 서버 이름
     */
    @NotNull
    String getServerName();
}