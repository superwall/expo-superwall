package com.superwall.sdk.analytics

import com.superwall.sdk.misc.Either
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
enum class Tier(
    val raw: String,
) {
    @SerialName("ULTRA_LOW")
    ULTRA_LOW("ultra_low"),

    @SerialName("LOW")
    LOW("low"),

    @SerialName("MID")
    MID("mid"),

    @SerialName("HIGH")
    HIGH("high"),

    @SerialName("ULTRA_HIGH")
    ULTRA_HIGH("ultra_high"),

    @SerialName("UNKNOWN")
    UNKNOWN("unknown"),
}

class DeviceClassifier(
    private val factory: ClassifierDataFactory,
) : ClassifierDataFactory by factory {
    enum class Orientation {
        LANDSCAPE,
        PORTRAIT,
        UNDEFINED,
    }

    private val videoCodecs by lazy { codecs().filter { it.contains("video") } }

    /** Classify the device into an assumed tier of devices.
     *  This evaluates:
     *  - The CPU's core speed and frequency in comparison to a 4Ghz max 8 core CPU
     *  - If the CPU is high end and has 30%+ high performing cores (>3.5Ghz)
     *  - Memory score in comparison to a 10GB RAM device
     *  - Video score for displaying 4k, 2k and 1080p videos
     *  - A display score for high density displays with better resolutions
     * */

    fun deviceTier(): Tier =
        when (val s = totalScore().getSuccess()) {
            null -> Tier.UNKNOWN // There was an error evaluating
            in 0..24 -> Tier.ULTRA_LOW
            in 25..44 -> Tier.LOW
            in 45..69 -> Tier.MID
            in 70..85 -> Tier.HIGH
            else -> Tier.ULTRA_HIGH // 86-100
        }

    // 0 to 35 points, scores both frequencies and core count.
    // As OEMs can misreport this, we ensure the frequencies are in proper range
    private fun cpuScore(): Int {
        // On some devices we cant return a CPU Freq so we ignore it
        if (cpuInfo.cores.isEmpty() || cpuInfo.cores.all { it.currentFreq < 0 }) {
            return -1
        }
        // `maxFreq` is usually reported in kHz; convert to GHz for clarity
        val coreFreqsKhz = cpuInfo.cores.map { it.maxFreq }
        val avgGhz =
            (coreFreqsKhz.average() / 1_000_000.0).let {
                if (it > 1000) {
                    it / 1000
                } else {
                    it
                }
            }
        val coreCount = coreFreqsKhz.size

        val freqPart = (avgGhz / 4.0).coerceAtMost(1.0) * 35 // up to 20 pt
        val corePart = (coreCount / 8.0).coerceAtMost(1.0) * freqPart // up to 15 pt
        return (corePart).toInt() // 0 – 35 p
    }

    // If we have a third or more cores with a high CPU frequency, add extra points
    private fun primeFreqBonus(): Int = if (cpuInfo.cores.count { it.maxFreq >= 3_500_000 } >= (cpuInfo.cores.size / 3)) 5 else 0

    // Classify memory, rounding to 10GB for max score
    private fun memScore(): Int {
        val memBytes = advertisedMemory()
        val memGb = memBytes / 1_073_741_824.0 // bytes → GB
        return ((memGb / 10.0).coerceAtMost(1.0) * 25).toInt() // 0 – 25 pt
    }

    // Classify codec support, giving extra points for 4K and 2K support.
    // Coerces at 20, as having av01 and hevc puts it into a higher tier immediately.
    private fun videoScore(): Int {
        var score = 0
        if (videoCodecs.any { it.contains("av01", true) }) score += 10
        if (videoCodecs.any { it.contains("hevc", true) }) score += 14
        if (videoCodecs.any { it.contains("vp9", true) }) score += 6
        if (videoCodecs.any { it.contains("vp8", true) }) score += 4
        return score.coerceAtMost(20) // 0 – 20 pt
    }

    // Gets display score for ppi, with 500ppi being the max score (ultra phones have even higher res)
    private fun displayScore(): Int {
        val pxW = getScreenWidth().toDouble()
        val pxH = getScreenHeight().toDouble()
        val diagInches = getScreenSize()
        val ppi = sqrt(pxW * pxW + pxH * pxH) / diagInches
        return ((ppi / 500.0).coerceAtMost(1.0) * 20).toInt() // 0 – 20 pt
    }

    // Gets the total score for the device
    private fun totalScore(): Either<Int, Throwable> =
        withErrorTracking {
            val cpuScore = cpuScore()
            val otherScores =
                primeFreqBonus() +
                    memScore() +
                    videoScore() +
                    displayScore()
            // We might not get access to CPU info, so we scale the existing score.
            if (cpuScore == -1) {
                return@withErrorTracking ((otherScores * 100) / 65).toInt()
            } else {
                return@withErrorTracking cpuScore + otherScores
            }
        }
}
