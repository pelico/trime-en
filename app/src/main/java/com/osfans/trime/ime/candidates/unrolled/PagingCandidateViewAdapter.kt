/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.osfans.trime.core.CandidateProto
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.data.translations.CnEnDictManager
import com.osfans.trime.ime.candidates.CandidateItemUi
import com.osfans.trime.ime.candidates.CandidateViewHolder

open class PagingCandidateViewAdapter(
    val scope: ThemeScope,
) : PagingDataAdapter<CandidateProto, CandidateViewHolder>(diffCallback) {
    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<CandidateProto>() {
                override fun areItemsTheSame(
                    oldItem: CandidateProto,
                    newItem: CandidateProto,
                ): Boolean = oldItem === newItem

                override fun areContentsTheSame(
                    oldItem: CandidateProto,
                    newItem: CandidateProto,
                ): Boolean = oldItem == newItem
            }
    }

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

    fun refreshWith(offset: Int, highlightedIndex: Int) {
        this.offset = offset
        this.highlightedIndex = highlightedIndex
        englishEnabled = AppPrefs.defaultInstance().candidates.showEnglishTranslation.getValue()
        refresh()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CandidateViewHolder = CandidateViewHolder(CandidateItemUi(parent.context, scope))

    override fun onBindViewHolder(
        holder: CandidateViewHolder,
        position: Int,
    ) {
        val item = getItem(position) ?: return
        val idx = position + offset
        val highlighted = idx == highlightedIndex
        // 功能开启时，即使没有翻译也传空串（由 UI 用空格占位）；关闭时传 null。
        val englishText =
            if (englishEnabled) {
                CnEnDictManager.lookup(item.text).orEmpty()
            } else {
                null
            }
        holder.ui.update(item, highlighted, englishText)
        holder.text = item.text
        holder.comment = item.comment
        holder.idx = idx
    }
}
