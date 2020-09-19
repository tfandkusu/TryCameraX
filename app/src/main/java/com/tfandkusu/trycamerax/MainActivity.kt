package com.tfandkusu.trycamerax

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        init {
            // soファイル読み込み
            System.loadLibrary("opencv_java4")
        }
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // プレビュー表示設定
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            setUpCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private val executorService = Executors.newSingleThreadExecutor()

    private fun setUpCamera(cameraProvider: ProcessCameraProvider) {
        Executors.newSingleThreadExecutor()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            // 回転後の解像度を設定する
            .setTargetResolution(Size(1440, 2560))
            .build()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

        // シャッターボタンの設定
        take.setOnClickListener {
            imageCapture.takePicture(
                executorService,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val buffer = image.planes[0].buffer
                        val size = buffer.remaining()
                        val jpeg = ByteArray(size)
                        buffer.get(jpeg, 0, size)
                        image.close()
                        val img = Imgcodecs.imdecode(MatOfByte(*jpeg), Imgcodecs.IMREAD_COLOR)
                        // RGBAに変換
                        val rgba = Mat(img.rows(), img.cols(), CvType.CV_8UC4)
                        Imgproc.cvtColor(img, rgba, Imgproc.COLOR_BGR2RGBA)
                        // Bitmapに変換
                        val bitmap =
                            Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(rgba, bitmap)
                        handler.post {
                            showBitmap(bitmap)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Timber.d(exception.toString())
                    }
                })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showBitmap(bitmap: Bitmap) {
        text.text = "size = (%d,%d)".format(bitmap.width, bitmap.height)
        image.setImageBitmap(bitmap)
    }

}
