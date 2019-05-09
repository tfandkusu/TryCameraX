package com.tfandkusu.trycamerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Rational
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // プレビュー設定
        val previewConfig = PreviewConfig.Builder()
            .setTargetAspectRatio(Rational(16,9))
            .build()
        // プレビューを作る
        val preview = Preview(previewConfig)
        // プレビューの出力先をTextureViewに設定
        preview.setOnPreviewOutputUpdateListener {
            textureView.surfaceTexture = it.surfaceTexture
        }
        CameraX.bindToLifecycle(this, preview)
    }
}
