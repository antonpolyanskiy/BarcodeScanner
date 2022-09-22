package com.example.barcodescanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

}