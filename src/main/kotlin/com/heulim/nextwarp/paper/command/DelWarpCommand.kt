package com.heulim.nextwarp.paper.command

import com.heulim.nextwarp.common.config.replacePlaceholders
import com.heulim.nextwarp.paper.NextWarpPaper
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class DelWarpCommand(private val plugin: NextWarpPaper) : CommandExecutor, TabCompleter {

    private val msg get() = plugin.messages

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg.playerOnly)
            return true
        }

        if (!sender.hasPermission("nextwarp.warp.delete") && !sender.hasPermission("nextwarp.admin") && !sender.isOp) {
            sender.sendMessage(msg.noPermission)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§c사용법: /delwarp <이름>")
            return true
        }

        val warpName = args[0]
        val deleted = plugin.databaseManager?.deleteWarp(warpName) ?: false

        if (deleted) {
            sender.sendMessage(msg.warp.deleted.replacePlaceholders("name" to warpName))
        } else {
            sender.sendMessage(msg.warp.deleteNotFound.replacePlaceholders("name" to warpName))
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val warps = plugin.databaseManager?.getAllWarps() ?: emptyList()
            return warps.map { it.name }.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
