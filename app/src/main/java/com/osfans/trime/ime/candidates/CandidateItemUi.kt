/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.osfans.trime.core.CandidateProto
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
import splitties.views.dsl.constraintlayout.centerVertically
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
    private val englishSize = (commentSize * 0.85f).coerceAtLeast(8f)

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
     * 英文翻译行 —— 始终位于 text 正上方。通过 update() 动态控制显隐
     * 和 text/comment 的约束关系，确保有翻译时布局自然撑开，无翻译时
     * 回退到原始 comment 位置逻辑，完全兼容主题配置。
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

    // 保存 text 和 comment 的 LayoutParams 引用，用于在 update 时动态切换 top 约束
    private lateinit var textLp: ConstraintLayout.LayoutParams
    private lateinit var commentLp: ConstraintLayout.LayoutParams

    private val content = constraintLayout {
        horizontalPadding = dp(theme.style.candidatePadding)
        // english 始终 topOfParent，有翻译时占据顶部一行，无翻译时 GONE（0 高度）
        add(
            english,
            lParams(wrapContent, wrapContent) {
                topOfParent()
                centerHorizontally()
            },
        )
        when (commentPosition) {
            GeneralStyle.CommentPosition.RIGHT -> {
                add(
                    text,
                    lParams(wrapContent, wrapContent) {
                        centerVertically()
                        startOfParent()
                        endToStartOf(comment)
                        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
                    }.also { textLp = it },
                )
                add(
                    comment,
                    lParams(wrapContent, wrapContent) {
                        startToEndOf(text, ctx.dp(1))
                        endOfParent()
                        baselineToBaselineOf(text)
                        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
                    }.also { commentLp = it },
                )
            }

            GeneralStyle.CommentPosition.TOP -> {
                add(
                    text,
                    lParams(wrapContent, matchConstraints) {
                        centerHorizontally()
                        bottomOfParent()
                        topToBottomOf(comment)
                    }.also { textLp = it },
                )
                add(
                    comment,
                    lParams(wrapContent, matchConstraints) {
                        matchConstraintPercentHeight = 0.4f
                        topOfParent()
                        centerHorizontally()
                        bottomToTopOf(text)
                    }.also { commentLp = it },
                )
            }

            GeneralStyle.CommentPosition.OVERLAY -> {
                add(
                    text,
                    lParams(wrapContent, wrapContent) {
                        centerInParent()
                        verticalBias = candidateTextVerticalBias
                    }.also { textLp = it },
                )
                add(
                    comment,
                    lParams(wrapContent, wrapContent) {
                        centerInParent()
                        verticalBias = commentVerticalBias
                    }.also { commentLp = it },
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

        // 英文翻译行：有翻译时显示并把 text/comment 约束到 english 下方；
        // 无翻译时 GONE，text/comment 回到各自的原始 top 约束。
        val hasEnglish = !englishText.isNullOrBlank()
        if (hasEnglish) {
            english.text = englishText
            english.setTextColor(cColor)
            english.visibility = View.VISIBLE
            applyEnglishTopConstraints()
        } else {
            english.visibility = View.GONE
            applyOriginalTopConstraints()
        }
    }

    /**
     * 切换到有英文翻译的约束状态：text（和 TOP 模式的 comment）从 english 下方开始。
     * 每种 commentPosition 场景下的调整策略不同：
     * - RIGHT: text/comment 保持 centerVertically 不变，因为它们在同一行，
     *   centerVertically 会在 english gone 时自动居中到剩余空间
     *   （实际上 ConstraintLayout 的 centerVertically 在父容器中仍然有效，
     *   我们只需要让 text 的 top 不越过 english 的 bottom 即可）
     * - TOP: comment 原本 topOfParent → 改为 topToBottomOf(english)
     *        text 原本 topToBottomOf(comment) → 保持不变（comment 已经往下挪了）
     * - OVERLAY: text/comment centerInParent → 无需调整（居中策略自然跳过 english 的空间）
     */
    private fun applyEnglishTopConstraints() {
        when (commentPosition) {
            GeneralStyle.CommentPosition.RIGHT -> {
                // text 的 top 约束改为 topToBottomOf(english)，确保不越过英文行
                textLp.topToBottomOf = english.id
                textLp.topToTop = ConstraintLayout.LayoutParams.UNSET
                // comment 的 baselineToBaselineOf(text) 已经会跟随 text 移动，无需额外处理
            }

            GeneralStyle.CommentPosition.TOP -> {
                // comment 原本 topOfParent → topToBottomOf(english)
                commentLp.topToBottomOf = english.id
                commentLp.topToTop = ConstraintLayout.LayoutParams.UNSET
                // text 保持 topToBottomOf(comment)，会随 comment 一起下移
            }

            GeneralStyle.CommentPosition.OVERLAY -> {
                // OVERLAY 用 centerInParent + verticalBias，保持不变
            }
        }
        text.layoutParams = textLp
        if (::commentLp.isInitialized) {
            comment.layoutParams = commentLp
        }
    }

    /**
     * 切换到无英文翻译的约束状态（恢复到 CandidateItemUi 初始化时的原始约束）。
     */
    private fun applyOriginalTopConstraints() {
        when (commentPosition) {
            GeneralStyle.CommentPosition.RIGHT -> {
                textLp.topToBottomOf = ConstraintLayout.LayoutParams.UNSET
                textLp.topToTop = ConstraintLayout.LayoutParams.UNSET
            }

            GeneralStyle.CommentPosition.TOP -> {
                commentLp.topToBottomOf = ConstraintLayout.LayoutParams.UNSET
                commentLp.topToTop = ConstraintLayout.LayoutParams.UNSET
            }

            GeneralStyle.CommentPosition.OVERLAY -> {
                // 不变
            }
        }
        text.layoutParams = textLp
        if (::commentLp.isInitialized) {
            comment.layoutParams = commentLp
        }
    }
}
