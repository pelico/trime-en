/*
 * SPDX-FileCopyrightText: 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.translations

import com.osfans.trime.util.appContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * 中英词典管理器。
 *
 * 从 assets/translations/cn_en_dict.json 加载常用中文 -> 英文翻译映射，
 * 为候选项提供英文释义查询。词典在首次查询时惰性加载并缓存，
 * 避免占用不必要的启动时间。
 */
object CnEnDictManager {

    private const val DICT_ASSET_PATH = "translations/cn_en_dict.json"

    @Volatile
    private var dict: Map<String, String>? = null

    /**
     * 加载词典。首次调用时从 assets 读取并反序列化，后续直接返回缓存。
     * 如果加载失败（文件缺失、JSON 损坏等），返回空 map 并记录日志，
     * 保证输入法主流程不受影响。
     */
    private fun ensureLoaded(): Map<String, String> {
        dict?.let { return it }
        synchronized(this) {
            dict?.let { return it }
            return try {
                val json = Json { ignoreUnknownKeys = true }
                val stream = appContext.assets.open(DICT_ASSET_PATH)
                val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val loaded = json.decodeFromString<Map<String, String>>(text)
                Timber.i("CnEnDictManager loaded ${loaded.size} entries")
                loaded.also { dict = it }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load cn_en_dict.json, English translations disabled")
                emptyMap<String, String>().also { dict = it }
            }
        }
    }

    /**
     * 查询单个中文词的英文翻译。
     *
     * @param text 候选项中文文本
     * @return 英文翻译；词典中不存在时返回 null
     */
    fun lookup(text: String): String? {
        if (text.isBlank()) return null
        val map = ensureLoaded()
        return map[text]
    }

    /**
     * 批量查询，对一组候选文本返回各自的英文翻译（存在则含，不存在则不含）。
     * 供 UI 层一次性处理多项候选时使用。
     */
    fun lookupAll(texts: List<String>): Map<String, String> {
        val map = ensureLoaded()
        val result = LinkedHashMap<String, String>(texts.size)
        for (t in texts) {
            if (t.isNotBlank()) {
                map[t]?.let { result[t] = it }
            }
        }
        return result
    }
}
