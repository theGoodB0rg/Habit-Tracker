package com.habittracker.timerux

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TimerUxEntryPoint {
    fun timerActionHandler(): TimerActionHandler
    fun timerActionCoordinator(): TimerActionCoordinator
}

internal fun resolveTimerUxEntryPoint(context: Context): TimerUxEntryPoint {
    val appContext = context.applicationContext
    return EntryPointAccessors.fromApplication(appContext, TimerUxEntryPoint::class.java)
}
