package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.core.ink.models.TextAnnotation
import kotlin.math.roundToInt

@Composable
fun TextBoxWidget(annotation: TextAnnotation, onAnnotationChanged: (TextAnnotation) -> Unit) {
    var offsetX by remember { mutableStateOf(annotation.x) }
    var offsetY by remember { mutableStateOf(annotation.y) }
    var text by remember { mutableStateOf(annotation.text) }

    Box(
            modifier =
                    Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .pointerInput(annotation.id) {
                                detectDragGestures(
                                        onDragEnd = {
                                            onAnnotationChanged(
                                                    annotation.copy(
                                                            xNorm = offsetX,
                                                            yNorm = offsetY,
                                                            content = text
                                                    )
                                            )
                                        }
                                ) { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            }
                            .background(
                                    Color(0x33D3D3D3)
                            ) // Light translucent grey for visibility while editing
                            .border(1.dp, Color.Gray)
                            .padding(8.dp)
                            .widthIn(min = 100.dp, max = 300.dp)
    ) {
        BasicTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onAnnotationChanged(annotation.copy(xNorm = offsetX, yNorm = offsetY, content = newText))
                },
                textStyle =
                        TextStyle(
                                fontSize = annotation.fontSize.sp,
                                color = Color(annotation.color)
                        ),
        )
    }
}
