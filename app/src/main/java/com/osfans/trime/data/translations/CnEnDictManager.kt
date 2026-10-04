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
 * 词库较大（约 12 万条）。若反序列化后直接保留为 `Map<String, String>`，
 * 仅键值对象与哈希桶就要占用约 19MB 常驻内存。这里把全部条目压进一个扁平
 * [String]（按 key 升序连续存放为 `key\0value\0`），再配一份同序的偏移表，
 * 常驻内存降到约 5MB；命中时才构造结果 String。
 *
 * 加载在后台线程进行；[lookup] 只读取已就绪的词典，未就绪时返回 null，
 * 不会阻塞渲染候选词的 UI 线程。
 */
object CnEnDictManager {

    private const val DICT_ASSET_PATH = "translations/cn_en_dict.json"

    /** 条目分隔符。中文词与英文释义都不含 NUL，可安全用作分隔。 */
    private const val SEP = '\u0000'

    /**
     * 扁平词典。
     *
     * [blob] 中所有条目按 key 升序连续存放为 `key\0value\0`；
     * [offsets] 记录每个条目在 [blob] 中的起始下标，因此其顺序即 key 的升序，
     * 可直接二分查询，无需再单独保存 key。
     */
    private class Table(
        val blob: String,
        val offsets: IntArray,
    ) {
        /** 比较 [blob] 中起始于 [start] 的 key 与 [query]，语义同 [String.compareTo]。 */
        private fun compareKeyAt(
            start: Int,
            query: String,
        ): Int {
            var i = start
            var j = 0
            val n = query.length
            while (true) {
                val c = blob[i]
                if (c == SEP) return if (j == n) 0 else -1 // blob 侧 key 更短
                if (j == n) return 1 // query 更短
                val d = query[j]
                if (c != d) return c - d
                i++
                j++
            }
        }

        /** 取出 [blob] 中起始于 [start] 的条目的释义。 */
        private fun valueAt(start: Int): String {
            var i = start
            while (blob[i] != SEP) i++ // 跳过 key
            i++ // 跳过分隔符
            val valueStart = i
            while (blob[i] != SEP) i++
            return blob.substring(valueStart, i)
        }

        fun lookup(query: String): String? {
            var lo = 0
            var hi = offsets.size - 1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                when (val cmp = compareKeyAt(offsets[mid], query)) {
                    0 -> return valueAt(offsets[mid])
                    in Int.MIN_VALUE until 0 -> hi = mid - 1
                    else -> lo = mid + 1
                }
            }
            return null
        }
    }

    @Volatile
    private var table: Table? = null

    private val lock = Any()

    @Volatile
    private var loadStarted = false

    /** 词典是否已加载完成。 */
    val isReady: Boolean
        get() = table != null

    /**
     * 在后台线程预加载词典。可在输入法启动时调用，通常首次渲染候选词前就已就绪。
     * 重复调用无副作用。
     */
    fun warmUp() {
        if (table != null) return
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
    private fun ensureLoaded(): Table {
        table?.let { return it }
        synchronized(lock) {
            table?.let { return it }
            return try {
                val loaded = loadFromAssets()
                Timber.i("CnEnDictManager loaded ${loaded.offsets.size} entries")
                loaded.also { table = it }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load cn_en_dict.json, English translations disabled")
                Table("", IntArray(0)).also { table = it }
            }
        }
    }

    private fun loadFromAssets(): Table {
        val json = Json { ignoreUnknownKeys = true }
        var text: String? =
            appContext.assets.open(DICT_ASSET_PATH)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        val decoded = json.decodeFromString<Map<String, String>>(text!!)
        text = null // 解析完成后即可释放原文，降低峰值内存

        val keys = decoded.keys.toTypedArray()
        keys.sort()

        // 精确预分配，避免 StringBuilder 扩容时产生额外的临时数组
        var size = 0
        for (key in keys) size += key.length + decoded.getValue(key).length + 2

        val blob = StringBuilder(size)
        val offsets = IntArray(keys.size)
        for (i in keys.indices) {
            val key = keys[i]
            offsets[i] = blob.length
            blob.append(key).append(SEP).append(decoded.getValue(key)).append(SEP)
        }
        return Table(blob.toString(), offsets)
    }

    /**
     * 查询单个中文词的英文翻译。词典未就绪时返回 null 并触发后台加载。
     *
     * @param text 候选项中文文本
     * @return 英文翻译；词典中不存在时返回 null
     */
    fun lookup(text: String): String? {
        if (text.isBlank()) return null
        val current = table
        if (current == null) {
            warmUp()
            return null
        }
        return current.lookup(text)
    }

    /**
     * 批量查询，对一组候选文本返回各自的英文翻译（存在则含，不存在则不含）。
     */
    fun lookupAll(texts: List<String>): Map<String, String> {
        val current = table
        if (current == null) {
            warmUp()
            return emptyMap()
        }
        val result = LinkedHashMap<String, String>(texts.size)
        for (text in texts) {
            if (text.isNotBlank()) {
                current.lookup(text)?.let { result[text] = it }
            }
        }
        return result
    }
}
