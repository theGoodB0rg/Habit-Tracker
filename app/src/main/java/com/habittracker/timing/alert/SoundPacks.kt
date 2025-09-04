package com.habittracker.timing.alert

import com.habittracker.timing.AlertType
import com.habittracker.R

/** Maps alert types to raw sound resource ids (or null when silent). */
interface TimerSoundPack {
    val id: String
    fun resIdFor(type: AlertType): Int?
}

object DefaultChimePack : TimerSoundPack {
    override val id: String = "default"
    override fun resIdFor(type: AlertType): Int? = when (type) {
        AlertType.START -> R.raw.timer_start
        AlertType.MIDPOINT -> R.raw.timer_mid
        AlertType.PROGRESS -> R.raw.timer_progress_soft
        AlertType.FINAL -> R.raw.timer_final
    }
}

object MinimalPack : TimerSoundPack {
    override val id: String = "minimal"
    override fun resIdFor(type: AlertType): Int? = when (type) {
        AlertType.START -> R.raw.timer_start
        AlertType.FINAL -> R.raw.timer_final
        else -> null
    }
}

object SilentPack : TimerSoundPack {
    override val id: String = "silent"
    override fun resIdFor(type: AlertType): Int? = null
}

object SystemPack : TimerSoundPack {
    // Delegates to system notification (handled elsewhere) so internal sounds suppressed
    override val id: String = "system"
    override fun resIdFor(type: AlertType): Int? = null
}

fun soundPackById(id: String): TimerSoundPack = when (id) {
    MinimalPack.id -> MinimalPack
    SilentPack.id -> SilentPack
    SystemPack.id -> SystemPack
    else -> DefaultChimePack
}
