package com.aderan.android.rtmbench

import com.aderan.android.rtmbench.entity.RtcUser

class ClientUserState(private val currentUserId: String) {
    val users = HashMap<String, RtcUser>()

    fun addUser(userId: String) {
        if (!users.containsKey(userId)) {
            users[userId] = RtcUser(userUUID = userId)
        }
    }

    fun removeUser(userId: String) {
        users.remove(userId)
    }

    fun updateUserState(
        userId: String,
        name: String,
        audioOpen: Boolean,
        videoOpen: Boolean,
        isSpeak: Boolean
    ) {
        if (!users.containsKey(userId)) {
            addUser(userId)
        }
        users[userId]?.run {
            users[userId] = copy(
                name = name,
                audioOpen = audioOpen,
                videoOpen = videoOpen,
                isSpeak = isSpeak,
            )
        }
    }

    fun initUsers(uuids: List<String>) {
        uuids.forEach {
            addUser(it)
        }
    }

    fun findOtherUser(): RtcUser? {
        return users.values.find {
            it.userUUID != currentUserId
        }
    }
}