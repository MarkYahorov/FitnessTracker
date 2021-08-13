package com.example.fitnesstracker.screens.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.loginAndRegister.LoginAndRegisterActivity
import com.example.fitnesstracker.screens.main.MainActivity
import com.example.fitnesstracker.screens.main.list.TrackListFragment.Companion.IS_FIRST
import com.example.fitnesstracker.screens.running.RunningActivity
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.CURRENT_ACTIVITY

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var logo: ImageView

    private var isFirstInApp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        logo = findViewById(R.id.logo_image)
        isFirstInApp = getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getBoolean(IS_FIRST, true)
    }

    override fun onStart() {
        super.onStart()
        startAnimation()
        supportActionBar?.hide()
        goToNextActivity()
    }

    private fun startAnimation() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        logo.animation = anim
    }

    private fun goToNextActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFirstInApp) {
                startActivity(Intent(this, LoginAndRegisterActivity::class.java))
                finish()
            } else {
                val currentActivity = getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
                    .getInt(CURRENT_ACTIVITY, 0)
                if (currentActivity == 0) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, RunningActivity::class.java))
                    finish()
                }
            }
        }, 3000)
    }
}