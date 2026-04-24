package com.example.truenorthzen

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class DailyChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("daily_challenge", Context.MODE_PRIVATE)
    
    private val _targetDegree = MutableStateFlow(0)
    val targetDegree: StateFlow<Int> = _targetDegree.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _holdProgress = MutableStateFlow(0f) // 0f to 1f
    val holdProgress: StateFlow<Float> = _holdProgress.asStateFlow()

    private var timerJob: Job? = null
    private val compassManager = CompassManager(application)

    val currentAzimuth = compassManager.azimuth

    init {
        generateDailyTarget()
        checkCompletion()
        observeCompass()
    }

    fun startCompass() {
        compassManager.start()
    }

    fun stopCompass() {
        compassManager.stop()
    }

    private fun generateDailyTarget() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val seed = today.toLong()
        val random = Random(seed)
        _targetDegree.value = random.nextInt(360)
    }

    private fun checkCompletion() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        _isCompleted.value = prefs.getString("completed_date", "") == today
    }

    private fun observeCompass() {
        viewModelScope.launch {
            currentAzimuth.collect { azimuth ->
                if (_isCompleted.value) return@collect

                val diff = abs(azimuth - _targetDegree.value)
                val normalizedDiff = if (diff > 180) 360 - diff else diff

                if (normalizedDiff < 5) { // Within 5 degrees
                    if (timerJob == null) {
                        startTimer()
                    }
                } else {
                    stopTimer()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val duration = 3000L
            while (System.currentTimeMillis() - startTime < duration) {
                _holdProgress.value = (System.currentTimeMillis() - startTime).toFloat() / duration
                delay(50)
            }
            _holdProgress.value = 1f
            completeChallenge()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _holdProgress.value = 0f
    }

    private fun completeChallenge() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        prefs.edit { putString("completed_date", today) }
        _isCompleted.value = true
        
        vibrate()

        // Add to completed coordinates list
        val completedCoordinates = prefs.getStringSet("completed_coordinates", setOf())?.toMutableSet() ?: mutableSetOf()
        completedCoordinates.add(_targetDegree.value.toString())
        prefs.edit { putStringSet("completed_coordinates", completedCoordinates) }
    }

    fun getCompletedDegrees(): Set<Int> {
        return prefs.getStringSet("completed_coordinates", setOf())?.map { it.toInt() }?.toSet() ?: setOf()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    override fun onCleared() {
        super.onCleared()
        compassManager.stop()
    }
}
