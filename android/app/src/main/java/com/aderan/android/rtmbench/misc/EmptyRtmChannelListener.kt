package com.aderan.android.rtmbench.misc

import io.agora.rtm.*

open class EmptyRtmChannelListener : RtmChannelListener {
    override fun onMemberCountUpdated(p0: Int) {
    }

    override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>) {
    }

    override fun onMessageReceived(p0: RtmMessage, p1: RtmChannelMember) {
    }

    override fun onImageMessageReceived(p0: RtmImageMessage, p1: RtmChannelMember) {
    }

    override fun onFileMessageReceived(p0: RtmFileMessage, p1: RtmChannelMember) {
    }

    override fun onMemberJoined(p0: RtmChannelMember) {

    }

    override fun onMemberLeft(p0: RtmChannelMember) {
    }
}