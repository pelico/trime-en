// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.candidates.unrolled

import com.osfans.trime.core.CandidateProto

/**
 * 展开面板的一个读音分组：[candidates] 里 second 为该候选在完整候选列表中的全局下标，
 * 组内保持原始顺序（词频序）。
 */
data class UnrolledReadingGroup(
    /** 该组的拼音读音；无读音候选（英文词、联想词等）组成 label 为 null 的组，不显示行头。 */
    val label: String?,
    val candidates: List<Pair<CandidateProto, Int>>,
)

/**
 * 把候选列表构建为按读音分组的横向行列表。
 *
 * - comment 为空的候选组成一个不加行头的组，排在最前，保持原始顺序；
 * - 有读音的候选按读音归组，组内保持原始顺序不变；
 * - 组间按各组最优（原始位置最靠前）候选的先后排序——最符合输入的读音排最前面，
 *   而不是按读音字典序，避免 kuai 被字典序靠前的 ju 挤到后面。
 */
fun buildUnrolledReadingGroups(
    candidates: List<CandidateProto>,
    offset: Int,
): List<UnrolledReadingGroup> {
    val noReading = mutableListOf<Pair<CandidateProto, Int>>()
    // LinkedHashMap 的遍历顺序 = 各读音首次出现的顺序 = 组内最优候选的原始先后
    val grouped = LinkedHashMap<String, MutableList<Pair<CandidateProto, Int>>>()
    for ((index, candidate) in candidates.withIndex()) {
        val reading = candidate.comment.trim()
        val entry = candidate to (offset + index)
        if (reading.isEmpty()) {
            noReading += entry
        } else {
            grouped.getOrPut(reading) { mutableListOf() }.add(entry)
        }
    }
    val result = mutableListOf<UnrolledReadingGroup>()
    if (noReading.isNotEmpty()) result += UnrolledReadingGroup(null, noReading)
    for ((reading, items) in grouped) {
        result += UnrolledReadingGroup(reading, items)
    }
    return result
}
