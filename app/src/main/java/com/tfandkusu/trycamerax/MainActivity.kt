package com.tfandkusu.trycamerax

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
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

//        val imageCapture = ImageCapture.Builder()
//            .setTargetRotation(display.rotation)
//            .build()
//
//        // プレビュー設定
//        val previewConfig = PreviewConfig.Builder()
//            .build()
//        // プレビューを作る
//        val preview = Preview(previewConfig)
//        // 静止画撮影設定
//        val imageCaptureConfig = ImageCaptureConfig.Builder()
//            .setTargetRotation(windowManager.defaultDisplay.rotation)
//            .build()
//        val imageCapture = ImageCapture(imageCaptureConfig)
//        // 画像認識設定
//        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
//            .setTargetRotation(windowManager.defaultDisplay.rotation)
//            .setTargetResolution(Size(1920, 1080))
//            .build()
//        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
//        imageAnalysis.setAnalyzer { image, rotationDegree ->
//            // 実はメインスレッド
//            ip.process(image, rotationDegree)
//        }
//
//        // プレビューの出力先をTextureViewに設定
//        preview.setOnPreviewOutputUpdateListener {
//            textureView.surfaceTexture = it.surfaceTexture
//        }
//        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis)
//
//        // TODO ライト
//
//        // 出力先
//        val file = File(/*filesDir.absolutePath*/ Environment.getExternalStorageDirectory().absolutePath + "/tmp.jpg")
//
//        // 撮影コールバック
//        val onImageCaptureListener = object : ImageCapture.OnImageSavedListener {
//            override fun onImageSaved(file: File) {
//                checkFile(file)
//            }
//
//            override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
//                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
//            }
//        }
//
//        // シャッターボタン
//        take.setOnClickListener {
//            imageCapture.takePicture(file, onImageCaptureListener)
//        }
//        // 画像処理結果表示
//        ip.listener = { bitmap ->
//            image.setImageBitmap(bitmap)
//        }
    }

//    private fun checkFile(file: File) {
//        GlobalScope.launch(Dispatchers.IO) {
//            val options = BitmapFactory.Options()
//            options.inJustDecodeBounds = true
//            BitmapFactory.decodeFile(file.absolutePath, options)
//            val message = "%d %d".format(options.outWidth, options.outHeight)
//            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
//        }
//    }

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

    private fun showBitmap(bitmap: Bitmap) {
        image.setImageBitmap(bitmap)
    }

}
