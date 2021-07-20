package com.example.fitnesstracker.screens.main

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fitnesstracker.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addListFragment()
    }

    private fun addListFragment(){
        supportFragmentManager.beginTransaction()
            .add(R.id.list_fragment_container,TrackListFragment.newInstance(getTokenFromSharedPref()))
            .addToBackStack("LIST_FRAGMENT")
            .commit()
    }

    private fun getTokenFromSharedPref(): String{
        return getSharedPreferences("FITNESS_SHARED", Context.MODE_PRIVATE)
            .getString("CURRENT_TOKEN", null)!!
    }
}