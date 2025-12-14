package com.heulim.nextwarp.common.config

data class MessageConfig(
    val prefix: String = "§7[§bNextWarp§7] ",
    val noPermission: String = "§c권한이 없습니다.",
    val playerOnly: String = "§c이 명령어는 플레이어만 사용할 수 있습니다.",
    val notConnected: String = "§c서버에 연결되어 있지 않습니다.",
    val worldNotFound: String = "§c월드를 찾을 수 없습니다.",
    val serverNotFound: String = "§c서버 '{server}'을(를) 찾을 수 없습니다.",
    val cooldown: String = "§c{seconds}초 후에 다시 사용할 수 있습니다.",

    // Warp messages
    val warp: WarpMessages = WarpMessages(),

    // RandomTP messages
    val randomTp: RandomTpMessages = RandomTpMessages(),

    // TPA messages
    val tpa: TpaMessages = TpaMessages(),

    // Spawn messages
    val spawn: SpawnMessages = SpawnMessages()
)

data class WarpMessages(
    val teleporting: String = "§a워프 '{name}'(으)로 이동합니다...",
    val teleported: String = "§a워프 위치로 텔레포트되었습니다.",
    val notFound: String = "§c워프 '{name}'을(를) 찾을 수 없습니다.",
    val setRequest: String = "§a워프 '{name}' 설정 요청을 보냈습니다.",
    val set: String = "§a워프 '{name}'이(가) 설정되었습니다.",
    val setLocation: String = "§7위치: {world} [{x}, {y}, {z}]",
    val deleted: String = "§a워프 '{name}'이(가) 삭제되었습니다.",
    val deleteNotFound: String = "§c워프 '{name}'을(를) 찾을 수 없습니다.",
    val listEmpty: String = "§e등록된 워프가 없습니다.",
    val listHeader: String = "§6=== 워프 목록 ===",
    val listEntry: String = "§7- {name} ({server}) [{world}: {x}, {y}, {z}]",
    val helpHeader: String = "§6=== NextWarp 도움말 ===",
    val helpWarp: String = "§7/warp <이름> - 워프로 이동",
    val helpSet: String = "§7/warp set <이름> - 현재 위치에 워프 설정",
    val helpDelete: String = "§7/warp delete <이름> - 워프 삭제",
    val helpList: String = "§7/warp list - 워프 목록 보기",
    val usageSet: String = "§c사용법: /warp set <이름>"
)

data class RandomTpMessages(
    val teleporting: String = "§a랜덤 텔레포트 중...",
    val coordinates: String = "§7위치: X={x}, Z={z}",
    val completed: String = "§a랜덤 텔레포트 완료!",
    val location: String = "§7위치: {world} [{x}, {y}, {z}]",
    val searching: String = "§e안전한 위치를 찾는 중...",
    val noSafeLocation: String = "§c안전한 위치를 찾을 수 없습니다. 다시 시도해주세요.",
    val disabled: String = "§c이 서버에서는 랜덤 텔레포트가 비활성화되어 있습니다."
)

data class TpaMessages(
    val usage: String = "§c사용법: /tpa <플레이어>",
    val hereUsage: String = "§c사용법: /tpahere <플레이어>",
    val sent: String = "§a{player}님에게 텔레포트 요청을 보냈습니다.",
    val hereSent: String = "§a{player}님에게 이곳으로 오라는 요청을 보냈습니다.",
    val received: String = "§a{player}님이 텔레포트 요청을 보냈습니다.",
    val hereReceived: String = "§a{player}님이 자신에게 오라고 요청했습니다.",
    // 버튼 설정
    val buttonAcceptText: String = "§a§l[수락]",
    val buttonAcceptHover: String = "§7클릭하여 텔레포트 요청을 수락합니다.",
    val buttonDenyText: String = "§c§l[거절]",
    val buttonDenyHover: String = "§7클릭하여 텔레포트 요청을 거절합니다.",
    val buttonSeparator: String = " §7| ",
    val accepted: String = "§a{player}님의 텔레포트 요청을 수락했습니다.",
    val denied: String = "§c{player}님의 텔레포트 요청을 거절했습니다.",
    val requestAccepted: String = "§a{player}님이 텔레포트 요청을 수락했습니다.",
    val requestDenied: String = "§c{player}님이 텔레포트 요청을 거절했습니다.",
    val teleporting: String = "§a{player}님에게 텔레포트 중...",
    val teleported: String = "§a텔레포트 완료!",
    val noPending: String = "§c대기 중인 텔레포트 요청이 없습니다.",
    val playerNotFound: String = "§c플레이어 '{player}'을(를) 찾을 수 없습니다.",
    val cannotSelf: String = "§c자기 자신에게 텔레포트할 수 없습니다.",
    val disabled: String = "§c이 서버에서는 TPA가 비활성화되어 있습니다.",
    val targetDisabled: String = "§c{player}님이 있는 서버에서는 TPA가 비활성화되어 있습니다.",
    val requesterOffline: String = "§c{player}님이 오프라인입니다.",
    val expired: String = "§c텔레포트 요청이 만료되었습니다."
)

data class SpawnMessages(
    val teleporting: String = "§a스폰으로 이동합니다...",
    val teleported: String = "§a스폰으로 텔레포트되었습니다.",
    val notSet: String = "§c스폰이 설정되지 않았습니다.",
    val setRequest: String = "§a스폰 설정 요청을 보냈습니다.",
    val set: String = "§a스폰이 설정되었습니다.",
    val setLocation: String = "§7위치: {world} [{x}, {y}, {z}]",
    val respawn: String = "§a스폰 지점으로 리스폰합니다."
)