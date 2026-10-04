/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.popup

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.osfans.trime.core.CandidateProto
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.util.sp
import splitties.dimensions.dp
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.textView

class LabeledCandidateItemUi(
    override val ctx: Context,
    private val scope: ThemeScope,
) : Ui {
    private val theme: Theme
        get() = scope.theme

    private val labelSize = theme.window.foreground.labelFontSize
    private val textSize = theme.window.foreground.textFontSize
    private val commentSize = theme.window.foreground.commentFontSize

    /** 英文翻译字号：略小于 comment 字号，节省空间同时保证可读性 */
    private val englishSize = (commentSize * 0.85f).coerceAtLeast(8f)
    private val labelFont = theme.fonts.label
    private val textFont = theme.fonts.candidate
    private val commentFont = theme.fonts.comment

    // Read at use time so a scheme switch re-binds rows with the new colors.
    private val labelColor: Int get() = scope.colors.labelColor
    private val textColor: Int get() = scope.colors.candidateTextColor
    private val commentColor: Int get() = scope.colors.commentTextColor
    private val highlightLabelColor: Int get() = scope.colors.hilitedLabelColor
    private val highlightCommentTextColor: Int get() = scope.colors.hilitedCommentTextColor
    private val highlightCandidateTextColor: Int get() = scope.colors.hilitedCandidateTextColor
    private val highlightCandidateBackColor: Int get() = scope.colors.hilitedCandidateBackColor

    override val root =
        textView {
            val v = dp(theme.window.itemPadding.vertical)
            val h = dp(theme.window.itemPadding.horizontal)
            setPadding(h, v, h, v)
        }

    private inline fun SpannableStringBuilder.inSpanWith(
        @ColorInt color: Int,
        textSize: Float,
        typeface: Typeface,
        builderAction: SpannableStringBuilder.() -> Unit,
    ) = inSpans(CandidateItemSpan(color, textSize, typeface), builderAction)

    /**
     * 构建候选词显示内容：
     * - 第一行（可选）：英文翻译，小号 comment 颜色字体
     * - 第二行：label + text + comment（原有逻辑保持不变）
     *
     * 单行布局通过 '\n' 实现多行显示，FlexboxLayoutManager 高度自适应。
     * 开启英文翻译后所有行都会保留该行（无翻译用空格占位），因此高度一致。
     */
    fun update(
        candidate: CandidateProto,
        highlighted: Boolean,
        englishText: String? = null,
    ) {
        val labelFg = if (highlighted) highlightLabelColor else labelColor
        val textFg = if (highlighted) highlightCandidateTextColor else textColor
        val commentFg = if (highlighted) highlightCommentTextColor else commentColor
        root.text =
            buildSpannedString {
                // 英文翻译行：开启该功能时始终占位（无翻译用空格），保证各行高度一致
                if (AppPrefs.defaultInstance().candidates.showEnglishTranslation.getValue()) {
                    inSpanWith(commentFg, ctx.sp(englishSize), commentFont) {
                        append(englishText?.takeIf { it.isNotBlank() } ?: " ")
                    }
                    append("\n")
                }
                // 原有 label + text + comment 布局（保持不变）
                inSpanWith(labelFg, ctx.sp(labelSize), labelFont) { append(candidate.label) }
                append(" ")
                inSpanWith(textFg, ctx.sp(textSize), textFont) { append(candidate.text) }
                if (candidate.comment.isNotBlank()) {
                    append(" ")
                    inSpanWith(commentFg, ctx.sp(commentSize), commentFont) { append(candidate.comment) }
                }
            }
        val bg =
            GradientDrawable().apply {
                if (highlighted) {
                    setColor(highlightCandidateBackColor)
                    cornerRadius = ctx.dp(theme.style.candidateCornerRadius)
                } else {
                    setColor(Color.TRANSPARENT)
                }
            }
        root.background = bg
    }
}
