package com.aderan.android.rtmbench.entity

import com.aderan.android.rtmbench.ClassModeType
import com.aderan.android.rtmbench.RoomStatus

data class RoomState(
    val roomType: RoomType = RoomType.SmallClass,
    // 房间状态
    val roomStatus: RoomStatus = RoomStatus.Idle,
    //
    val region: String = "",
    // 房间所有者
    val ownerUUID: String = "",

    // 房间所有者的名称
    val ownerName: String? = null,
    // 房间标题
    val title: String? = null,
    // 房间开始时间
    val beginTime: Long = 0L,
    // 结束时间
    val endTime: Long = 0L,
    // 禁用
    val ban: Boolean = false,
    // 交互模式
    val classMode: ClassModeType = ClassModeType.Interaction,
)