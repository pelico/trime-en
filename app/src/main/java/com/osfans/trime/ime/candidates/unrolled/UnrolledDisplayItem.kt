// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.candidates.unrolled

import com.osfans.trime.core.CandidateProto

/** 展开面板的列表项：读音分组行头或候选项。 */
sealed class UnrolledDisplayItem {
    /**
     * 读音分组行头（如 pin / ping），显示在一组候选的最上方，
     * 配合分隔线让每个读音对应的候选一目了然。
     */
    data class Header(
        val label: String,
    ) : UnrolledDisplayItem()

    /** 候选项；[globalIdx] 为其在 Rime 完整候选列表中的全局下标，用于点选/长按。 */
    data class Candidate(
        val proto: CandidateProto,
        val globalIdx: Int,
    ) : UnrolledDisplayItem()
}

/**
 * 把候选列表构建为按读音分组的展示列表。
 *
 * - comment 为空（英文词、联想词、自定义短语等没有读音的候选）保持原顺序排在最前，不加行头；
 * - 其余按读音（comment）字典序分组，组内保持原候选顺序（词频序）不变；
 * - 每组最上方插入一个 [UnrolledDisplayItem.Header] 行头。
 *
 * 候选项的 [UnrolledDisplayItem.Candidate.globalIdx] 始终映射回其在原始列表中的全局下标
 * （offset + 原始下标），排序仅影响展示顺序，不影响点选。
 */
fun buildUnrolledDisplayItems(
    candidates: List<CandidateProto>,
    offset: Int,
): List<UnrolledDisplayItem> {
    val sorted =
        candidates.withIndex()
            .sortedWith(compareBy({ it.value.comment.trim() }, { it.index }))
    val items = mutableListOf<UnrolledDisplayItem>()
    var lastKey: String? = null
    for ((index, candidate) in sorted) {
        val key = candidate.comment.trim()
        if (key.isNotEmpty() && key != lastKey) {
            items += UnrolledDisplayItem.Header(key)
        }
        lastKey = key
        items += UnrolledDisplayItem.Candidate(candidate, offset + index)
    }
    return items
}
