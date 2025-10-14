package com.superwall.sdk.analytics

import android.os.Build
import kotlinx.serialization.Serializable
import java.io.RandomAccessFile

class CPUInfo {
    @Serializable
    data class Data(
        val cores: List<Core>,
    ) {
        @Serializable
        data class Core(
            val number: Int,
            val currentFreq: Long,
            val maxFreq: Long,
            val minFreq: Long,
        ) {
            override fun toString(): String = "#$number, current: $currentFreq, max: $maxFreq, min: $minFreq"
        }

        override fun toString(): String = "Cores: ${cores.size}: [${cores.joinToString()}"
    }

    val cpuData: Data get() {
        val cores = getNumberOfCores
        return Data(
            (0..<getNumberOfCores).map {
                val maxMin = minMaxFrequencyForCore(it)
                Data.Core(
                    it,
                    frequencyForCore(it),
                    maxFreq = maxMin.second,
                    minFreq = maxMin.first,
                )
            },
        )
    }

    val abi: String
        get() {
            return if (Build.VERSION.SDK_INT >= 21) {
                Build.SUPPORTED_ABIS[0]
            } else {
                @Suppress("DEPRECATION")
                Build.CPU_ABI
            }
        }

    val getNumberOfCores: Int get() =
        Runtime.getRuntime().availableProcessors()

    fun frequencyForCore(coreNumber: Int): Long {
        val currentFreqPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/scaling_cur_freq"
        return try {
            RandomAccessFile(currentFreqPath, "r").use { it.readLine().toLong() / 1000 }
        } catch (e: Exception) {
            -1
        }
    }

    fun minMaxFrequencyForCore(coreNumber: Int): Pair<Long, Long> {
        val minPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_min_freq"
        val maxPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_max_freq"
        return try {
            val minMhz = RandomAccessFile(minPath, "r").use { it.readLine().toLong() / 1000 }
            val maxMhz = RandomAccessFile(maxPath, "r").use { it.readLine().toLong() / 1000 }
            Pair(minMhz, maxMhz)
        } catch (e: Exception) {
            Pair(-1, -1)
        }
    }

    companion object {
        private const val CPU_INFO_DIR = "/sys/devices/system/cpu/"
    }
}
