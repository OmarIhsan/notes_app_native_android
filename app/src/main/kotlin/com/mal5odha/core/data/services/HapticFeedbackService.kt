package com.mal5odha.core.data.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import javax.inject.Inject
import javax.inject.Singleton

interface HapticFeedbackService {
    fun performLightVibration()
    fun performHeavyVibration()
}

@Singleton
class HapticFeedbackServiceImpl
@Inject
constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) :
        HapticFeedbackService {

    private val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as
                                VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

    override fun performLightVibration() {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(30)
            }
        }
    }

    override fun performHeavyVibration() {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(100)
            }
        }
    }
}
