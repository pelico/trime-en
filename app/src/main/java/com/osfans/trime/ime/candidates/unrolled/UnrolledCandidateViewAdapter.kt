// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.candidates.unrolled

import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
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
 * 展开面板的适配器：每个读音组占一行——左侧是拼音行头，右侧是该读音的候选横向列表，
 * 手指右滑即可查看同一读音的更多候选，避免冷门读音把面板撑得过长。
 */
class UnrolledCandidateViewAdapter(
    private val scope: ThemeScope,
    private val rime: RimeSession,
    private val bindCandidateUi: (CandidateViewHolder) -> Unit,
) : RecyclerView.Adapter<UnrolledCandidateViewAdapter.GroupViewHolder>() {
    companion object {
        /**
         * 一次性拉取的最大候选数。按读音分组需要完整候选列表才能正确归组，
         * 因此不再做增量分页；常规输入的候选菜单远小于该值。
         */
        const val MAX_CANDIDATES = 500
    }

    private val groups = mutableListOf<UnrolledReadingGroup>()

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
        groups.clear()
        groups += buildUnrolledReadingGroups(candidates.toList(), offset)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = groups.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): GroupViewHolder {
        val context = parent.context
        // 拼音行头固定在行左侧，不随候选滚动
        val header =
            TextView(context).apply {
                textSize = scope.theme.style.commentTextSize
                typeface = scope.theme.fonts.comment
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(scope.theme.style.candidatePadding + 2), dp(6), dp(4), dp(2))
            }
        val rowAdapter = RowAdapter()
        val row =
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = rowAdapter
            }
        val container =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    header,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    row,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
            }
        return GroupViewHolder(container, header, row, rowAdapter)
    }

    override fun onBindViewHolder(
        holder: GroupViewHolder,
        position: Int,
    ) {
        val group = groups[position]
        holder.header.isVisible = group.label != null
        holder.header.text = group.label.orEmpty()
        // 行头用高亮候选的强调色，与正文候选文字区分开
        holder.header.setTextColor(scope.colors.hilitedCandidateTextColor)
        holder.rowAdapter.group = group
        holder.rowAdapter.notifyDataSetChanged()
    }

    /** 单个读音组的横向候选列表。 */
    inner class RowAdapter : RecyclerView.Adapter<CandidateViewHolder>() {
        var group: UnrolledReadingGroup? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): CandidateViewHolder = CandidateViewHolder(CandidateItemUi(parent.context, scope))

        override fun getItemCount(): Int = group?.candidates?.size ?: 0

        override fun onBindViewHolder(
            holder: CandidateViewHolder,
            position: Int,
        ) {
            val (proto, globalIdx) = group?.candidates?.get(position) ?: return
            val highlighted = globalIdx == highlightedIndex
            // 功能开启时，即使没有翻译也传空串（由 UI 用空格占位）；关闭时传 null。
            val englishText =
                if (englishEnabled) {
                    CnEnDictManager.lookup(proto.text).orEmpty()
                } else {
                    null
                }
            // 读音已由行头展示，候选项本身不再显示 comment。
            holder.ui.update(proto, highlighted, englishText, showComment = false)
            holder.text = proto.text
            holder.comment = proto.comment
            holder.idx = globalIdx
            bindCandidateUi(holder)
        }
    }

    inner class GroupViewHolder(
        container: ViewGroup,
        val header: TextView,
        val row: RecyclerView,
        val rowAdapter: RowAdapter,
    ) : RecyclerView.ViewHolder(container)
}
