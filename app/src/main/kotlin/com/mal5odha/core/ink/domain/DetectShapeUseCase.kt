package com.mal5odha.core.ink.domain

import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke
import com.mal5odha.core.ink.models.Stroke as AppStroke
import kotlinx.coroutines.tasks.await

class DetectShapeUseCase {

    private val recognizer by lazy {
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-autodraw")
        val model = DigitalInkRecognitionModel.builder(modelIdentifier!!).build()
        DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }

    suspend operator fun invoke(appStrokes: List<AppStroke>): String {
        val inkBuilder = Ink.builder()
        appStrokes.forEach { appStroke ->
            val strokeBuilder = Stroke.builder()
            appStroke.points.forEach { point ->
                strokeBuilder.addPoint(
                    Ink.Point.create(point.x, point.y, point.timestamp)
                )
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        
        val ink = inkBuilder.build()
        
        return try {
            val result = recognizer.recognize(ink).await()
            if (result.candidates.isNotEmpty()) {
                result.candidates[0].text
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
