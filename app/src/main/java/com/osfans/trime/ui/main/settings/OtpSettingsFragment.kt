/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ui.main.settings

import android.content.Intent
import android.provider.Settings
import androidx.preference.PreferenceScreen
import com.osfans.trime.R
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.prefs.PreferenceDelegateFragment
import com.osfans.trime.util.addPreference

/**
 * 验证码助手设置页。
 *
 * 除了开关外，提供一个跳转到系统「通知使用权」设置的入口：
 * 只有授予通知使用权后，键盘才能读取短信通知里的验证码。
 */
class OtpSettingsFragment : PreferenceDelegateFragment(AppPrefs.defaultInstance().otp) {
    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        screen.addPreference(
            R.string.otp_notification_access,
            R.string.otp_notification_access_summary,
            R.drawable.ic_baseline_lock_24,
        ) {
            runCatching {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }
}
