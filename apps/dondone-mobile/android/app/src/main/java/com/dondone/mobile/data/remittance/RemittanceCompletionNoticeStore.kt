package com.dondone.mobile.data.remittance

import android.content.Context
import android.content.SharedPreferences

private const val REMITTANCE_NOTICE_PREFS_NAME = "dondone.remittance.notice"
private const val KEY_DISMISSED_TRANSFER_ID_PREFIX = "dismissed_transfer_id_"

interface RemittanceCompletionNoticeStore {
    fun readDismissedTransferId(userId: Long): String?
    fun saveDismissedTransferId(userId: Long, transferId: String)
    fun clear(userId: Long)
    fun clearAll()
}

class SharedPreferencesRemittanceCompletionNoticeStore(
    context: Context
) : RemittanceCompletionNoticeStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(REMITTANCE_NOTICE_PREFS_NAME, Context.MODE_PRIVATE)

    override fun readDismissedTransferId(userId: Long): String? =
        preferences.getString(keyFor(userId), null)

    override fun saveDismissedTransferId(userId: Long, transferId: String) {
        preferences.edit().putString(keyFor(userId), transferId).apply()
    }

    override fun clear(userId: Long) {
        preferences.edit().remove(keyFor(userId)).apply()
    }

    override fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun keyFor(userId: Long): String = "$KEY_DISMISSED_TRANSFER_ID_PREFIX$userId"
}

class InMemoryRemittanceCompletionNoticeStore : RemittanceCompletionNoticeStore {
    private val dismissedTransferIds = mutableMapOf<Long, String>()

    override fun readDismissedTransferId(userId: Long): String? = dismissedTransferIds[userId]

    override fun saveDismissedTransferId(userId: Long, transferId: String) {
        dismissedTransferIds[userId] = transferId
    }

    override fun clear(userId: Long) {
        dismissedTransferIds.remove(userId)
    }

    override fun clearAll() {
        dismissedTransferIds.clear()
    }
}
