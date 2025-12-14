package com.heulim.nextwarp.common.redis

import com.google.gson.Gson
import com.heulim.nextwarp.common.data.TeleportRequest
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub

class RedisManager(
    host: String,
    port: Int,
    password: String?
) {
    private val jedisPool: JedisPool
    private val gson = Gson()

    companion object {
        const val CHANNEL_TELEPORT = "nextwarp:teleport"
        const val CHANNEL_TELEPORT_RESPONSE = "nextwarp:teleport_response"
        const val CHANNEL_PLAYER_LIST = "nextwarp:player_list"
        const val CHANNEL_SERVER_TRANSFER = "nextwarp:server_transfer"
        const val KEY_ONLINE_PLAYERS = "nextwarp:online_players"
        const val KEY_SERVERS = "nextwarp:servers"
    }

    init {
        val config = JedisPoolConfig().apply {
            maxTotal = 16
            maxIdle = 8
            minIdle = 2
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
        }

        jedisPool = if (password.isNullOrEmpty()) {
            JedisPool(config, host, port)
        } else {
            JedisPool(config, host, port, 2000, password)
        }
    }

    fun publish(channel: String, message: String) {
        jedisPool.resource.use { jedis ->
            jedis.publish(channel, message)
        }
    }

    fun publishTeleportRequest(request: TeleportRequest) {
        val json = gson.toJson(request)
        publish(CHANNEL_TELEPORT, json)
    }

    fun subscribe(channel: String, handler: (String, String) -> Unit): JedisPubSub {
        val pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                handler(channel, message)
            }
        }

        Thread {
            jedisPool.resource.use { jedis ->
                jedis.subscribe(pubSub, channel)
            }
        }.start()

        return pubSub
    }

    fun subscribeMultiple(channels: Array<String>, handler: (String, String) -> Unit): JedisPubSub {
        val pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                handler(channel, message)
            }
        }

        Thread {
            jedisPool.resource.use { jedis ->
                jedis.subscribe(pubSub, *channels)
            }
        }.start()

        return pubSub
    }

    fun parseTeleportRequest(json: String): TeleportRequest {
        return gson.fromJson(json, TeleportRequest::class.java)
    }

    // 서버 이동 요청 (Paper -> Velocity)
    fun publishServerTransfer(playerUuid: String, targetServer: String) {
        publish(CHANNEL_SERVER_TRANSFER, "$playerUuid:$targetServer")
    }

    // 서버 이동 구독 (Velocity에서 사용)
    fun subscribeToServerTransfer(handler: (String, String) -> Unit): JedisPubSub {
        return subscribe(CHANNEL_SERVER_TRANSFER) { _, message ->
            val parts = message.split(":")
            if (parts.size == 2) {
                handler(parts[0], parts[1])
            }
        }
    }

    // 온라인 플레이어 관리 (Velocity에서 사용)
    // 저장 형식: key=소문자이름, value=서버:원래이름
    fun setOnlinePlayers(players: Map<String, String>) {
        jedisPool.resource.use { jedis ->
            jedis.del(KEY_ONLINE_PLAYERS)
            if (players.isNotEmpty()) {
                // key: 소문자, value: 서버:원래이름
                val formattedPlayers = players.map { (name, server) ->
                    name.lowercase() to "$server:$name"
                }.toMap()
                jedis.hset(KEY_ONLINE_PLAYERS, formattedPlayers)
            }
        }
    }

    fun addOnlinePlayer(playerName: String, serverName: String) {
        jedisPool.resource.use { jedis ->
            // key: 소문자, value: 서버:원래이름
            jedis.hset(KEY_ONLINE_PLAYERS, playerName.lowercase(), "$serverName:$playerName")
        }
    }

    fun removeOnlinePlayer(playerName: String) {
        jedisPool.resource.use { jedis ->
            jedis.hdel(KEY_ONLINE_PLAYERS, playerName.lowercase())
        }
    }

    /**
     * 온라인 플레이어 목록 반환
     * @return Map<원래이름, 서버>
     */
    fun getOnlinePlayers(): Map<String, String> {
        return jedisPool.resource.use { jedis ->
            val raw = jedis.hgetAll(KEY_ONLINE_PLAYERS) ?: emptyMap()
            // value 형식: 서버:원래이름 -> 원래이름 to 서버
            raw.values.associate { value ->
                val parts = value.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[1] to parts[0]  // 원래이름 to 서버
                } else {
                    value to ""
                }
            }
        }
    }

    fun getPlayerServer(playerName: String): String? {
        return jedisPool.resource.use { jedis ->
            val value = jedis.hget(KEY_ONLINE_PLAYERS, playerName.lowercase()) ?: return@use null
            // value 형식: 서버:원래이름
            value.split(":", limit = 2).firstOrNull()
        }
    }

    // 서버 목록 관리 (Velocity에서 사용)
    fun setServers(servers: Set<String>) {
        jedisPool.resource.use { jedis ->
            jedis.del(KEY_SERVERS)
            if (servers.isNotEmpty()) {
                jedis.sadd(KEY_SERVERS, *servers.toTypedArray())
            }
        }
    }

    fun getServers(): Set<String> {
        return jedisPool.resource.use { jedis ->
            jedis.smembers(KEY_SERVERS) ?: emptySet()
        }
    }

    fun close() {
        if (!jedisPool.isClosed) {
            jedisPool.close()
        }
    }
}