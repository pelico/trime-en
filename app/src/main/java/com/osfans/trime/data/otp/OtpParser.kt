/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.otp

/**
 * 从通知或剪贴板文本中提取短信验证码。
 *
 * 为尽量避免把价格、电话号码等普通数字误判成验证码，只接受 4-8 位数字，且必须满足：
 * - 整段文本本身就是一个纯数字验证码；或
 * - 数字紧跟在「验证码」等关键词之后。
 */
object OtpParser {
    private val pureCode = Regex("^\\d{4,8}$")

    private val keywordCode = Regex(
        "(?:验证码|校验码|动态码|短信码|verification\\s*code|one[\\s-]*time\\s*(?:code|password)|\\botp\\b|code)" +
            "[^0-9]{0,8}(\\d{4,8})",
        RegexOption.IGNORE_CASE,
    )

    /** 验证码关键词，用于判断一条通知是否值得解析。 */
    val keywordRegex: Regex = Regex(
        "验证码|校验码|动态码|短信码|verification\\s*code|one[\\s-]*time\\s*(?:code|password)|\\botp\\b",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String?): String? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        pureCode.find(trimmed)?.let { return it.value }
        return keywordCode.find(trimmed)?.groupValues?.getOrNull(1)
    }
}
