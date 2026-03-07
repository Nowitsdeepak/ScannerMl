package com.ml.android.scanner.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Utility functions for image format conversions.
 */
object ImageUtils {
    
    /**
     * Convert YUV_420_888 Image to RGBA Mat
     */
    fun yuvToRgba(image: Image): Mat {
        val rgbaMat = Mat()
        
        if (image.format == ImageFormat.YUV_420_888 && image.planes.size == 3) {
            val chromaPixelStride = image.planes[1].pixelStride
            
            if (chromaPixelStride == 2) {
                // Chroma channels are interleaved
                assert(image.planes[0].pixelStride == 1)
                assert(image.planes[2].pixelStride == 2)
                
                val yPlane = image.planes[0].buffer
                val uvPlane1 = image.planes[1].buffer
                val uvPlane2 = image.planes[2].buffer
                
                val yMat = Mat(image.height, image.width, CvType.CV_8UC1, yPlane)
                val uvMat1 = Mat(image.height / 2, image.width / 2, CvType.CV_8UC2, uvPlane1)
                val uvMat2 = Mat(image.height / 2, image.width / 2, CvType.CV_8UC2, uvPlane2)
                val addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr()
                
                if (addrDiff > 0) {
                    assert(addrDiff == 1L)
                    Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12)
                } else {
                    assert(addrDiff == -1L)
                    Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
                }
            } else {
                // Chroma channels are not interleaved
                val yuvBytes = ByteArray(image.width * (image.height + image.height / 2))
                val yPlane = image.planes[0].buffer
                val uPlane = image.planes[1].buffer
                val vPlane = image.planes[2].buffer
                
                yPlane.get(yuvBytes, 0, image.width * image.height)
                
                val chromaRowStride = image.planes[1].rowStride
                val chromaRowPadding = chromaRowStride - image.width / 2
                
                var offset = image.width * image.height
                
                if (chromaRowPadding == 0) {
                    uPlane.get(yuvBytes, offset, image.width * image.height / 4)
                    offset += image.width * image.height / 4
                    vPlane.get(yuvBytes, offset, image.width * image.height / 4)
                } else {
                    for (i in 0 until image.height / 2) {
                        uPlane.get(yuvBytes, offset, image.width / 2)
                        offset += image.width / 2
                        if (i < image.height / 2 - 1) {
                            uPlane.position(uPlane.position() + chromaRowPadding)
                        }
                    }
                    for (i in 0 until image.height / 2) {
                        vPlane.get(yuvBytes, offset, image.width / 2)
                        offset += image.width / 2
                        if (i < image.height / 2 - 1) {
                            vPlane.position(vPlane.position() + chromaRowPadding)
                        }
                    }
                }
                
                val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
                yuvMat.put(0, 0, yuvBytes)
                Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4)
            }
        }
        
        return rgbaMat
    }
    
    /**
     * Convert Mat to Bitmap
     */
    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = createBitmap(mat.cols(), mat.rows())
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    
    /**
     * Convert Bitmap to Mat
     */
    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
}
