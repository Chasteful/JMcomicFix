package net.ccbluex.liquidbounce.utils.math

fun getRandomInRange(min: Float, max: Float): Float {
    return (Math.random() * (max - min) + min).toFloat()
}
