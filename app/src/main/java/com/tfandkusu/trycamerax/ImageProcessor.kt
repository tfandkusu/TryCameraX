package com.tfandkusu.trycamerax

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.concurrent.atomic.AtomicBoolean
import org.opencv.imgproc.Imgproc
import org.opencv.core.CvType.CV_8UC1
import kotlin.math.max


/**
 * 画像認識用データをBitmapに変換するだけ
 */
class ImageProcessor {

    companion object {
        init {
            // soファイル読み込み
            System.loadLibrary("opencv_java4")
        }
    }

    /**
     * 処理中フラグ
     */
    private val processing = AtomicBoolean(false)

    var listener = { _: Bitmap ->

    }

    /**
     * 画像処理を行う
     */
    fun process(image: ImageProxy, rotationDegree: Int) {
        if (processing.compareAndSet(false, true))
            return
        // 入力はYUV_422_888形式
        // https//developer.android.com/training/camerax/analyze
        // OpenCVのMatへの変換方法
        // https://stackoverflow.com/questions/30510928/convert-android-camera2-api-yuv-420-888-to-rgb
        // 3チャンネルを1配列に統合
        // imageはこのメソッドが終わると閉じるので、情報取得までは非同期にしない
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val width = image.width
        val height = image.height
        GlobalScope.launch {
            // OpenCVでBGRに変換
            val yuv = Mat(height + height / 2, width, CV_8UC1)
            yuv.put(0, 0, nv21)
            val bgr = Mat()
            Imgproc.cvtColor(yuv, bgr, Imgproc.COLOR_YUV2BGR_NV21)
            // サイズを縮小する
            val maxSize = max(bgr.cols(), bgr.rows())
            val shrink = Mat(bgr.rows() * 640 / maxSize, bgr.cols() * 640 / maxSize, CvType.CV_8UC3)
            Imgproc.resize(bgr, shrink, shrink.size())
            // TODO 回転する

            // rgbaに変換する
            val rgba = Mat(shrink.rows(), shrink.cols(), CvType.CV_8UC4)
            Imgproc.cvtColor(shrink, rgba, Imgproc.COLOR_BGR2RGBA)
            val bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, bitmap)
            launch(Dispatchers.Main) {
                listener(bitmap)
            }
            Log.d("Takada","IP end")
            processing.set(false)
        }
    }

}