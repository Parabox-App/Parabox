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
}