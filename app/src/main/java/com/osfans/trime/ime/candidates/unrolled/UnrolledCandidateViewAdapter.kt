// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.candidates.unrolled

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.osfans.trime.core.CandidateProto
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.data.translations.CnEnDictManager
import com.osfans.trime.ime.candidates.CandidateItemUi
import com.osfans.trime.ime.candidates.CandidateViewHolder
import splitties.dimensions.dp

/**
 * 展开面板的候选列表适配器：一次性拉取候选（见 [MAX_CANDIDATES]）后按读音分组展示，
 * 每组最上方是一个拼音读音行头。
 */
class UnrolledCandidateViewAdapter(
    private val scope: ThemeScope,
    private val rime: RimeSession,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        /**
         * 一次性拉取的最大候选数。按读音分组需要完整候选列表才能正确排序，
         * 因此不再做增量分页；常规输入的候选菜单远小于该值。
         */
        const val MAX_CANDIDATES = 500

        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_CANDIDATE = 2
    }

    private val items = mutableListOf<UnrolledDisplayItem>()

    var offset: Int = 0
        private set

    var highlightedIndex: Int = -1
        private set

    /**
     * 英文翻译开关的缓存值：每次 [refreshWith] 读一次配置，
     * 避免在 onBindViewHolder 里为每个候选项重复读取 SharedPreferences。
     */
    private var englishEnabled: Boolean =
        AppPrefs.defaultInstance().candidates.showEnglishTranslation.getValue()

    /**
     * 从 [offset] 开始拉取候选并重建读音分组列表。
     * [highlightedIndex] 为当前高亮候选的全局下标。
     */
    suspend fun refreshWith(
        offset: Int,
        highlightedIndex: Int,
    ) {
        this.offset = offset
        this.highlightedIndex = highlightedIndex
        englishEnabled = AppPrefs.defaultInstance().candidates.showEnglishTranslation.getValue()
        val candidates =
            rime.runOnReady {
                getCandidates(offset, MAX_CANDIDATES)
            }
        items.clear()
        items += buildUnrolledDisplayItems(candidates.toList(), offset)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is UnrolledDisplayItem.Header -> VIEW_TYPE_HEADER
            is UnrolledDisplayItem.Candidate -> VIEW_TYPE_CANDIDATE
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(headerLabel(parent))
            else -> CandidateViewHolder(CandidateItemUi(parent.context, scope))
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = items[position]) {
            is UnrolledDisplayItem.Header ->
                (holder as HeaderViewHolder).label.apply {
                    text = item.label
                    setTextColor(scope.colors.commentTextColor)
                }

            is UnrolledDisplayItem.Candidate -> bindCandidate(holder as CandidateViewHolder, item)
        }
    }

    private fun bindCandidate(
        holder: CandidateViewHolder,
        item: UnrolledDisplayItem.Candidate,
    ) {
        val proto = item.proto
        val highlighted = item.globalIdx == highlightedIndex
        // 功能开启时，即使没有翻译也传空串（由 UI 用空格占位）；关闭时传 null。
        val englishText =
            if (englishEnabled) {
                CnEnDictManager.lookup(proto.text).orEmpty()
            } else {
                null
            }
        // 读音已由分组行头展示，候选项本身不再显示 comment。
        holder.ui.update(proto, highlighted, englishText, showComment = false)
        holder.text = proto.text
        holder.comment = proto.comment
        holder.idx = item.globalIdx
    }

    private fun headerLabel(parent: ViewGroup): TextView =
        TextView(parent.context).apply {
            val theme = scope.theme
            textSize = theme.style.commentTextSize
            typeface = theme.fonts.comment
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(theme.style.candidatePadding + 2), dp(6), dp(4), dp(2))
        }

    class HeaderViewHolder(
        val label: TextView,
    ) : RecyclerView.ViewHolder(label)
}
