package dev.rakrwi.nextwarp.common.database

import dev.rakrwi.nextwarp.common.data.WarpLocation
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class DatabaseManager(
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String
) {
    private val dataSource: HikariDataSource

    init {
        // MySQL 드라이버 명시적 로드
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            Class.forName("com.mysql.jdbc.Driver")
        }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true"
            this.username = username
            this.password = password
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 60000
            connectionTimeout = 30000
            maxLifetime = 1800000
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        dataSource = HikariDataSource(config)
        createTables()
    }

    private fun getConnection(): Connection = dataSource.connection

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // 워프 테이블
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS nextwarp_warps (
                        name VARCHAR(64) PRIMARY KEY,
                        server VARCHAR(64) NOT NULL,
                        world VARCHAR(64) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL
                    )
                """.trimIndent())

                // 스폰 테이블 (전체 서버 통틀어 단 하나만 존재)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS nextwarp_spawn (
                        id INT PRIMARY KEY DEFAULT 1,
                        server VARCHAR(64) NOT NULL,
                        world VARCHAR(64) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL,
                        CONSTRAINT single_spawn CHECK (id = 1)
                    )
                """.trimIndent())
            }
        }
    }

    // 워프 관련
    fun saveWarp(warp: WarpLocation) {
        getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT INTO nextwarp_warps (name, server, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE server=?, world=?, x=?, y=?, z=?, yaw=?, pitch=?
            """.trimIndent()).use { stmt ->
                stmt.setString(1, warp.name)
                stmt.setString(2, warp.server)
                stmt.setString(3, warp.world)
                stmt.setDouble(4, warp.x)
                stmt.setDouble(5, warp.y)
                stmt.setDouble(6, warp.z)
                stmt.setFloat(7, warp.yaw)
                stmt.setFloat(8, warp.pitch)
                stmt.setString(9, warp.server)
                stmt.setString(10, warp.world)
                stmt.setDouble(11, warp.x)
                stmt.setDouble(12, warp.y)
                stmt.setDouble(13, warp.z)
                stmt.setFloat(14, warp.yaw)
                stmt.setFloat(15, warp.pitch)
                stmt.executeUpdate()
            }
        }
    }

    fun getWarp(name: String): WarpLocation? {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM nextwarp_warps WHERE name = ?").use { stmt ->
                stmt.setString(1, name)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return WarpLocation(
                            name = rs.getString("name"),
                            server = rs.getString("server"),
                            world = rs.getString("world"),
                            x = rs.getDouble("x"),
                            y = rs.getDouble("y"),
                            z = rs.getDouble("z"),
                            yaw = rs.getFloat("yaw"),
                            pitch = rs.getFloat("pitch")
                        )
                    }
                }
            }
        }
        return null
    }

    fun deleteWarp(name: String): Boolean {
        getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM nextwarp_warps WHERE name = ?").use { stmt ->
                stmt.setString(1, name)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun getAllWarps(): List<WarpLocation> {
        val warps = mutableListOf<WarpLocation>()
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM nextwarp_warps").use { rs ->
                    while (rs.next()) {
                        warps.add(WarpLocation(
                            name = rs.getString("name"),
                            server = rs.getString("server"),
                            world = rs.getString("world"),
                            x = rs.getDouble("x"),
                            y = rs.getDouble("y"),
                            z = rs.getDouble("z"),
                            yaw = rs.getFloat("yaw"),
                            pitch = rs.getFloat("pitch")
                        ))
                    }
                }
            }
        }
        return warps
    }

    // 스폰 관련
    fun saveSpawn(server: String, world: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT INTO nextwarp_spawn (id, server, world, x, y, z, yaw, pitch)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE server=?, world=?, x=?, y=?, z=?, yaw=?, pitch=?
            """.trimIndent()).use { stmt ->
                stmt.setString(1, server)
                stmt.setString(2, world)
                stmt.setDouble(3, x)
                stmt.setDouble(4, y)
                stmt.setDouble(5, z)
                stmt.setFloat(6, yaw)
                stmt.setFloat(7, pitch)
                stmt.setString(8, server)
                stmt.setString(9, world)
                stmt.setDouble(10, x)
                stmt.setDouble(11, y)
                stmt.setDouble(12, z)
                stmt.setFloat(13, yaw)
                stmt.setFloat(14, pitch)
                stmt.executeUpdate()
            }
        }
    }

    fun getSpawn(): WarpLocation? {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM nextwarp_spawn WHERE id = 1").use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return WarpLocation(
                            name = "_spawn",
                            server = rs.getString("server"),
                            world = rs.getString("world"),
                            x = rs.getDouble("x"),
                            y = rs.getDouble("y"),
                            z = rs.getDouble("z"),
                            yaw = rs.getFloat("yaw"),
                            pitch = rs.getFloat("pitch")
                        )
                    }
                }
            }
        }
        return null
    }

    fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }
}