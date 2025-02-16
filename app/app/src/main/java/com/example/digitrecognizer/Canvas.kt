package com.example.digitrecognizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

@Composable
fun Canvas() {
    // Change this value to adjust the canvas resolution
    val canvasSizePixels = 32

    var pixels by remember {
        mutableStateOf(FloatArray(canvasSizePixels * canvasSizePixels) { 1f })
    }
    var currentPaintValue by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(Size.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.LightGray)
            .onSizeChanged { size ->
                boxSize = Size(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(boxSize) {
                if (boxSize.width == 0f || boxSize.height == 0f) return@pointerInput

                val pixelWidth = boxSize.width / canvasSizePixels
                val pixelHeight = boxSize.height / canvasSizePixels

                detectDragGestures(onDragStart = { offset ->
                    handleDragEvent(
                        offset, pixelWidth, pixelHeight, canvasSizePixels, currentPaintValue
                    ) { x, y ->
                        pixels = updatePixel(pixels, x, y, canvasSizePixels, currentPaintValue)
                    }
                }, onDrag = { change, _ ->
                    handleDragEvent(
                        change.position,
                        pixelWidth,
                        pixelHeight,
                        canvasSizePixels,
                        currentPaintValue
                    ) { x, y ->
                        pixels = updatePixel(pixels, x, y, canvasSizePixels, currentPaintValue)
                    }
                })
            }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pixelWidth = size.width / canvasSizePixels
                val pixelHeight = size.height / canvasSizePixels

                repeat(canvasSizePixels) { x ->
                    repeat(canvasSizePixels) { y ->
                        val grayscale = pixels[y * canvasSizePixels + x]
                        drawRect(
                            color = Color(grayscale, grayscale, grayscale),
                            topLeft = Offset(x * pixelWidth, y * pixelHeight),
                            size = Size(pixelWidth, pixelHeight)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Current brush value: ${"%.2f".format(currentPaintValue)}")
        Slider(
            value = currentPaintValue,
            onValueChange = { currentPaintValue = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                pixels = FloatArray(canvasSizePixels * canvasSizePixels) { 1f }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Canvas")
        }
    }
}

private fun handleDragEvent(
    offset: Offset,
    pixelWidth: Float,
    pixelHeight: Float,
    canvasSize: Int,
    paintValue: Float,
    updatePixels: (x: Int, y: Int) -> Unit
) {
    val x = (offset.x / pixelWidth).toInt().coerceIn(0, canvasSize - 1)
    val y = (offset.y / pixelHeight).toInt().coerceIn(0, canvasSize - 1)
    updatePixels(x, y)
}

private fun updatePixel(
    currentPixels: FloatArray, x: Int, y: Int, canvasSize: Int, value: Float
): FloatArray {
    val newPixels = currentPixels.copyOf()
    val index = y * canvasSize + x
    newPixels[index] = value
    return newPixels
}