package com.tfandkusu.trycamerax

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // プレビュー設定
        val previewConfig = PreviewConfig.Builder()
            .build()
        // プレビューを作る
        val preview = Preview(previewConfig)
        // 静止画撮影設定
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()
        val imageCapture = ImageCapture(imageCaptureConfig)
        // 画像認識設定
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .setTargetResolution(Size(1920, 1080))
            .build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
        imageAnalysis.setAnalyzer { image, rotationDegree ->
            // 実はメインスレッド
            Log.d("Takada", "rotationDegree = " + rotationDegree)
            Log.d("Takada", "size = " + image.width + " " + image.height)
        }

        // プレビューの出力先をTextureViewに設定
        preview.setOnPreviewOutputUpdateListener {
            textureView.surfaceTexture = it.surfaceTexture
        }
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis)

        // TODO ライト

        // 出力先
        val file = File(filesDir.absolutePath + "/tmp.jpg")

        // 撮影コールバック
        val onImageCaptureListener = object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                checkFile(file)
            }

            override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        // シャッターボタン
        take.setOnClickListener {
            imageCapture.takePicture(file, onImageCaptureListener)
        }
    }

    private fun checkFile(file: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            val message = "%d %d".format(options.outWidth, options.outHeight)
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
        }

    }

}
