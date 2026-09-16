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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.core.ink.models.TextAnnotation
import kotlin.math.roundToInt

@Composable
fun TextBoxWidget(annotation: TextAnnotation, onAnnotationChanged: (TextAnnotation) -> Unit) {
    var offsetX by remember(annotation.id) { mutableFloatStateOf(annotation.x) }
    var offsetY by remember(annotation.id) { mutableFloatStateOf(annotation.y) }
    var textFieldValue by remember(annotation.id) {
        mutableStateOf(
            TextFieldValue(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        )
    }

    LaunchedEffect(annotation.content) {
        if (annotation.content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        }
    }

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
                                    content = textFieldValue.text
                                )
                            )
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .background(Color(0x33D3D3D3))
                .border(1.dp, Color.Gray)
                .padding(8.dp)
                .widthIn(min = 100.dp, max = 300.dp)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onAnnotationChanged(
                    annotation.copy(
                        xNorm = offsetX,
                        yNorm = offsetY,
                        content = newValue.text
                    )
                )
            },
            textStyle =
                TextStyle(
                    fontSize = annotation.fontSize.sp,
                    color = Color(annotation.color)
                ),
        )
    }
}
