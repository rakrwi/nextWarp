package dev.rakrwi.nextwarp.paper.command

import dev.rakrwi.nextwarp.common.config.replacePlaceholders
import dev.rakrwi.nextwarp.common.data.WarpLocation
import dev.rakrwi.nextwarp.paper.NextWarpPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SetWarpCommand(private val plugin: NextWarpPaper) : CommandExecutor {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        if (!sender.hasPermission("nextwarp.warp.set") && !sender.hasPermission("nextwarp.admin") && !sender.isOp) {
            sender.sendMessage(msg.noPermission)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(msg.warp.usageSet)
            return true
        }

        val warpName = args[0]
        val location = sender.location

        val warp = WarpLocation(
            name = warpName,
            server = plugin.pluginConfig.serverName,
            world = location.world.name,
            x = location.x,
            y = location.y,
            z = location.z,
            yaw = location.yaw,
            pitch = location.pitch
        )

        plugin.databaseManager?.saveWarp(warp)
        sender.sendMessage(msg.warp.set.replacePlaceholders("name" to warpName))
        sender.sendMessage(msg.warp.setLocation.replacePlaceholders(
            "world" to location.world.name,
            "x" to location.blockX,
            "y" to location.blockY,
            "z" to location.blockZ
        ))

        return true
    }
}
