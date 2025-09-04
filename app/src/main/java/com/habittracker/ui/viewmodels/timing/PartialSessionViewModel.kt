package com.habittracker.ui.viewmodels.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.habittracker.data.repository.timing.TimingRepository
import com.habittracker.analytics.domain.usecases.TrackTimerEventUseCase
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class PartialSessionViewModel @Inject constructor(
    private val timingRepository: TimingRepository,
    private val trackTimerEvent: TrackTimerEventUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<Result<Long>>(extraBufferCapacity = 4)
    val events: SharedFlow<Result<Long>> = _events

    suspend fun logPartial(habitId: Long, duration: Duration, note: String? = null): Long {
        return withContext(Dispatchers.IO) {
            val id = timingRepository.logPartialSession(habitId, duration, note)
            // Analytics: timer_partial_save
            trackTimerEvent(
                eventType = "timer_partial_save",
                habitId = habitId,
                sessionId = null,
                source = null,
                extra = mapOf("durationMs" to duration.toMillis(), "note" to (note ?: ""))
            )
            _events.tryEmit(Result.success(id))
            id
        }
    }
}
