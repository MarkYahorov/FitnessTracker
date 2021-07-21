package com.example.fitnesstracker.screens.main

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.fitnesstracker.R
import com.example.fitnesstracker.screens.NotificationFragment
import com.example.fitnesstracker.screens.RunningFragment
import com.example.fitnesstracker.screens.TrackFragment
import com.example.fitnesstracker.screens.loginAndRegister.CURRENT_TOKEN
import com.example.fitnesstracker.screens.loginAndRegister.FITNESS_SHARED
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), TrackListFragment.Navigator {

    companion object {
        private const val NOTIFICATION = "NOTIFICATION"
        private const val TRACK = "TRACK"
        private const val RUNNING = "RUNNING"
        private const val LIST_FRAGMENT = "LIST_FRAGMENT"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var navDrawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var logout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAll()
        addListFragment()
    }

    private fun addListFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container_view,
                TrackListFragment.newInstance(getTokenFromSharedPref()))
            .addToBackStack(LIST_FRAGMENT)
            .commit()
    }

    private fun getTokenFromSharedPref(): String {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, null).toString()
    }

    private fun initAll() {
        toolbar = findViewById(R.id.main_toolbar)
        navDrawer = findViewById(R.id.list_fragment_container)
        navigationView = findViewById(R.id.navigation_view)
        logout = findViewById(R.id.logout_item)
    }

    override fun onStart() {
        super.onStart()
        setToolbar()
        createDrawer()
        setLogoutBtnListener()
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
        toggle.setToolbarNavigationClickListener {
            onBackPressed()
        }
        navDrawer.addDrawerListener(toggle)
        navigationView.setNavigationItemSelectedListener(createNavListener())
        updateToolbarBtn()
    }

    private fun createNavListener() = NavigationView.OnNavigationItemSelectedListener {
        when (it.itemId) {
            R.id.go_to_main_screen -> {
                supportFragmentManager.popBackStackImmediate("", POP_BACK_STACK_INCLUSIVE)
                Log.e("key", "MAIN SCREEN ${supportFragmentManager.backStackEntryCount}")
            }
            R.id.go_to_notification_screen -> {
                replaceFragment(NotificationFragment(), NOTIFICATION)
                Log.e("key", "NOTIFICATION SCREEN ${supportFragmentManager.backStackEntryCount}")
            }
        }
        true
    }

    private fun setLogoutBtnListener(){
        logout.setOnClickListener {
            Toast.makeText(this, "LOG OUT", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateToolbarBtn() {
        toggle.isDrawerIndicatorEnabled = supportFragmentManager.backStackEntryCount <= 1
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
        removeTransaction()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment)
            .addToBackStack(backStackName)
            .commit()
    }

    private fun removeTransaction() {
        if (supportFragmentManager.popBackStackImmediate(NOTIFICATION, POP_BACK_STACK_INCLUSIVE)
            || supportFragmentManager.popBackStackImmediate(TRACK, POP_BACK_STACK_INCLUSIVE)
            || supportFragmentManager.popBackStackImmediate(RUNNING, POP_BACK_STACK_INCLUSIVE)
        ) {
            supportFragmentManager.popBackStackImmediate("", POP_BACK_STACK_INCLUSIVE)
        }
    }

    override fun goToRunningScreen() {
        replaceFragment(RunningFragment(), NOTIFICATION)
        Log.e("key", "NOTIFICATION SCREEN ${supportFragmentManager.backStackEntryCount}")
    }

    override fun goToTrackScreen(id: Int) {
        val trackFragment = TrackFragment.newInstance(id)
        replaceFragment(trackFragment, TRACK)
        Log.e("key", "TRACK SCREEN ${supportFragmentManager.backStackEntryCount}")
    }
}