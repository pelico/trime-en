/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import kotlinx.coroutines.flow.SharedFlow

interface RimeApi {
    val messageFlow: SharedFlow<RimeMessage<*>>

    val isReady: Boolean

    val schemaCached: RimeSchema

    val statusCached: StatusProto

    val compositionCached: CompositionProto

    val hasMenu: Boolean

    val paging: Boolean

    suspend fun isEmpty(): Boolean

    suspend fun deploy(skipImport: Boolean = false)

    suspend fun updateConfig()

    suspend fun syncUserData(): Boolean

    suspend fun processKey(
        value: Int,
        modifiers: UInt = 0u,
        isVirtual: Boolean = true,
    ): Boolean

    suspend fun processKey(
        value: KeyValue,
        modifiers: KeyModifiers,
        isVirtual: Boolean = true,
    ): Boolean

    suspend fun simulateKeySequence(
        sequence: String,
    ): Boolean

    suspend fun selectCandidate(idx: Int, global: Boolean): Boolean

    suspend fun deleteCandidate(idx: Int, global: Boolean): Boolean

    suspend fun changeCandidatePage(backward: Boolean): Boolean

    suspend fun moveCursorPos(position: Int)

    suspend fun availableSchemata(): Array<SchemaItem>

    suspend fun enabledSchemata(): Array<SchemaItem>

    suspend fun setEnabledSchemata(schemaIds: Array<String>): Boolean

    suspend fun selectedSchemata(): Array<SchemaItem>

    suspend fun selectedSchemaId(): String

    suspend fun selectSchema(schemaId: String): Boolean

    suspend fun currentSchema(): RimeSchema

    suspend fun commitComposition(): Boolean

    suspend fun clearComposition()

    /**
     * 清空组合，但清理期间临时关闭 prediction 选项。
     *
     * librime-predict 的 predictor 监听 update 通知，在组合为空且 prediction
     * 开启时会基于最近一次上屏文本立即重建联想段，因此直接 [clearComposition]
     * 清不掉联想词；关闭选项后再清理即可避免重建（set_option 只发 option 通知，
     * 不会触发联想）。
     */
    suspend fun clearCompositionWithoutPrediction() {
        val predictionEnabled = getRuntimeOption("prediction")
        setRuntimeOption("prediction", false)
        clearComposition()
        setRuntimeOption("prediction", predictionEnabled)
    }

    /**
     * 提交当前组合；若组合里只剩联想段（librime-predict 在每次上屏后把联想候选
     * 挂进 composition，此时用户并没有拼音输入，[getRawInput] 为空），则丢弃
     * 联想而不是把第一条联想词提交上屏。
     *
     * @return 是否发生了真实提交（存在用户拼音输入时）
     */
    suspend fun commitOrDropPrediction(): Boolean {
        if (!statusCached.isComposing) return false
        if (getRawInput().isNotEmpty()) return commitComposition()
        clearCompositionWithoutPrediction()
        return false
    }

    suspend fun getRawInput(): String

    suspend fun setRuntimeOption(
        option: String,
        value: Boolean,
    )

    suspend fun getRuntimeOption(option: String): Boolean

    suspend fun setNullInputType(value: Boolean)

    suspend fun getCandidates(
        startIndex: Int,
        limit: Int,
    ): Array<CandidateProto>

    suspend fun setCandidatePagingMode(enabled: Boolean)
}
