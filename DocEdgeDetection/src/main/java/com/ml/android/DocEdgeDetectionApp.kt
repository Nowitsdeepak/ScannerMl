package com.ml.android

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class DocEdgeDetectionApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
    }
}