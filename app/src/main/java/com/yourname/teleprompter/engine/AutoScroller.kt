package com.yourname.teleprompter.engine

import android.view.Choreographer
import android.widget.ScrollView

/**
 * 匀速自动滚动器，使用 Choreographer 跑满刷新率（GT7 Pro 120Hz）
 */
class AutoScroller(
    private val scrollView: ScrollView,
    private val speedPxPerSec: () -> Float
) {
    private val choreographer = Choreographer.getInstance()
    private var lastFrameNanos = 0L
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
            val dtSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameTimeNanos
            val dy = (speedPxPerSec() * dtSec).toInt().coerceAtLeast(0)
            if (dy > 0) scrollView.scrollBy(0, dy)
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }
}