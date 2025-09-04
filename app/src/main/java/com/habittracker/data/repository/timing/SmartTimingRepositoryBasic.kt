package com.habittracker.data.repository.timing

/**
 * Optional simple implementation; currently just wraps TimingRepository.
 */
class SmartTimingRepositoryBasic(
    private val delegate: TimingRepository
) : SmartTimingRepository by delegate
