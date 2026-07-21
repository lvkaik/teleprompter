package com.yourname.teleprompter.engine

/**
 * 文稿匹配引擎：
 * 将 ASR 识别出的文本（needle）与提词文稿（haystack）当前位置附近做模糊匹配，
 * 找到最相似的子串位置，作为新的阅读位置。
 *
 * 策略：
 * 1. 取 haystack 在 currentPos 前后 ±window 范围内的子串
 * 2. 在该子串中滑动 needle，逐个计算归一化编辑距离相似度
 * 3. 相似度 > 阈值则更新位置；否则保留原位置
 */
class MatchEngine(private val script: String) {

    private var currentPos: Int = 0

    /**
     * @param asrText ASR 本次返回的文本片段
     * @return 新的阅读位置（字符索引）
     */
    fun advance(asrText: String): Int {
        if (asrText.isBlank()) return currentPos

        val cleanScript = script.filterNot { it.isWhitespace() }
        val cleanAsr = asrText.filterNot { it.isWhitespace() }
        if (cleanAsr.isEmpty()) return currentPos

        // 大致映射到无空格的位置
        val approxPos = (currentPos.coerceAtMost(cleanScript.length))

        val winStart = (approxPos - WINDOW_BACK).coerceAtLeast(0)
        val winEnd = (approxPos + WINDOW_FORWARD).coerceAtMost(cleanScript.length)
        val window = cleanScript.substring(winStart, winEnd)
        if (window.isEmpty()) return currentPos

        val best = findBestMatch(cleanAsr, window)
        if (best.score >= SIM_THRESHOLD) {
            currentPos = winStart + best.index + cleanAsr.length
            currentPos = currentPos.coerceAtMost(cleanScript.length)
        }
        return currentPos
    }

    fun reset(pos: Int = 0) { currentPos = pos }
    fun position(): Int = currentPos

    private data class BestMatch(val index: Int, val score: Float)

    /**
     * 在 haystack 中找最像 needle 的起点
     */
    private fun findBestMatch(needle: String, haystack: String): BestMatch {
        var bestIndex = 0
        var bestScore = 0f
        val lenN = needle.length
        val lenH = haystack.length
        if (lenN == 0 || lenH < lenN) return BestMatch(0, 0f)

        val stride = STRIDE.coerceAtLeast(1)
        var i = 0
        while (i <= lenH - lenN) {
            val cand = haystack.substring(i, i + lenN)
            val sim = similarity(needle, cand)
            if (sim > bestScore) {
                bestScore = sim
                bestIndex = i
                if (sim >= 0.999f) break
            }
            i += stride
        }

        // 精细化：在 bestIndex 附近 ±STRIDE 再扫一遍
        val refineStart = (bestIndex - stride).coerceAtLeast(0)
        val refineEnd = (bestIndex + stride).coerceAtMost(lenH - lenN)
        for (j in refineStart..refineEnd) {
            val cand = haystack.substring(j, j + lenN)
            val sim = similarity(needle, cand)
            if (sim > bestScore) {
                bestScore = sim
                bestIndex = j
            }
        }
        return BestMatch(bestIndex, bestScore)
    }

    /**
     * 相似度：1 - 归一化编辑距离
     * 使用 Levenshtein DP（O(n*m)，对小 needle 足够快）
     */
    private fun similarity(a: String, b: String): Float {
        val n = a.length; val m = b.length
        if (n == 0 || m == 0) return 0f
        if (n == m && a == b) return 1f

        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)
        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        val dist = prev[m]
        return 1f - dist.toFloat() / maxOf(n, m)
    }

    companion object {
        private const val WINDOW_BACK = 100
        private const val WINDOW_FORWARD = 200
        private const val STRIDE = 2
        private const val SIM_THRESHOLD = 0.55f
    }
}