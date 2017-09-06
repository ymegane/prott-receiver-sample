package com.prottapp.android.prottreciever

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class MyFirebaseInstanceIDService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()

        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.d("MyFirebase", "Refreshed token: " + refreshedToken!!)
    }
}
