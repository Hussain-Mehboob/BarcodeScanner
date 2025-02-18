package com.hussainmehboob.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hussainmehboob.barcodescanner.databinding.ActivityBarcodeScannerBinding

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var binding: ActivityBarcodeScannerBinding

    // Define a request code for camera permission
    private val CAMERA_PERMISSION_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        barcodeScanner = BarcodeScanning.getClient()

        // Check and request camera permission
        startCamera()

    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            initializeCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.previewView.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val imageAnalysis = ImageAnalysis.Builder()
                .build()

            // Set up the analyzer to detect barcodes
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Process the image for barcodes
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        handleBarcode(barcode)
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        // Handle barcode
        val valueType = barcode.valueType
        val barcodeValue = when (valueType) {
            Barcode.TYPE_WIFI -> barcode.wifi?.ssid
            Barcode.TYPE_URL -> barcode.url?.url
            Barcode.TYPE_TEXT -> barcode.displayValue
            else -> "Unsupported type"
        }

        // Show Toast Message to inform the user that item is added to the cart
        Toast.makeText(this, "Scanned: $barcodeValue", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        initializeCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the camera again
                initializeCamera()
            } else {
                // Permission denied, show an alert dialog
                AlertDialog.Builder(this)
                    .setTitle("Permission Denied")
                    .setMessage("Camera permission is required to scan barcodes.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        startCamera()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        finishAffinity()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
}
