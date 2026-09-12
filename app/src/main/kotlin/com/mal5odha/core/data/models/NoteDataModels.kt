package com.mal5odha.core.data.models

import com.google.gson.annotations.SerializedName

data class Ml5Document(
        @SerializedName("version") val version: Int = 1,
        @SerializedName("pages") val pages: List<Ml5Page> = emptyList()
)

data class Ml5Page(
        @SerializedName("id") val id: String,
        @SerializedName("orderIndex") val orderIndex: Int,
        @SerializedName("backgroundType") val backgroundType: String,
        @SerializedName("backgroundData") val backgroundData: String? = null,
        @SerializedName("isBookmarked") val isBookmarked: Boolean = false,
        @SerializedName("widthPt") val widthPt: Float = 595f,
        @SerializedName("heightPt") val heightPt: Float = 842f
) {
        val aspectRatio: Float
                get() = if (heightPt > 0f) widthPt / heightPt else (595f / 842f)
}

data class Ml5Stroke(
        @SerializedName("id") val id: String,
        @SerializedName("color") val color: Int,
        @SerializedName("width") val width: Float,
        @SerializedName("tool") val tool: String,
        @SerializedName("points") val points: List<Ml5Point>,
        @SerializedName("audioSessionId") val audioSessionId: String? = null,
        @SerializedName("audioTimestampMs") val audioTimestampMs: Long = 0L,
        @SerializedName("layerId") val layerId: String? = null
)

data class Ml5Point(
        @SerializedName("x") val x: Float,
        @SerializedName("y") val y: Float,
        @SerializedName("pressure") val pressure: Float,
        @SerializedName("timestamp") val timestamp: Long
)

data class Ml5TextAnnotation(
        @SerializedName("id") val id: String,
        @SerializedName("text") val text: String,
        @SerializedName("x") val x: Float,
        @SerializedName("y") val y: Float,
        @SerializedName("width") val width: Float,
        @SerializedName("height") val height: Float,
        @SerializedName("fontSize") val fontSize: Float,
        @SerializedName("color") val color: Int,
        @SerializedName("rotation") val rotation: Float = 0f,
        @SerializedName("backgroundColor") val backgroundColor: Int? = null,
        @SerializedName("alignment") val alignment: String = "START",
        @SerializedName("audioSessionId") val audioSessionId: String? = null,
        @SerializedName("audioTimestampMs") val audioTimestampMs: Long = 0L
)

data class Ml5MediaAnnotation(
        @SerializedName("id") val id: String,
        @SerializedName("localPath") val localPath: String,
        @SerializedName("x") val x: Float,
        @SerializedName("y") val y: Float,
        @SerializedName("width") val width: Float,
        @SerializedName("height") val height: Float,
        @SerializedName("rotation") val rotation: Float = 0f
)
