/*
 * SPDX-FileCopyrightText: 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.translations

import com.osfans.trime.util.appContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.concurrent.thread

/**
 * 中英词典管理器。
 *
 * 从 assets/translations/cn_en_dict.json 加载「中文 -> 英文释义」映射，
 * 为候选项提供英文翻译查询。
 *
 * 词库较大（约 9 万条），因此在后台线程中预加载；
 * [lookup] 只读取已就绪的词典，词典尚未加载完成时直接返回 null，
 * 不会阻塞渲染候选词的 UI 线程。
 */
object CnEnDictManager {

    private const val DICT_ASSET_PATH = "translations/cn_en_dict.json"

    @Volatile
    private var dict: Map<String, String>? = null

    private val lock = Any()

    @Volatile
    private var loadStarted = false

    /** 词典是否已加载完成。 */
    val isReady: Boolean
        get() = dict != null

    /**
     * 在后台线程预加载词典。可在输入法启动时调用，通常首次渲染候选词前就已就绪。
     * 重复调用无副作用。
     */
    fun warmUp() {
        if (dict != null) return
        synchronized(lock) {
            if (loadStarted) return
            loadStarted = true
        }
        thread(name = "cn-en-dict-loader") { ensureLoaded() }
    }

    /**
     * 加载词典并缓存。加载失败（文件缺失、JSON 损坏等）时返回空表并记录日志，
     * 保证输入法主流程不受影响。
     */
    private fun ensureLoaded(): Map<String, String> {
        dict?.let { return it }
        synchronized(lock) {
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
     * 查询单个中文词的英文翻译。词典未就绪时返回 null 并触发后台加载。
     *
     * @param text 候选项中文文本
     * @return 英文翻译；词典中不存在时返回 null
     */
    fun lookup(text: String): String? {
        if (text.isBlank()) return null
        val map = dict ?: run {
            warmUp()
            return null
        }
        return map[text]
    }

    /**
     * 批量查询，对一组候选文本返回各自的英文翻译（存在则含，不存在则不含）。
     */
    fun lookupAll(texts: List<String>): Map<String, String> {
        val map = dict ?: run {
            warmUp()
            return emptyMap()
        }
        val result = LinkedHashMap<String, String>(texts.size)
        for (t in texts) {
            if (t.isNotBlank()) {
                map[t]?.let { result[t] = it }
            }
        }
        return result
    }
}
