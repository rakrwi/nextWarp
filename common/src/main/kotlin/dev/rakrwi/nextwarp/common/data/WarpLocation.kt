package dev.rakrwi.nextwarp.common.data

data class WarpLocation(
    val name: String,
    val server: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)