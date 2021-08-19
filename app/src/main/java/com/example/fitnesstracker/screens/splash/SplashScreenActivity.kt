package com.example.fitnesstracker.screens.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstracker.App.Companion.MAIN_ACTIVITY_MARKER
import com.example.fitnesstracker.App.Companion.RUNNING_ACTIVITY_MARKER
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.loginAndRegister.LoginAndRegisterActivity
import com.example.fitnesstracker.screens.main.MainActivity
import com.example.fitnesstracker.screens.main.list.TrackListFragment.Companion.IS_FIRST
import com.example.fitnesstracker.screens.running.RunningActivity
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.CURRENT_ACTIVITY

class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private const val HANDLER_DELAY = 3000L
    }

    private var logoImage: ImageView? = null

    private var isFirstTimeInApp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        logoImage = findViewById(R.id.logo_image)
        isFirstTimeInApp = getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getBoolean(IS_FIRST, true)
    }

    override fun onStart() {
        super.onStart()

        startLogoAnimation()
        supportActionBar?.hide()
        goToNextActivity()
    }

    private fun startLogoAnimation() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        logoImage?.animation = anim
    }

    private fun goToNextActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFirstTimeInApp) {
                startActivity(Intent(this, LoginAndRegisterActivity::class.java))
            } else {
                val currentActivity = getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .getInt(CURRENT_ACTIVITY, MAIN_ACTIVITY_MARKER)
                if (currentActivity == MAIN_ACTIVITY_MARKER) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else if (currentActivity == RUNNING_ACTIVITY_MARKER) {
                    startActivity(Intent(this, RunningActivity::class.java))
                }
            }
            finish()
        }, HANDLER_DELAY)
    }

    override fun onDestroy() {
        super.onDestroy()
        logoImage = null
    }
}