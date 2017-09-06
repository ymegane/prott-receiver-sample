package com.prottapp.android.prottreciever

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (!pref.getBoolean("enable", false)) {
            return
        }

        // Check if message contains a data payload.
        if (remoteMessage!!.data.isNotEmpty()) {
            val data = remoteMessage.data
            Log.d(TAG, "Message data payload: " + data)

            if (data.containsKey("shareUrl")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data["shareUrl"]))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.w("MyFirebaseMessaging", e)
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification

            Log.d(TAG, "Message Notification Body: " + notification.body)
        }
    }

    companion object {
        private val TAG = "MyFirebaseMessaging"
    }
}
