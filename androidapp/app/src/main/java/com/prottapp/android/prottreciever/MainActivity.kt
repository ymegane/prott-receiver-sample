package com.prottapp.android.prottreciever

import android.content.Intent
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager

import com.google.firebase.messaging.FirebaseMessaging
import com.prottapp.android.prottreciever.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import android.util.Log
import android.view.View

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "MainActivity"
        val REQUEST_G_SIGN_IN = 100
    }

    val viewModel = MainViewModel(this)
    val binding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseMessaging.getInstance().subscribeToTopic("preview")

        binding.viewModel = viewModel
        initLayout(binding)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_G_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    private fun initLayout(binding: ActivityMainBinding) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)

        binding.buttonSignIn.setOnClickListener { viewModel.signIn(REQUEST_G_SIGN_IN) }
        binding.buttonSignOut.setOnClickListener {
            viewModel.signOut()
            updateUI(false)
        }

        binding.switchReceive.setOnCheckedChangeListener { _, check ->
            pref.edit().putBoolean("enable", check).apply()

            if (check) {
                viewModel.createDevice()
            } else {
                viewModel.deleteDevice()
            }
        }

        binding.switchReceive.isChecked = pref.getBoolean("enable", false)

        when(viewModel.getCurrentUser() != null) {
            true -> {
                viewModel.createDevice()
                updateUI (true)
            }
            else -> updateUI(false)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess)
        if (result.isSuccess) {
            // Signed in successfully, show authenticated UI.
            val acct = result.signInAccount
            viewModel.firebaseAuthWithGoogle(acct!!)
            updateUI(signIn = true)
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(signIn = false)
        }
    }

    private fun updateUI(signIn: Boolean) {
        when(signIn) {
            true -> {
                viewModel.showLoginUser()
                binding.switchReceive.visibility = View.VISIBLE
                binding.buttonSignOut.visibility = View.VISIBLE
                binding.buttonSignIn.visibility = View.GONE
            }
            else -> {
                binding.textSignInUser.text = ""
                binding.buttonSignIn.visibility = View.VISIBLE
                binding.buttonSignOut.visibility = View.GONE
                binding.switchReceive.visibility = View.GONE
            }

        }
    }
}
