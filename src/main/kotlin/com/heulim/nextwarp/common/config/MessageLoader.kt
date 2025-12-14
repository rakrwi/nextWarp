package com.heulim.nextwarp.common.config

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.logging.Logger

object MessageLoader {

    fun loadMessages(dataFolder: File, logger: Logger): MessageConfig {
        val messagesFile = File(dataFolder, "messages.yml")

        if (!messagesFile.exists()) {
            saveDefaultMessages(messagesFile, logger)
        }

        return try {
            val yaml = Yaml()
            val data: Map<String, Any> = messagesFile.inputStream().use { yaml.load(it) } ?: emptyMap()
            parseMessageConfig(data)
        } catch (e: Exception) {
            logger.warning("Failed to load messages.yml: ${e.message}")
            logger.info("Using default messages")
            MessageConfig()
        }
    }

    private fun saveDefaultMessages(file: File, logger: Logger) {
        file.parentFile?.mkdirs()

        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
        }
        val yaml = Yaml(options)

        val defaultConfig = createDefaultMap()
        file.writeText(yaml.dump(defaultConfig))
        logger.info("Created default messages.yml")
    }

    private fun createDefaultMap(): Map<String, Any> {
        val default = MessageConfig()
        return mapOf(
            "prefix" to default.prefix,
            "no-permission" to default.noPermission,
            "player-only" to default.playerOnly,
            "not-connected" to default.notConnected,
            "world-not-found" to default.worldNotFound,
            "server-not-found" to default.serverNotFound,
            "cooldown" to default.cooldown,
            "warp" to mapOf(
                "teleporting" to default.warp.teleporting,
                "teleported" to default.warp.teleported,
                "not-found" to default.warp.notFound,
                "set-request" to default.warp.setRequest,
                "set" to default.warp.set,
                "set-location" to default.warp.setLocation,
                "deleted" to default.warp.deleted,
                "delete-not-found" to default.warp.deleteNotFound,
                "list-empty" to default.warp.listEmpty,
                "list-header" to default.warp.listHeader,
                "list-entry" to default.warp.listEntry,
                "help-header" to default.warp.helpHeader,
                "help-warp" to default.warp.helpWarp,
                "help-set" to default.warp.helpSet,
                "help-delete" to default.warp.helpDelete,
                "help-list" to default.warp.helpList,
                "usage-set" to default.warp.usageSet
            ),
            "random-tp" to mapOf(
                "teleporting" to default.randomTp.teleporting,
                "coordinates" to default.randomTp.coordinates,
                "completed" to default.randomTp.completed,
                "location" to default.randomTp.location,
                "searching" to default.randomTp.searching,
                "no-safe-location" to default.randomTp.noSafeLocation,
                "disabled" to default.randomTp.disabled
            ),
            "tpa" to mapOf(
                "usage" to default.tpa.usage,
                "here-usage" to default.tpa.hereUsage,
                "sent" to default.tpa.sent,
                "here-sent" to default.tpa.hereSent,
                "received" to default.tpa.received,
                "here-received" to default.tpa.hereReceived,
                "button-accept-text" to default.tpa.buttonAcceptText,
                "button-accept-hover" to default.tpa.buttonAcceptHover,
                "button-deny-text" to default.tpa.buttonDenyText,
                "button-deny-hover" to default.tpa.buttonDenyHover,
                "button-separator" to default.tpa.buttonSeparator,
                "accepted" to default.tpa.accepted,
                "denied" to default.tpa.denied,
                "request-accepted" to default.tpa.requestAccepted,
                "request-denied" to default.tpa.requestDenied,
                "teleporting" to default.tpa.teleporting,
                "teleported" to default.tpa.teleported,
                "no-pending" to default.tpa.noPending,
                "player-not-found" to default.tpa.playerNotFound,
                "cannot-self" to default.tpa.cannotSelf,
                "disabled" to default.tpa.disabled,
                "target-disabled" to default.tpa.targetDisabled,
                "requester-offline" to default.tpa.requesterOffline,
                "expired" to default.tpa.expired
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMessageConfig(data: Map<String, Any>): MessageConfig {
        val default = MessageConfig()

        val warpData = data["warp"] as? Map<String, Any> ?: emptyMap()
        val rtpData = data["random-tp"] as? Map<String, Any> ?: emptyMap()
        val tpaData = data["tpa"] as? Map<String, Any> ?: emptyMap()

        return MessageConfig(
            prefix = data["prefix"] as? String ?: default.prefix,
            noPermission = data["no-permission"] as? String ?: default.noPermission,
            playerOnly = data["player-only"] as? String ?: default.playerOnly,
            notConnected = data["not-connected"] as? String ?: default.notConnected,
            worldNotFound = data["world-not-found"] as? String ?: default.worldNotFound,
            serverNotFound = data["server-not-found"] as? String ?: default.serverNotFound,
            cooldown = data["cooldown"] as? String ?: default.cooldown,
            warp = WarpMessages(
                teleporting = warpData["teleporting"] as? String ?: default.warp.teleporting,
                teleported = warpData["teleported"] as? String ?: default.warp.teleported,
                notFound = warpData["not-found"] as? String ?: default.warp.notFound,
                setRequest = warpData["set-request"] as? String ?: default.warp.setRequest,
                set = warpData["set"] as? String ?: default.warp.set,
                setLocation = warpData["set-location"] as? String ?: default.warp.setLocation,
                deleted = warpData["deleted"] as? String ?: default.warp.deleted,
                deleteNotFound = warpData["delete-not-found"] as? String ?: default.warp.deleteNotFound,
                listEmpty = warpData["list-empty"] as? String ?: default.warp.listEmpty,
                listHeader = warpData["list-header"] as? String ?: default.warp.listHeader,
                listEntry = warpData["list-entry"] as? String ?: default.warp.listEntry,
                helpHeader = warpData["help-header"] as? String ?: default.warp.helpHeader,
                helpWarp = warpData["help-warp"] as? String ?: default.warp.helpWarp,
                helpSet = warpData["help-set"] as? String ?: default.warp.helpSet,
                helpDelete = warpData["help-delete"] as? String ?: default.warp.helpDelete,
                helpList = warpData["help-list"] as? String ?: default.warp.helpList,
                usageSet = warpData["usage-set"] as? String ?: default.warp.usageSet
            ),
            randomTp = RandomTpMessages(
                teleporting = rtpData["teleporting"] as? String ?: default.randomTp.teleporting,
                coordinates = rtpData["coordinates"] as? String ?: default.randomTp.coordinates,
                completed = rtpData["completed"] as? String ?: default.randomTp.completed,
                location = rtpData["location"] as? String ?: default.randomTp.location,
                searching = rtpData["searching"] as? String ?: default.randomTp.searching,
                noSafeLocation = rtpData["no-safe-location"] as? String ?: default.randomTp.noSafeLocation,
                disabled = rtpData["disabled"] as? String ?: default.randomTp.disabled
            ),
            tpa = TpaMessages(
                usage = tpaData["usage"] as? String ?: default.tpa.usage,
                hereUsage = tpaData["here-usage"] as? String ?: default.tpa.hereUsage,
                sent = tpaData["sent"] as? String ?: default.tpa.sent,
                hereSent = tpaData["here-sent"] as? String ?: default.tpa.hereSent,
                received = tpaData["received"] as? String ?: default.tpa.received,
                hereReceived = tpaData["here-received"] as? String ?: default.tpa.hereReceived,
                buttonAcceptText = tpaData["button-accept-text"] as? String ?: default.tpa.buttonAcceptText,
                buttonAcceptHover = tpaData["button-accept-hover"] as? String ?: default.tpa.buttonAcceptHover,
                buttonDenyText = tpaData["button-deny-text"] as? String ?: default.tpa.buttonDenyText,
                buttonDenyHover = tpaData["button-deny-hover"] as? String ?: default.tpa.buttonDenyHover,
                buttonSeparator = tpaData["button-separator"] as? String ?: default.tpa.buttonSeparator,
                accepted = tpaData["accepted"] as? String ?: default.tpa.accepted,
                denied = tpaData["denied"] as? String ?: default.tpa.denied,
                requestAccepted = tpaData["request-accepted"] as? String ?: default.tpa.requestAccepted,
                requestDenied = tpaData["request-denied"] as? String ?: default.tpa.requestDenied,
                teleporting = tpaData["teleporting"] as? String ?: default.tpa.teleporting,
                teleported = tpaData["teleported"] as? String ?: default.tpa.teleported,
                noPending = tpaData["no-pending"] as? String ?: default.tpa.noPending,
                playerNotFound = tpaData["player-not-found"] as? String ?: default.tpa.playerNotFound,
                cannotSelf = tpaData["cannot-self"] as? String ?: default.tpa.cannotSelf,
                disabled = tpaData["disabled"] as? String ?: default.tpa.disabled,
                targetDisabled = tpaData["target-disabled"] as? String ?: default.tpa.targetDisabled,
                requesterOffline = tpaData["requester-offline"] as? String ?: default.tpa.requesterOffline,
                expired = tpaData["expired"] as? String ?: default.tpa.expired
            )
        )
    }
}

// 플레이스홀더 치환 유틸리티
fun String.replacePlaceholders(vararg pairs: Pair<String, Any>): String {
    var result = this
    for ((key, value) in pairs) {
        result = result.replace("{$key}", value.toString())
    }
    return result
}