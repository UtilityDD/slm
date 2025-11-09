package com.blackgrapes.smartlineman

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.chrisbanes.photoview.PhotoView
import java.io.IOException

class ImageZoomActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_NAME = "EXTRA_IMAGE_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_zoom)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_zoom)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageName = intent.getStringExtra(EXTRA_IMAGE_NAME)
        val photoView: PhotoView = findViewById(R.id.photo_view)

        if (imageName != null) {
            try {
                val inputStream = assets.open(imageName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                photoView.setImageBitmap(bitmap)
                inputStream.close()
            } catch (e: IOException) {
                Log.e("ImageZoomActivity", "Error loading image from assets: $imageName", e)
                Toast.makeText(this, "Could not load image.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}