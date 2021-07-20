package com.example.fitnesstracker.screens.loginAndRegister.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.fitnesstracker.R

class RegisterAndLoginFragment : Fragment() {

    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

    private var navigator: ChangeScreen? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as ChangeScreen
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_and_login, container, false)
        initAllViews(view)
        return view
    }

    private fun initAllViews(view: View){
        loginBtn = view.findViewById(R.id.go_to_login_screen)
        registerBtn = view.findViewById(R.id.go_to_register_screen)
    }

    override fun onStart() {
        super.onStart()
        setListeners()
    }

    private fun setListeners(){
        loginBtn.setOnClickListener {
            navigator?.goToLoginScreen()
        }
        registerBtn.setOnClickListener {
            navigator?.goToRegisterScreen()
        }
    }

    interface ChangeScreen{
        fun goToLoginScreen()
        fun goToRegisterScreen()
    }
}