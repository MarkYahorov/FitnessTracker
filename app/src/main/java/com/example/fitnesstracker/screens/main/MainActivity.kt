package com.example.fitnesstracker.screens.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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
import com.example.fitnesstracker.models.tracks.Tracks
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.example.fitnesstracker.screens.loginAndRegister.LoginAndRegisterActivity
import com.example.fitnesstracker.screens.main.list.TrackListFragment
import com.example.fitnesstracker.screens.main.notification.NotificationFragment
import com.example.fitnesstracker.screens.main.track.TrackFragment
import com.example.fitnesstracker.screens.running.RunningActivity
import com.example.fitnesstracker.screens.running.RunningActivity.Companion.TRACK_ID
import com.google.android.material.navigation.NavigationView

const val IS_FROM_NOTIFICATION = "IS FROM NOTIFICATION"

class MainActivity : AppCompatActivity(), TrackListFragment.Navigator {

    companion object {
        private const val DEFAULT_BACK_STACK_SIZE = 1
        private const val NOTIFICATION = "NOTIFICATION"
        private const val TRACK = "TRACK"
        private const val LIST_FRAGMENT = "LIST_FRAGMENT"
        private const val CURRENT_TRACK = "CURRENT_TRACK"
    }

    private var toolbar: Toolbar? = null
    private var navDrawer: DrawerLayout? = null
    private var toggle: ActionBarDrawerToggle? = null
    private var navigationView: NavigationView? = null
    private var logout: ConstraintLayout? = null

    private var track: Tracks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAll()
        setToolbar()
        setDrawer()
        if (savedInstanceState == null) {
            addListFragment()
        } else {
            track = savedInstanceState.getParcelable(CURRENT_TRACK)
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                replaceFragmentsIfLocatedInBackStack(container = R.id.fragment_container_view)
            } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                replaceFragmentsIfLocatedInBackStack(container = R.id.fragment_container_view_for_all)
            }
        }
    }

    private fun replaceFragmentsIfLocatedInBackStack(container: Int) {
        if (supportFragmentManager.popBackStackImmediate(
                TRACK,
                POP_BACK_STACK_INCLUSIVE
            ) && track != null
        ) {
            track?.let {
                val token = getTokenFromSharedPref()
                if (token != null) {
                    replaceFragment(
                        fragment = TrackFragment.newInstance(
                            id = it.id,
                            serverId = it.serverId,
                            beginTime = it.beginTime,
                            runningTime = it.time,
                            distance = it.distance,
                            token = token
                        ),
                        backStackName = TRACK,
                        container = container
                    )
                }
            }
        } else if (supportFragmentManager.popBackStackImmediate(
                NOTIFICATION,
                POP_BACK_STACK_INCLUSIVE
            )
        ) {
            replaceFragment(
                fragment = NotificationFragment(),
                backStackName = NOTIFICATION,
                container = container
            )
        }
    }

    private fun addListFragment() {
        val token = getTokenFromSharedPref()
        if (token != null) {
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container_view,
                    TrackListFragment.newInstance(token = token)
                )
                .addToBackStack(LIST_FRAGMENT)
                .commit()
        }
    }

    private fun getTokenFromSharedPref(): String? {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, null)
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

    private fun setDrawer() {
        toggle = ActionBarDrawerToggle(
            this@MainActivity,
            navDrawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle?.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        setListeners()
    }

    private fun setListeners() {
        toggle?.setToolbarNavigationClickListener {
            onBackPressed()
        }
        toggle?.let { navDrawer?.addDrawerListener(it) }
        navigationView?.setNavigationItemSelectedListener(createNavListener())
        setLogoutBtnListener()
    }

    private fun createNavListener() = NavigationView.OnNavigationItemSelectedListener {
        when (it.itemId) {
            R.id.go_to_main_screen -> {
                supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
                track = null
                onBackPressed()
            }
            R.id.go_to_notification_screen -> {
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    track = null
                    replaceFragment(
                        fragment = NotificationFragment(),
                        backStackName = NOTIFICATION,
                        container = R.id.fragment_container_view
                    )
                } else {
                    track = null
                    replaceFragment(
                        fragment = NotificationFragment(),
                        backStackName = NOTIFICATION,
                        container = R.id.fragment_container_view_for_all
                    )
                }
                onBackPressed()
            }
        }
        true
    }

    private fun setLogoutBtnListener() {
        logout?.setOnClickListener {
            App.INSTANCE.repositoryImpl.clearDb(this)
                .continueWith({
                    track = null
                    startActivity(Intent(this, LoginAndRegisterActivity::class.java))
                    finish()
                }, Task.UI_THREAD_EXECUTOR)
        }
    }

    private fun replaceFragment(fragment: Fragment, backStackName: String, container: Int) {
        supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
        supportFragmentManager.beginTransaction()
            .replace(container, fragment)
            .addToBackStack(backStackName)
            .commit()

    }

    override fun goToRunningScreen(token: String, trackId: Int) {
        val intent = Intent(this, RunningActivity::class.java)
            .putExtra(IS_FROM_NOTIFICATION, false)
            .putExtra(TRACK_ID, trackId)
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CURRENT_TRACK, track)
    }

    override fun goToTrackScreen(
        id: Int,
        serverId: Int?,
        beginTime: Long,
        runningTime: Long,
        distance: Int,
        token: String
    ) {
        track = Tracks(id, serverId, beginTime, runningTime, distance)
        val trackFragment = TrackFragment.newInstance(
            id = id,
            serverId = serverId,
            beginTime = beginTime,
            runningTime = runningTime,
            distance = distance,
            token = token
        )
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            replaceFragment(
                fragment = trackFragment,
                backStackName = TRACK,
                container = R.id.fragment_container_view
            )
        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            replaceFragment(
                fragment = trackFragment,
                backStackName = TRACK,
                container = R.id.fragment_container_view_for_all
            )
        }
    }

    override fun onBackPressed() {
        if (navDrawer?.isDrawerVisible(GravityCompat.START) == true) {
            navDrawer?.closeDrawer(GravityCompat.START)
            return
        }
        if (supportFragmentManager.backStackEntryCount > DEFAULT_BACK_STACK_SIZE) {
            supportFragmentManager.popBackStackImmediate(LIST_FRAGMENT, 0)
            saveMarkerFromNotificationSharedPref()
            track = null
            return
        }
        if (supportFragmentManager.backStackEntryCount == DEFAULT_BACK_STACK_SIZE) {
            finish()
        }
        super.onBackPressed()
    }

    private fun saveMarkerFromNotificationSharedPref() {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(IS_FROM_NOTIFICATION, false)
            .apply()
    }

    override fun onStop() {
        super.onStop()
        toggle?.toolbarNavigationClickListener = null
        toggle?.let { navDrawer?.removeDrawerListener(it) }
        navigationView?.setNavigationItemSelectedListener(null)
        logout?.setOnClickListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        toolbar = null
        navDrawer = null
        navigationView = null
        logout = null
    }
}