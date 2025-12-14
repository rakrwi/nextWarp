package com.heulim.nextwarp.paper.command

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class WarpListCommand(private val plugin: NextWarpPaper) : CommandExecutor {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        val warps = plugin.databaseManager?.getAllWarps() ?: emptyList()

        if (warps.isEmpty()) {
            sender.sendMessage(msg.warp.listEmpty)
            return true
        }

        sender.sendMessage(msg.warp.listHeader)
        for (warp in warps) {
            sender.sendMessage(msg.warp.listEntry.replacePlaceholders(
                "name" to warp.name,
                "server" to warp.server,
                "world" to warp.world,
                "x" to warp.x.toInt(),
                "y" to warp.y.toInt(),
                "z" to warp.z.toInt()
            ))
        }

        return true
    }
}
