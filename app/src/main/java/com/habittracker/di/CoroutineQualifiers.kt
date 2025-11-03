package com.habittracker.di

import javax.inject.Qualifier

/**
 * Qualifier for application-wide CoroutineScope used for long-lived work.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

/**
 * Qualifier for main-thread dispatchers/scope used for UI-facing coordination.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher

