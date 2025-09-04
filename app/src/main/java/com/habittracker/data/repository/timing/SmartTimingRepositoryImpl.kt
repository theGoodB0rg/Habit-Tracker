package com.habittracker.data.repository.timing

import javax.inject.Inject

/**
 * Delegates all behavior to TimingRepository. Keep for compatibility.
 */
class SmartTimingRepositoryImpl @Inject constructor(
    private val delegate: TimingRepository
) : SmartTimingRepository by delegate
