package com.example.superapp.utils

import com.dropbox.differ.Color
import com.dropbox.differ.Image
import com.dropbox.differ.ImageComparator
import com.dropbox.differ.Mask
import com.dropbox.differ.SimpleImageComparator

class ModifiedImage(
    private val original: Image,
    private val systemBarHeight: Int,
) : Image {
    override val width: Int = original.width
    override val height: Int = original.height - systemBarHeight

    override fun getPixel(
        x: Int,
        y: Int,
    ): Color = original.getPixel(x, y + systemBarHeight)
}

/**
 * Custom screenshot comparator that takes into account the system bar height.
 */
class CustomComparator(
    maxDistance: Float = 0.004f,
    hShift: Int = 0,
    vShift: Int = 0,
    private val systemBarHeight: Int = 50,
) : ImageComparator {
    private val simpleComparator = SimpleImageComparator(maxDistance, hShift, vShift)

    override fun compare(
        left: Image,
        right: Image,
        mask: Mask?,
    ): ImageComparator.ComparisonResult {
        val modifiedLeft = ModifiedImage(left, systemBarHeight)
        val modifiedRight = ModifiedImage(right, systemBarHeight)
        val res = simpleComparator.compare(modifiedLeft, modifiedRight, mask)

        // This mask is the one containing diffed pixels, so we offset it by systemBarHeight
        mask?.let {
            for (x in 0 until mask.width) {
                for (y in mask.height - systemBarHeight - 1 downTo 0) {
                    mask.setValue(x, y + systemBarHeight, mask.getValue(x, y))
                }
                for (y in 0 until systemBarHeight) {
                    mask.setValue(x, y, 0f)
                }
            }
        }
        return res
    }
}
