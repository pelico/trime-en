/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.otp

import android.content.ClipData
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.util.WeakHashSet
import splitties.systemservices.clipboardManager
import timber.log.Timber

/**
 * 验证码状态中心。
 *
 * 由 [com.osfans.trime.service.SmsOtpNotificationListener] 或剪贴板监听写入，
 * 输入栏注册监听后读取并展示「一键填入」提示。
 *
 * 进程内单例：通知监听服务与输入法运行在同一个应用进程，可直接共享。
 */
object OtpManager {
    fun interface OnCodeUpdateListener {
        fun onUpdate(code: String)
    }

    private const val DEDUP_WINDOW = 60_000L

    private val listeners = WeakHashSet<OnCodeUpdateListener>()

    /** 最近一次识别到的验证码，null 表示已消费或没有。 */
    @Volatile
    var lastCode: String? = null
        private set

    private var lastSubmitCode: String? = null
    private var lastSubmitTime: Long = 0L

    fun addOnCodeUpdateListener(listener: OnCodeUpdateListener) {
        listeners.add(listener)
    }

    fun removeOnCodeUpdateListener(listener: OnCodeUpdateListener) {
        listeners.remove(listener)
    }

    /**
     * 提交一个识别到的验证码。
     *
     * 同一验证码在 [DEDUP_WINDOW] 内重复提交会被忽略（通知可能被反复投递）。
     */
    fun submitCode(code: String) {
        if (code.isEmpty()) return
        val now = System.currentTimeMillis()
        if (code == lastSubmitCode && now - lastSubmitTime < DEDUP_WINDOW) return
        lastSubmitCode = code
        lastSubmitTime = now
        lastCode = code
        if (AppPrefs.defaultInstance().otp.otpAutoCopy.getValue()) {
            copyToClipboard(code)
        }
        listeners.forEach { it.onUpdate(code) }
    }

    /** 消费掉当前验证码，避免提示被重复展示。 */
    fun consume() {
        lastCode = null
    }

    private fun copyToClipboard(code: String) {
        try {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("otp", code))
        } catch (e: Exception) {
            Timber.w(e, "Failed to copy verification code to clipboard")
        }
    }
}
