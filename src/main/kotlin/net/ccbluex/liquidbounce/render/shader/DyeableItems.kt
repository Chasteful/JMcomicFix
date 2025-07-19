package net.ccbluex.liquidbounce.render.shader

import java.awt.image.BufferedImage

object DyeableItems {

    fun applyDyeToImage(originalImage: BufferedImage, dyeColor: Int, isLeather: Boolean = true): BufferedImage {
        val dyedImage = BufferedImage(
            originalImage.width,
            originalImage.height,
            BufferedImage.TYPE_INT_ARGB
        )

        val red = (dyeColor shr 16) and 0xFF
        val green = (dyeColor shr 8) and 0xFF
        val blue = dyeColor and 0xFF

        if (isLeather) {
            applyLeatherDye(originalImage, dyedImage, red, green, blue)
        } else {
            applyRegularDye(originalImage, dyedImage, red, green, blue)
        }

        return dyedImage
    }

    private fun applyLeatherDye(
        originalImage: BufferedImage,
        dyedImage: BufferedImage,
        red: Int,
        green: Int,
        blue: Int
    ) {
        val defaultLeatherR = 160.0
        val defaultLeatherG = 101.0
        val defaultLeatherB = 64.0

        for (x in 0 until originalImage.width) {
            for (y in 0 until originalImage.height) {
                val originalPixel = originalImage.getRGB(x, y)
                val alpha = (originalPixel shr 24) and 0xFF

                if (alpha > 0) {
                    val originalRed = (originalPixel shr 16) and 0xFF
                    val originalGreen = (originalPixel shr 8) and 0xFF
                    val originalBlue = originalPixel and 0xFF

                    val grayFactor = (
                        (originalRed / defaultLeatherR) +
                            (originalGreen / defaultLeatherG) +
                            (originalBlue / defaultLeatherB)
                        ) / 3.0

                    val mixedRed = (grayFactor * red).toInt().coerceIn(0, 255)
                    val mixedGreen = (grayFactor * green).toInt().coerceIn(0, 255)
                    val mixedBlue = (grayFactor * blue).toInt().coerceIn(0, 255)

                    dyedImage.setRGB(
                        x, y,
                        (alpha shl 24) or (mixedRed shl 16) or (mixedGreen shl 8) or mixedBlue
                    )
                } else {
                    dyedImage.setRGB(x, y, originalPixel)
                }
            }
        }
    }

    private fun applyRegularDye(
        originalImage: BufferedImage,
        dyedImage: BufferedImage,
        red: Int,
        green: Int,
        blue: Int
    ) {
        for (x in 0 until originalImage.width) {
            for (y in 0 until originalImage.height) {
                val originalPixel = originalImage.getRGB(x, y)
                val alpha = (originalPixel shr 24) and 0xFF

                if (alpha > 0) {
                    val originalRed = (originalPixel shr 16) and 0xFF
                    val originalGreen = (originalPixel shr 8) and 0xFF
                    val originalBlue = originalPixel and 0xFF

                    val mixedRed = (originalRed * red) / 255
                    val mixedGreen = (originalGreen * green) / 255
                    val mixedBlue = (originalBlue * blue) / 255

                    dyedImage.setRGB(
                        x, y,
                        (alpha shl 24) or (mixedRed shl 16) or (mixedGreen shl 8) or mixedBlue
                    )
                } else {
                    dyedImage.setRGB(x, y, originalPixel)
                }
            }
        }
    }
}
