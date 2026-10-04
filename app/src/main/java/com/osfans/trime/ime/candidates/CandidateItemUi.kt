/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.osfans.trime.core.CandidateProto
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.data.theme.model.GeneralStyle
import com.osfans.trime.ime.core.AutoScaleTextView
import com.osfans.trime.ime.keyboard.GestureFrame
import com.osfans.trime.util.roundedRippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.baselineToBaselineOf
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.bottomToTopOf
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerInParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.endToStartOf
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.startToEndOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.constraintlayout.topToBottomOf
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.horizontalPadding

class CandidateItemUi(
    override val ctx: Context,
    private val scope: ThemeScope,
) : Ui {
    private val theme: Theme
        get() = scope.theme

    private val textSize = theme.style.candidateTextSize
    private val commentSize = theme.style.commentTextSize

    /** 英文翻译字号：略小于 comment 字号，节省空间同时保证可读性 */
    private val englishSize = (commentSize * 0.85f).coerceAtLeast(8f)

    /** 候选项默认高度（与改动前一致）。 */
    private val candidateHeight = ctx.dp(theme.style.candidateViewHeight)

    /**
     * 显示英文翻译时使用的高度。
     * 输入栏（bar）预留的高度本就是 `candidate_view_height + comment_height`，
     * 因此这里可以直接借用编码提示区的那部分空间，不会造成裁剪。
     */
    private val expandedHeight = ctx.dp(theme.style.candidateViewHeight + theme.style.commentHeight)

    // Read at use time so a scheme switch re-binds rows with the new colors.
    private val textColor: Int get() = scope.colors.candidateTextColor
    private val commentColor: Int get() = scope.colors.commentTextColor
    private val hlCommentColor: Int get() = scope.colors.hilitedCommentTextColor
    private val hlTextColor: Int get() = scope.colors.hilitedCandidateTextColor
    private val hlBackColor: Int get() = scope.colors.hilitedCandidateBackColor

    private val commentPosition = theme.style.commentPosition
    private val commentVerticalBias = theme.style.commentVerticalBias
    private val candidateTextVerticalBias = theme.style.candidateTextVerticalBias

    private val text =
        view(::AutoScaleTextView) {
            id = View.generateViewId()
            this.textSize = this@CandidateItemUi.textSize
            typeface = theme.fonts.candidate
            isSingleLine = true
            gravity = gravityCenter
            scaleMode = AutoScaleTextView.Mode.Proportional
        }

    private val comment =
        view(::AutoScaleTextView) {
            id = View.generateViewId()
            this.textSize = commentSize
            typeface = theme.fonts.comment
            isSingleLine = true
            gravity = gravityCenter
            scaleMode = AutoScaleTextView.Mode.Proportional
        }

    /**
     * 英文翻译行。布局始终包含此视图，但默认 [View.GONE]，
     * 此时 ConstraintLayout 会将其视为零高度并保留其它视图的原始位置，
     * 因此在没有翻译时布局与改动前完全一致。
     */
    private val english =
        view(::AutoScaleTextView) {
            id = View.generateViewId()
            this.textSize = this@CandidateItemUi.englishSize
            typeface = theme.fonts.comment
            isSingleLine = true
            gravity = gravityCenter
            scaleMode = AutoScaleTextView.Mode.Proportional
            visibility = View.GONE
        }

    private val content = constraintLayout {
        horizontalPadding = dp(theme.style.candidatePadding)
        when (commentPosition) {
            GeneralStyle.CommentPosition.RIGHT -> {
                // 英文行固定在最上方，候选项（text + comment）在剩余空间垂直居中：
                // english 为 GONE 时其高度为 0，text 的居中效果与改动前一致。
                add(
                    english,
                    lParams(wrapContent, wrapContent) {
                        topOfParent()
                        centerHorizontally()
                    },
                )
                add(
                    text,
                    lParams(wrapContent, matchConstraints) {
                        topToBottomOf(english)
                        bottomOfParent()
                        verticalBias = 0.5f
                        startOfParent()
                        endToStartOf(comment)
                        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
                    },
                )
                add(
                    comment,
                    lParams(wrapContent, wrapContent) {
                        startToEndOf(text, ctx.dp(1))
                        endOfParent()
                        baselineToBaselineOf(text)
                        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
                    },
                )
            }

            GeneralStyle.CommentPosition.TOP -> {
                // 自上而下：comment（编码提示）→ english → text。
                add(
                    comment,
                    lParams(wrapContent, matchConstraints) {
                        matchConstraintPercentHeight = 0.4f
                        topOfParent()
                        centerHorizontally()
                        bottomToTopOf(english)
                    },
                )
                add(
                    english,
                    lParams(wrapContent, wrapContent) {
                        centerHorizontally()
                        topToBottomOf(comment)
                        bottomToTopOf(text)
                    },
                )
                add(
                    text,
                    lParams(wrapContent, matchConstraints) {
                        centerHorizontally()
                        bottomOfParent()
                        topToBottomOf(english)
                    },
                )
            }

            GeneralStyle.CommentPosition.OVERLAY -> {
                // OVERLAY 模式下面板高度固定，英文行紧贴候选词上方叠加显示。
                add(
                    english,
                    lParams(wrapContent, wrapContent) {
                        centerHorizontally()
                        bottomToTopOf(text)
                    },
                )
                add(
                    text,
                    lParams(wrapContent, wrapContent) {
                        centerInParent()
                        verticalBias = candidateTextVerticalBias
                    },
                )
                add(
                    comment,
                    lParams(wrapContent, wrapContent) {
                        centerInParent()
                        verticalBias = commentVerticalBias
                    },
                )
            }
        }
    }

    /**
     * candidate long press feedback is handled by `showCandidateActionMenu`
     */
    override val root = view(::GestureFrame) {
        add(
            content,
            lParams(wrapContent, dp(theme.style.candidateViewHeight)) {
                gravity = gravityCenter
            },
        )
    }

    @SuppressLint("UseKtx")
    fun update(
        item: CandidateProto,
        highlighted: Boolean,
        englishText: String? = null,
    ) {
        val tColor = if (highlighted) hlTextColor else textColor
        val cColor = if (highlighted) hlCommentColor else commentColor
        val cornerRadius = ctx.dp(theme.style.candidateCornerRadius)
        val contentColor = if (highlighted) hlBackColor else Color.TRANSPARENT

        content.background = roundedRippleDrawable(hlBackColor, cornerRadius, contentColor)
        text.text = item.text
        text.setTextColor(tColor)

        val commentText = item.comment
        comment.text = commentText
        comment.setTextColor(cColor)
        comment.isVisible = commentText.isNotEmpty()

        // 开启英文翻译时始终保留英文行：有翻译则显示翻译，无翻译用空格占位，
        // 避免未翻译的候选项因独占了英文行的空间而显得更大、更低。
        val englishMode = AppPrefs.defaultInstance().candidates.showEnglishTranslation.getValue()
        if (englishMode) {
            english.text = englishText?.takeIf { it.isNotBlank() } ?: " "
            english.setTextColor(cColor)
            english.isVisible = true
        } else {
            english.text = ""
            english.isVisible = false
        }

        // 开启英文翻译时把候选项撑高到输入栏预留的完整高度，为英文行腾出空间；
        // 关闭时保持原有高度，外观与改动前完全一致。
        val targetHeight = if (englishMode) expandedHeight else candidateHeight
        if (content.layoutParams?.height != targetHeight) {
            content.updateLayoutParams<ViewGroup.LayoutParams> { height = targetHeight }
        }
    }
}
