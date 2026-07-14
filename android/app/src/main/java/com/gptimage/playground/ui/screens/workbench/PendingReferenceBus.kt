package com.gptimage.playground.ui.screens.workbench

import com.gptimage.playground.data.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 跨页面传输 [HistoryItem] 的轻量总线。
 *
 * 用于「相册 → 用作参考图 / 发送到编辑」流程：
 * - [AlbumScreen] 在用户点击「用作参考图」或「发送到编辑」时调用 [request]
 * - [WorkbenchScreen] 启动时订阅 [pending]，消费掉 pending item 后调用 [consume]
 *
 * 这样避免了把 [WorkbenchViewModel] 提升到 NavGraph 共享的复杂改造。
 */
class PendingReferenceBus {

    /** 待处理的项 + 是否同时把 prompt 也带回工作台（send-to-edit=true / use-as-reference=false）。 */
    private val _pending = MutableStateFlow<PendingReference?>(null)
    val pending: StateFlow<PendingReference?> = _pending.asStateFlow()

    fun request(item: HistoryItem, sendToEdit: Boolean) {
        _pending.value = PendingReference(item, sendToEdit)
    }

    fun consume() {
        _pending.value = null
    }

    data class PendingReference(val item: HistoryItem, val sendToEdit: Boolean)
}
