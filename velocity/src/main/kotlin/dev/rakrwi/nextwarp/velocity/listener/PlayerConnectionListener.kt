package dev.rakrwi.nextwarp.velocity.listener

import dev.rakrwi.nextwarp.velocity.NextWarpVelocity
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent

class PlayerConnectionListener(private val plugin: NextWarpVelocity) {

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val serverName = event.server.serverInfo.name

        // Redis에 플레이어 위치 업데이트
        plugin.redisManager?.addOnlinePlayer(player.username, serverName)
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player

        // Redis에서 플레이어 제거
        plugin.redisManager?.removeOnlinePlayer(player.username)
    }
}