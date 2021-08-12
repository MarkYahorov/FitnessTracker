package com.example.fitnesstracker.screens.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.loginAndRegister.LoginAndRegisterActivity
import com.example.fitnesstracker.screens.main.list.TrackListFragment
import com.example.fitnesstracker.screens.main.notification.NotificationFragment
import com.example.fitnesstracker.screens.main.track.TrackFragment
import com.example.fitnesstracker.screens.running.RunningActivity
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.CURRENT_ACTIVITY
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.TRACK_ID
import com.google.android.material.navigation.NavigationView

const val IS_FROM_NOTIFICATION = "IS FROM NOTIFICATION"

class MainActivity : AppCompatActivity(), TrackListFragment.Navigator {

    companion object {
        private const val NOTIFICATION = "NOTIFICATION"
        private const val TRACK = "TRACK"
        private const val RUNNING = "RUNNING"
        private const val LIST_FRAGMENT = "LIST_FRAGMENT"
        const val EMPTY_VALUE = ""
    }

    private lateinit var toolbar: Toolbar
    private lateinit var navDrawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var logout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putInt(CURRENT_ACTIVITY, 0)
            .apply()
        initAll()
        setToolbar()
        createDrawer()
        if (savedInstanceState == null) {
            addListFragment()
        }
    }

    private fun addListFragment() {
        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container_view,
                TrackListFragment.newInstance(getTokenFromSharedPref())
            )
            .addToBackStack(LIST_FRAGMENT)
            .commit()
    }

    private fun getTokenFromSharedPref(): String {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, EMPTY_VALUE).toString()
    }

    private fun initAll() {
        toolbar = findViewById(R.id.main_toolbar)
        navDrawer = findViewById(R.id.list_fragment_container)
        navigationView = findViewById(R.id.navigation_view)
        logout = findViewById(R.id.logout_item)
    }

    private fun setToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun createDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            navDrawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
    }

    override fun onStart() {
        super.onStart()
        setListeners()
    }

    private fun setListeners() {
        toggle.setToolbarNavigationClickListener {
            onBackPressed()
        }
        navDrawer.addDrawerListener(toggle)
        navigationView.setNavigationItemSelectedListener(createNavListener())
        setLogoutBtnListener()
    }

    private fun createNavListener() = NavigationView.OnNavigationItemSelectedListener {
        when (it.itemId) {
            R.id.go_to_main_screen -> {
                supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
                onBackPressed()
            }
            R.id.go_to_notification_screen -> {
                replaceFragment(NotificationFragment(), NOTIFICATION)
                onBackPressed()
            }
        }
        true
    }

    private fun setLogoutBtnListener() {
        logout.setOnClickListener {
            App.INSTANCE.repositoryImpl.clearDb(this)
                .continueWith({
                    startActivity(Intent(this, LoginAndRegisterActivity::class.java))
                    finish()
                }, Task.UI_THREAD_EXECUTOR)
        }
    }

    private fun updateToolbarBtn() {
        toggle.isDrawerIndicatorEnabled =
            !supportFragmentManager.popBackStackImmediate(RUNNING, POP_BACK_STACK_INCLUSIVE)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    private fun replaceFragment(fragment: Fragment, backStackName: String) {
        supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment)
            .addToBackStack(backStackName)
            .commit()
    }

    override fun goToRunningScreen(token: String, trackId: Int) {
        val intent = Intent(this, RunningActivity::class.java)
            .putExtra(IS_FROM_NOTIFICATION, false)
            .putExtra(TRACK_ID, trackId)
        startActivity(intent)
    }


    override fun goToTrackScreen(
        id: Int,
        serverId: Int,
        beginTime: Long,
        runningTime: Long,
        distance: Int,
        token: String
    ) {
        replaceFragment(
            TrackFragment.newInstance(
                id, serverId, beginTime,
                runningTime,
                distance,
                token
            ), TRACK
        )
    }

    override fun onBackPressed() {
        if (navDrawer.isDrawerVisible(GravityCompat.START)) {
            navDrawer.closeDrawer(GravityCompat.START)
            return
        }
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
            updateToolbarBtn()
            saveInSharedPref()
            return
        }
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        }
        super.onBackPressed()
    }

    private fun saveInSharedPref() {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(IS_FROM_NOTIFICATION, false)
            .apply()
    }

    override fun onStop() {
        super.onStop()
        toggle.toolbarNavigationClickListener = null
        navDrawer.removeDrawerListener(toggle)
        navigationView.setNavigationItemSelectedListener(null)
        logout.setOnClickListener(null)
    }
}