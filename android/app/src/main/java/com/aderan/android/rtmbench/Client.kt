package com.aderan.android.rtmbench

import android.content.Context
import android.util.Log
import com.aderan.android.rtmbench.entity.RoomState
import com.aderan.android.rtmbench.misc.EmptyClientListener
import com.aderan.android.rtmbench.misc.EmptyRtmChannelListener
import io.agora.rtm.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Client constructor(private val userId: String, private val rtmToken: String) {
    companion object {
        val TAG = Client::class.simpleName

        const val CONNECTION_CHANGE_REASON_REMOTE_LOGIN = 8
    }

    private var state = RoomState()
    private var userState = ClientUserState(userId)

    private lateinit var rtmClient: RtmClient

    private val rtmClientListener = object : EmptyClientListener() {
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Connection state changes to $state reason:$reason")
            if (reason == CONNECTION_CHANGE_REASON_REMOTE_LOGIN) {

            }
        }

        override fun onMessageReceived(message: RtmMessage, peerId: String) {
            Log.d(TAG, "Message received from $peerId ${message.text}")
            val event = RTMEvent.parseRTMEvent(message.text)
        }
    }

    fun init(context: Context) {
        try {
            rtmClient = RtmClient.createInstance(context, Constants.RTM_APP_ID, rtmClientListener)
        } catch (e: Exception) {
            Log.w(TAG, "RTM SDK init fatal error!")
        }
    }

    private var messageListener = object : EmptyRtmChannelListener() {
        override fun onMemberJoined(member: RtmChannelMember) {
            userState.addUser(member.userId)
        }

        override fun onMemberLeft(member: RtmChannelMember) {
            userState.removeUser(member.userId)
        }

        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            super.onMessageReceived(message, member)
            Log.d(TAG, "messageListener received from ${member.userId} ${message.text}")
        }
    }

    private var commandListener = object : EmptyRtmChannelListener() {
        override fun onMessageReceived(message: RtmMessage, member: RtmChannelMember) {
            super.onMessageReceived(message, member)
            // Log.d(TAG, "commandListener received from ${member.userId} ${message.text}")
            when (val event = RTMEvent.parseRTMEvent(message.text)) {
                is RTMEvent.AcceptRaiseHand -> {}
                is RTMEvent.AllOffStage -> {}
                is RTMEvent.BanText -> {

                }
                is RTMEvent.CancelHandRaising -> {}
                is RTMEvent.ChannelMessage -> {}
                is RTMEvent.ChannelStatus -> {}
                is RTMEvent.ClassMode -> {}
                is RTMEvent.DeviceState -> {}
                is RTMEvent.Notice -> {}
                is RTMEvent.RaiseHand -> {}
                is RTMEvent.RequestChannelStatus -> {
                    val state = event.value.user
                    userState.updateUserState(
                        member.userId,
                        name = state.name,
                        audioOpen = state.mic,
                        videoOpen = state.camera,
                        isSpeak = state.isSpeak,
                    )
                    if (event.value.userUUIDs.contains(userId)) {
                        sendChannelStatus(member.userId)
                    }
                }
                is RTMEvent.RoomStatusRemote -> {}
                is RTMEvent.Speak -> {}
                else -> {}
            }
        }
    }

    private fun sendChannelStatus(senderId: String) {
        val uStates = HashMap<String, String>()
        userState.users.values.forEach {
            uStates[it.userUUID] = StringBuilder().apply {
                if (it.isSpeak) append(RTMUserProp.IsSpeak.flag)
                if (it.isRaiseHand) append(RTMUserProp.IsRaiseHand.flag)
                if (it.videoOpen) append(RTMUserProp.Camera.flag)
                if (it.audioOpen) append(RTMUserProp.Mic.flag)
            }.toString()
        }
        val channelState = RTMEvent.ChannelStatus(
            ChannelStatusValue(
                ban = state.ban,
                rStatus = state.roomStatus,
                rMode = state.classMode,
                uStates = uStates
            )
        )
        sendPeerCommand(channelState, senderId)
    }

    suspend fun initChannel(channelId: String): Boolean {
        channelMessageID = channelId
        channelCommandID = channelId + "commands"

        login(rtmToken, userId)
        channelMessage = joinChannel(channelMessageID, messageListener)
        channelCommand = joinChannel(channelCommandID, commandListener)

        return true
    }

    private suspend fun login(token: String, userId: String): Boolean = suspendCoroutine { cont ->
        rtmClient.login(token, userId, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(RuntimeException(e.toString()))
            }
        })
    }

    suspend fun logout(): Boolean = suspendCoroutine { cont ->
        rtmClient.logout(object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(e: ErrorInfo) {
                cont.resumeWithException(RuntimeException(e.toString()))
            }
        })
    }

    private lateinit var channelMessageID: String
    private lateinit var channelCommandID: String
    private var channelMessage: RtmChannel? = null
    private var channelCommand: RtmChannel? = null

    private suspend fun joinChannel(channelId: String, listener: RtmChannelListener): RtmChannel =
        suspendCoroutine {
            val channel = rtmClient.createChannel(channelId, listener)
            channel.join(object : ResultCallback<Void?> {
                override fun onSuccess(v: Void?) {
                    Log.d(TAG, "join $channelId onSuccess")
                    it.resume(channel)
                }

                override fun onFailure(e: ErrorInfo) {
                    Log.d(TAG, "join $channelId onFailure")
                    it.resumeWithException(RuntimeException(e.toString()))
                }
            })
        }

    private suspend fun getMembers(): List<RtmChannelMember> = suspendCoroutine { cont ->
        channelMessage?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(members: List<RtmChannelMember>) {
                Log.d(TAG, "member $members")
                cont.resume(members)
            }

            override fun onFailure(e: ErrorInfo) {
                Log.d(TAG, "onFailure $e")
                cont.resume(listOf())
            }
        }) ?: cont.resume(listOf())
    }

    private var sendMessageOptions = SendMessageOptions().apply {
        enableHistoricalMessaging = true
    }

    suspend fun sendChannelMessage(msg: String): Boolean = suspendCoroutine { cont ->
        val message = rtmClient.createMessage()
        message.text = msg

        channelMessage?.sendMessage(message, sendMessageOptions, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {
                cont.resume(true)
            }

            override fun onFailure(errorIn: ErrorInfo) {
                cont.resume(false)
            }
        }) ?: cont.resume(false)
    }

    suspend fun sendChannelCommand(event: RTMEvent) = suspendCoroutine<Boolean> { cont ->
        run {
            val message = rtmClient.createMessage()
            message.text = RTMEvent.toText(event)

            channelCommand?.sendMessage(
                message,
                sendMessageOptions,
                object : ResultCallback<Void?> {
                    override fun onSuccess(v: Void?) {
                        cont.resume(true)
                    }

                    override fun onFailure(error: ErrorInfo) {
                        cont.resume(false)
                    }
                }) ?: cont.resume(false)
        }
    }

    private fun sendPeerCommand(event: RTMEvent, peerId: String) {
        event.r = channelCommandID

        val message = rtmClient.createMessage()
        message.text = RTMEvent.toText(event)

        val option = SendMessageOptions().apply {
            enableOfflineMessaging = true
        }

        rtmClient.sendMessageToPeer(peerId, message, option, object : ResultCallback<Void?> {
            override fun onSuccess(v: Void?) {

            }

            override fun onFailure(error: ErrorInfo) {

            }
        })
    }

    suspend fun initChannelStatus() {
        userState.initUsers(uuids = getMembers().map { it.userId })
        // update local user state
        userState.updateUserState(
            userId = userId,
            audioOpen = false,
            videoOpen = false,
            name = currentUsername(),
            isSpeak = false
        )
        userState.findOtherUser()?.run {
            val state = RTMUserState(
                name = currentUsername(),
                camera = false,
                mic = false,
                isSpeak = false,
            )

            val event = RTMEvent.RequestChannelStatus(
                RequestChannelStatusValue(channelMessageID, listOf(userId), state)
            )

            sendChannelCommand(event)
        }
    }

    private fun currentUsername(): String {
        return "fake_${userId.substring(userId.length - 5)}"
    }
}