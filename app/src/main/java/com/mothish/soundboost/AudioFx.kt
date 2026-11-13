package com.mothish.soundboost

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.*
import kotlin.math.log10

/** Global audio-effect engine (session 0 = whole device output mix). */
object AudioFx {
    var enabled by mutableStateOf(false)
    var boost by mutableIntStateOf(100)      // 100..400 %
    var bass by mutableIntStateOf(0)         // 0..1000
    var surround by mutableIntStateOf(0)     // 0..1000
    var preset by mutableStateOf("Flat")
    val bands = mutableStateListOf<Int>()    // millibels
    var freqs = listOf<Int>()                // Hz
    var minLvl = -1500; var maxLvl = 1500

    private var le: LoudnessEnhancer? = null
    private var eq: Equalizer? = null
    private var bb: BassBoost? = null
    private var vz: Virtualizer? = null

    val presets = listOf("Flat", "Bass", "Vocal", "Treble", "Cinema", "Gaming", "Loud")

    fun start() {
        release()
        le = runCatching { LoudnessEnhancer(0) }.getOrNull()
        eq = runCatching { Equalizer(0, 0) }.getOrNull()
        bb = runCatching { BassBoost(0, 0) }.getOrNull()
        vz = runCatching { Virtualizer(0, 0) }.getOrNull()
        eq?.let { e ->
            minLvl = e.bandLevelRange[0].toInt(); maxLvl = e.bandLevelRange[1].toInt()
            freqs = (0 until e.numberOfBands).map { e.getCenterFreq(it.toShort()) / 1000 }
            if (bands.size != freqs.size) { bands.clear(); repeat(freqs.size) { bands.add(0) } }
        }
        apply()
    }

    fun apply() {
        val gainMb = (2000.0 * log10(boost / 100.0)).toInt()   // 400% = ~1204 mB (+12 dB)
        runCatching { le?.setTargetGain(gainMb); le?.enabled = enabled }
        runCatching {
            eq?.let { e -> bands.forEachIndexed { i, v -> e.setBandLevel(i.toShort(), v.toShort()) }; e.enabled = enabled }
        }
        runCatching { bb?.let { if (it.strengthSupported) it.setStrength(bass.toShort()); it.enabled = enabled && bass > 0 } }
        runCatching { vz?.let { if (it.strengthSupported) it.setStrength(surround.toShort()); it.enabled = enabled && surround > 0 } }
    }

    fun applyPreset(name: String) {
        preset = name
        val cap = maxLvl * 0.8
        for (i in freqs.indices) {
            val f = freqs[i]
            val x = when (name) {
                "Bass" -> if (f < 250) 0.9 else if (f < 600) 0.3 else 0.0
                "Vocal" -> if (f < 200) -0.3 else if (f in 800..4500) 0.7 else 0.0
                "Treble" -> if (f > 4000) 0.9 else if (f < 250) -0.2 else 0.0
                "Cinema" -> if (f < 250) 0.6 else if (f > 6000) 0.5 else -0.1
                "Gaming" -> if (f < 150) 0.3 else if (f in 2000..8000) 0.7 else 0.0
                "Loud" -> if (f < 250) 0.7 else 0.4
                else -> 0.0
            }
            if (i < bands.size) bands[i] = (x * cap).toInt()
        }
        if (name == "Bass" || name == "Cinema") bass = 600 else if (name == "Flat") { bass = 0; surround = 0 }
        if (name == "Cinema") surround = 700
        apply()
    }

    fun release() {
        listOf(le, eq, bb, vz).forEach { runCatching { (it as? android.media.audiofx.AudioEffect)?.release() } }
        le = null; eq = null; bb = null; vz = null
    }
}
