package com.ojhdtapp.parabox.domain.built_in.onebot11.util

class CompatibilityUtil(val enabled: Boolean = false) {
    fun getGroupAvatar(groupId: Long, size: Int): String? {
        if (!enabled) {
            return null
        }
        return String.format("https://p.qlogo.cn/gh/%s/%s/%s", groupId, groupId, size)
    }

    fun getUserAvatar(userId: Long, size: Int): String? {
        if (!enabled) {
            return null
        }
        return String.format("https://q2.qlogo.cn/headimg_dl?dst_uin=%s&spec=%s", userId, size)
    }

    fun queryFace(id: Int): String? {
        return when (id) {
            0 -> "😲"
            1 -> "😖"
            2 -> "😍"
            3 -> "😶"
            4 -> "😎"
            5 -> "😭"
            6 -> "😳"
            7 -> "🤐"
            8 -> "😴"
            9 -> "😢"
            10 -> "😟"
            11 -> "😡"
            12 -> "🤪"
            13 -> "😁"
            14 -> "😊"
            15 -> "☹️"
            16 -> "😎"
            96 -> "😓"
            18 -> "😫"
            19 -> "🤮"
            20 -> "🤭"
            21 -> "😊"
            22 -> "🙄"
            23 -> "😤"
            24 -> "🥴"
            25 -> "😪"
            26 -> "😲"
            27 -> "😓"
            28 -> "😄"
            29 -> "😙"
            30 -> "✊"
            31 -> "🤬"
            32 -> "😕"
            33 -> "🤫"
            34 -> "😵‍💫"
            35 -> "😣"
            36 -> "🤯"
            37 -> "💀"
            38 -> "😡"
            39 -> "👋"
            40 -> "😑"
            97 -> "😅"
            98 -> "😪"
            99 -> "👏"
            100 -> "😓"
            101 -> "😁"
            102 -> "😤"
            103 -> "😤"
            104 -> "🥱"
            105 -> "😒"
            106 -> "😟"
            107 -> "😞"
            108 -> "🥸"
            109 -> "😙"
            110 -> "😲"
            111 -> "🥺"
            172 -> "😜"
            182 -> "😂"
            179 -> "😊"
            173 -> "😭"
            174 -> "😛"
            212 -> "😶"
            175 -> "😛"
            178 -> "😆"
            177 -> "🤢"
            180 -> "😃"
            181 -> "😐"
            176 -> "😊"
            183 -> "😝"
            293 -> "🐟"
            else -> null
        }
    }
}