package com.prottapp.android.prottreciever

import android.databinding.ObservableField
import android.os.Build
import com.google.android.gms.auth.api.Auth
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import android.widget.Toast
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference


class MainViewModel(val activity: MainActivity) : GoogleApiClient.OnConnectionFailedListener {
    companion object {
        val TAG = "MainViewModel"
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.w(TAG, "onConnectionFailed" +  p0.toString())
    }

    val signInUser = ObservableField<String>()

    val gso by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()!!
    }

    val googleApiClient by lazy {
        GoogleApiClient.Builder(activity)
                .enableAutoManage(activity, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()!!
    }

    val auth = FirebaseAuth.getInstance()!!

    var authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            Log.d("MainActivity", "onAuthStateChanged:signed_in:" + user.uid)
        } else {
            Log.d("MainActivity", "onAuthStateChanged:signed_out")
        }
    }

    fun onStart() {
        auth.addAuthStateListener(authListener)
    }

    fun onStop() {
        auth.removeAuthStateListener(authListener)
    }

    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    fun signIn(requestCode: Int) {
        if (!googleApiClient.isConnected) {
            return
        }
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        activity.startActivityForResult(signInIntent, requestCode)
    }

    fun signOut() {
        if (!googleApiClient.isConnected) {
            return
        }
        deleteDevice()
        Auth.GoogleSignInApi.signOut(googleApiClient)
        FirebaseAuth.getInstance().signOut()
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, { task ->
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful) {
                        Log.w(TAG, "signInWithCredential", task.exception)
                        Toast.makeText(activity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    showLoginUser()
                    createDevice()
                })
    }

    fun createDevice() {
        getUserDatabase()?.let {
            val fbInstance = FirebaseInstanceId.getInstance()
            val user = Device(fbInstance.token, Build.MODEL, Build.VERSION.SDK_INT)
            it.child("devices").child(fbInstance.id)?.setValue(user)
        }
    }

    fun deleteDevice() {
        getUserDatabase()?.let {
            val fbInstance = FirebaseInstanceId.getInstance()
            it.child("devices").child(fbInstance.id).removeValue()
        }
    }

    fun getUserDatabase() : DatabaseReference? {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            return FirebaseDatabase.getInstance().reference.child("users").child(it.uid)!!
        }
        return null
    }

    fun showLoginUser() {
        FirebaseAuth.getInstance().currentUser?.let {
            signInUser.set(it.displayName + " (" + it.email + ")")
        }
    }
}