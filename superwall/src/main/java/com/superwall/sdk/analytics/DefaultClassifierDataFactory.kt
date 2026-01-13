package com.superwall.sdk.analytics

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Insets
import android.graphics.Point
import android.media.MediaCodecList
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import kotlin.math.pow
import kotlin.math.sqrt

class DefaultClassifierDataFactory(
    private val _context: () -> Context,
) : ClassifierDataFactory {
    override fun context(): Context = _context()
}

// Used to provide dependencies so we can mock in tests
interface ClassifierDataFactory {
    fun context(): Context

    private val activityManager: ActivityManager
        get() =
            getSystemService(
                context(),
                ActivityManager::class.java,
            ) as ActivityManager
    private val windowManager: WindowManager
        get() = (
            getSystemService(
                context(),
                WindowManager::class.java,
            ) as WindowManager
        )

    fun codecs(): List<String> = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.flatMap { it.supportedTypes.toList() }

    fun advertisedMemory(): Long {
        ActivityManager
            .MemoryInfo()
            .also { memoryInfo ->
                activityManager.getMemoryInfo(memoryInfo)
            }.let {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val ratio =
                        if (it.advertisedMem != 0L) {
                            ((it.advertisedMem - it.totalMem) * 100) / it.advertisedMem
                        } else {
                            31L
                        }
                    if (ratio > 30) {
                        it.totalMem
                    } else {
                        it.advertisedMem
                    }
                } else {
                    it.totalMem
                }
            }
    }

    fun getScreenWidth(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets: Insets =
                windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return windowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            ContextCompat.getDisplayOrDefault(context()).getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }
    }

    fun getScreenHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets: Insets =
                windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return windowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            ContextCompat.getDisplayOrDefault(context()).getMetrics(displayMetrics)
            return displayMetrics.heightPixels
        }
    }

    fun getScreenSize(): Double {
        val point = Point()
        ContextCompat.getDisplayOrDefault(context()).getRealSize(point)
        val displayMetrics: DisplayMetrics = context().resources.displayMetrics
        val width = point.x
        val height = point.y
        val wi = width.toDouble() / displayMetrics.xdpi.toDouble()
        val hi = height.toDouble() / displayMetrics.ydpi.toDouble()
        val x = wi.pow(2.0)
        val y = hi.pow(2.0)
        return (Math.round((sqrt(x + y)) * 10.0) / 10.0)
    }

    fun isTabletMinWidth() = context().resources.configuration.smallestScreenWidthDp >= 600

    val orientation: DeviceClassifier.Orientation
        get() =
            context().resources.configuration.orientation.let {
                when (it) {
                    Configuration.ORIENTATION_PORTRAIT -> DeviceClassifier.Orientation.PORTRAIT
                    Configuration.ORIENTATION_LANDSCAPE -> DeviceClassifier.Orientation.LANDSCAPE
                    else -> DeviceClassifier.Orientation.UNDEFINED
                }
            }
    val cpuInfo: CPUInfo.Data
        get() = CPUInfo().cpuData
}
