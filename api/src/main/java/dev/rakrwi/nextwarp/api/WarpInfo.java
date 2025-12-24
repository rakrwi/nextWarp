package dev.rakrwi.nextwarp.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 워프 정보를 담는 클래스
 */
public class WarpInfo {
    private final String name;
    private final String server;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * WarpInfo 생성자
     *
     * @param name   워프 이름
     * @param server 서버 이름
     * @param world  월드 이름
     * @param x      X 좌표
     * @param y      Y 좌표
     * @param z      Z 좌표
     * @param yaw    Yaw (수평 회전)
     * @param pitch  Pitch (수직 회전)
     */
    public WarpInfo(
            @NotNull String name,
            @NotNull String server,
            @NotNull String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        this.name = name;
        this.server = server;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * 워프 이름을 반환합니다.
     *
     * @return 워프 이름
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * 워프가 위치한 서버 이름을 반환합니다.
     *
     * @return 서버 이름
     */
    @NotNull
    public String getServer() {
        return server;
    }

    /**
     * 워프가 위치한 월드 이름을 반환합니다.
     *
     * @return 월드 이름
     */
    @NotNull
    public String getWorld() {
        return world;
    }

    /**
     * X 좌표를 반환합니다.
     *
     * @return X 좌표
     */
    public double getX() {
        return x;
    }

    /**
     * Y 좌표를 반환합니다.
     *
     * @return Y 좌표
     */
    public double getY() {
        return y;
    }

    /**
     * Z 좌표를 반환합니다.
     *
     * @return Z 좌표
     */
    public double getZ() {
        return z;
    }

    /**
     * Yaw (수평 회전)를 반환합니다.
     *
     * @return Yaw 값
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Pitch (수직 회전)를 반환합니다.
     *
     * @return Pitch 값
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * 워프 정보를 Location 객체로 변환합니다.
     * 같은 서버에 있는 경우에만 사용 가능합니다.
     * 월드가 존재하지 않으면 (예: 워프가 다른 서버에 있는 경우) null을 반환합니다.
     *
     * @return Location 객체, 월드를 찾을 수 없으면 null
     */
    @Nullable
    public Location toLocation() {
        World w = Bukkit.getWorld(this.world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return "WarpInfo{" +
                "name='" + name + '\'' +
                ", server='" + server + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarpInfo warpInfo = (WarpInfo) o;
        return Double.compare(warpInfo.x, x) == 0 &&
                Double.compare(warpInfo.y, y) == 0 &&
                Double.compare(warpInfo.z, z) == 0 &&
                Float.compare(warpInfo.yaw, yaw) == 0 &&
                Float.compare(warpInfo.pitch, pitch) == 0 &&
                name.equals(warpInfo.name) &&
                server.equals(warpInfo.server) &&
                world.equals(warpInfo.world);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + server.hashCode();
        result = 31 * result + world.hashCode();
        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        result = 31 * result + Float.hashCode(yaw);
        result = 31 * result + Float.hashCode(pitch);
        return result;
    }
}