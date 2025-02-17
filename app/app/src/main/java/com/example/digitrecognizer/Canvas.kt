package com.example.digitrecognizer

import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun Canvas() {
    // Canvas configuration - change these values to adjust the app behavior
    val canvasSizePixels = 28
    val maxBrushSize = 7  // Maximum brush diameter in pixels

    var pixels by remember {
        mutableStateOf(FloatArray(canvasSizePixels * canvasSizePixels) { 1f })
    }
    var currentPaintValue by remember { mutableFloatStateOf(0f) }
    var brushSize by remember { mutableIntStateOf(1) }  // Brush diameter in pixels
    var boxSize by remember { mutableStateOf(Size.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Drawing canvas
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.LightGray)
            .onSizeChanged { size ->
                boxSize = Size(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(boxSize, brushSize) {
                if (boxSize.width == 0f || boxSize.height == 0f) return@pointerInput

                val pixelWidth = boxSize.width / canvasSizePixels
                val pixelHeight = boxSize.height / canvasSizePixels

                detectDragGestures(onDragStart = { offset ->
                    handleDragEvent(
                        offset = offset,
                        pixelWidth = pixelWidth,
                        pixelHeight = pixelHeight,
                        canvasSize = canvasSizePixels,
                        brushSize = brushSize,
                        paintValue = currentPaintValue
                    ) { x, y ->
                        pixels = updatePixel(pixels, x, y, canvasSizePixels, currentPaintValue)
                    }
                }, onDrag = { change, _ ->
                    handleDragEvent(
                        offset = change.position,
                        pixelWidth = pixelWidth,
                        pixelHeight = pixelHeight,
                        canvasSize = canvasSizePixels,
                        brushSize = brushSize,
                        paintValue = currentPaintValue
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

        // Controls
        Text("Brush value: ${"%.2f".format(currentPaintValue)}")
        Slider(
            value = currentPaintValue,
            onValueChange = { currentPaintValue = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Brush size: $brushSize")
        Slider(
            value = brushSize.toFloat(),
            onValueChange = { brushSize = it.toInt() },
            valueRange = 1f..maxBrushSize.toFloat(),
            steps = maxBrushSize - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                pixels = FloatArray(canvasSizePixels * canvasSizePixels) { 1f }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Canvas")
        }

        val context = LocalContext.current
        Button(
            onClick = {
                val guess = recognizeDigit(context, pixels)
                Toast.makeText(
                    context, "Digit: $guess", Toast.LENGTH_SHORT
                ).show()
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guess")
        }
    }
}

private fun handleDragEvent(
    offset: Offset,
    pixelWidth: Float,
    pixelHeight: Float,
    canvasSize: Int,
    brushSize: Int,
    paintValue: Float,
    updatePixels: (x: Int, y: Int) -> Unit
) {
    val centerX = (offset.x / pixelWidth).toInt().coerceIn(0, canvasSize - 1)
    val centerY = (offset.y / pixelHeight).toInt().coerceIn(0, canvasSize - 1)

    val radius = (brushSize - 1) / 2
    for (xOffset in -radius..radius) {
        for (yOffset in -radius..radius) {
            val x = (centerX + xOffset).coerceIn(0, canvasSize - 1)
            val y = (centerY + yOffset).coerceIn(0, canvasSize - 1)
            updatePixels(x, y)
        }
    }
}

private fun updatePixel(
    currentPixels: FloatArray, x: Int, y: Int, canvasSize: Int, value: Float
): FloatArray {
    val newPixels = currentPixels.copyOf()
    val index = y * canvasSize + x
    newPixels[index] = value
    return newPixels
}

private fun recognizeDigit(context: Context, pixels: FloatArray): Int {
    val invertedPixels = pixels.map { 1 - it }.toFloatArray()

    val model = readNpz(context, "model.npz")
    val layers = transformLayers(model)

    return feedForward(invertedPixels, layers.weights, layers.biases, 0)
}

data class TransformedLayers(
    val layerSizes: FloatArray, val biases: Array<FloatArray>, val weights: Array<Array<FloatArray>>
)

fun transformLayers(map: Map<String, Array<FloatArray>>): TransformedLayers {
    val layerSizesArray =
        map["layer_sizes"] ?: throw IllegalArgumentException("Missing 'layer_sizes' in the map")
    val layerSizes = layerSizesArray.firstOrNull()
        ?: throw IllegalArgumentException("'layer_sizes' array is empty")
    val numLayers = layerSizes.size - 1

    val biases = Array(numLayers) { i ->
        map["biases_$i"]?.firstOrNull()
            ?: throw IllegalArgumentException("Missing 'biases_$i' in the map")
    }

    val weights = Array(numLayers) { i ->
        map["weights_$i"] ?: throw IllegalArgumentException("Missing 'weights_$i' in the map")
    }

    return TransformedLayers(layerSizes, biases, weights)
}

fun feedForward(
    input: FloatArray, weights: Array<Array<FloatArray>>, biases: Array<FloatArray>, l: Int
): Int {
    if (l >= weights.size) {
        var maxValueIndex = 0
        var maxValue = input[0]
        for (i in 1 until input.size) {
            if (input[i] > maxValue) {
                maxValue = input[i]
                maxValueIndex = i
            }
        }

        return maxValueIndex;
    }

    val z = FloatArray(weights[l].size)
    for (i in weights[l].indices) {
        z[i] = biases[l][i]
        for (j in input.indices) {
            z[i] += weights[l][i][j] * input[j]
        }
    }

    val activation = FloatArray(z.size)
    for (i in z.indices) {
        activation[i] = 1 / (1 + kotlin.math.exp(-z[i])) // Sigmoid activation function
    }

    return feedForward(activation, weights, biases, l + 1)
}