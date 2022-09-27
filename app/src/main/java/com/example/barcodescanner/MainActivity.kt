package com.example.barcodescanner

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun captureVideo() {

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalizer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalizer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalizer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    fun readBarcode(){
//        val image = object {}.javaClass.getResourceAsStream("1.png")
//        val bufferedImage = ImageIO.read(image)
//
//        val pixels = IntArray(bufferedImage.width * bufferedImage.height)
//
//        var index = 0
//        for (y in 0 until bufferedImage.height)
//            for (x in 0 until  bufferedImage.width) {
//                pixels[index] = bufferedImage.getRGB(x, y);
//                index++
//            }
//
//        val luminanceSource = RGBLuminanceSource(bufferedImage.width, bufferedImage.height, pixels)
//
//        val hybridBinarizer = HybridBinarizer(luminanceSource)
//
//        val binaryBitmap = BinaryBitmap(hybridBinarizer)
//
//        val multiFormatReader = MultiFormatReader()
//
//        val hints = mapOf<DecodeHintType,List<BarcodeFormat>>(
//            DecodeHintType.POSSIBLE_FORMATS to listOf<BarcodeFormat>(BarcodeFormat.EAN_13)
//        )
//
//        multiFormatReader.setHints(hints)
//
//        val result = multiFormatReader.decode(binaryBitmap)
//
//        println(result.text)
    }

    private class LuminosityAnalyser(private val listener: (luma: Double) -> Unit): ImageAnalysis.Analyzer{

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()
            listener(luma)

            image.close()
        }
    }

    private class BarcodeAnalizer(private val listener: (luma: String) -> Unit): ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {

            val imageByteArray = image.image?.toByteArray()

            val luminanceSource = PlanarYUVLuminanceSource(
                imageByteArray, image.width, image.height,
                0, 0, 640, 480, false
            )

            val hybridBinarizer = HybridBinarizer(luminanceSource)

            val binaryBitmap = BinaryBitmap(hybridBinarizer)

            val multiFormatReader = MultiFormatReader()

            val hints = mapOf<DecodeHintType,List<BarcodeFormat>>(
                DecodeHintType.POSSIBLE_FORMATS to listOf<BarcodeFormat>(BarcodeFormat.EAN_13)
            )

            multiFormatReader.setHints(hints)

            try {
                val result = multiFormatReader.decode(binaryBitmap)
                listener(result.text)
            } catch (exc: NotFoundException) {

            }

            image.close()

        }
    }



    companion object {
        private const val TAG = "BarcodeScanner"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

}

fun Image.toByteArray(): ByteArray {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)

    return yuvImage.yuvData
}