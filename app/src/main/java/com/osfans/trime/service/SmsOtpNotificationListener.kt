/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.service

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.osfans.trime.data.otp.OtpManager
import com.osfans.trime.data.otp.OtpParser
import com.osfans.trime.data.prefs.AppPrefs
import timber.log.Timber

/**
 * 读取短信通知、提取验证码并交给 [OtpManager]。
 *
 * 使用「通知使用权」而非短信权限：
 * - 无需 `READ_SMS` / `RECEIVE_SMS`，规避商店对短信权限的限制；
 * - 兼容各厂商短信应用，只要通知里包含验证码文本即可。
 *
 * 用户未授予通知使用权时系统不会绑定本服务，此处不会被调用。
 */
class SmsOtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val notification = statusBarNotification.notification ?: return
        val extras = notification.extras ?: return
        val enabled =
            runCatching { AppPrefs.defaultInstance().otp.otpEnabled.getValue() }.getOrDefault(false)
        if (!enabled) return
        val text = collectText(extras)
        if (text.isEmpty()) return
        if (!isMessageLike(notification, extras, text)) return
        val code = OtpParser.extract(text) ?: return
        Timber.d("Verification code detected from %s", statusBarNotification.packageName)
        OtpManager.submitCode(code)
    }

    private fun collectText(extras: Bundle): String = buildString {
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { append(it).append('\n') }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { append(it).append('\n') }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { append(it).append('\n') }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { append(it) }
    }

    /**
     * 判断一条通知是否值得解析。优先看消息类通知的标记，其次看文本里有没有明确的关键词，
     * 尽量不误伤普通应用推送。
     */
    private fun isMessageLike(
        notification: Notification,
        extras: Bundle,
        text: String,
    ): Boolean {
        if (notification.category == Notification.CATEGORY_MESSAGE) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notification.channelId
            if (channel != null && channel.contains("sms", ignoreCase = true)) return true
        }
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        if (template.contains("MessagingStyle")) return true
        return OtpParser.keywordRegex.containsMatchIn(text)
    }
}
