package com.example.fitnesstracker.screens.loginAndRegister.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.login.LoginResponse


class LoginFragment : Fragment() {

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var loginBtn: Button
    private lateinit var goToRegisterScreenBtn: Button

    private val textWatcher = createTextListener()
    private val repositoryImpl = App.INSTANCE.repositoryImpl
    private var navigator: ChangeScreen? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as ChangeScreen
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        initAllView(view)
        return view
    }

    private fun initAllView(view: View) {
        emailText = view.findViewById(R.id.email_edit_text_to_login)
        passwordText = view.findViewById(R.id.password_edit_text_to_login)
        loginBtn = view.findViewById(R.id.login_btn)
        goToRegisterScreenBtn = view.findViewById(R.id.go_to_registr_screen_btn)
    }

    override fun onStart() {
        super.onStart()
        addTextListeners()
        setButtonsListeners()
    }

    private fun setButtonsListeners() {
        loginBtn.setOnClickListener {
            repositoryImpl.login(createLoginRequest())
                .continueWith {
                    if (it.error != null) {
                        // AlertDialog
                    } else {
                        saveTokenInSharedPref(it.result.token)
                        navigator?.goToMainScreen(it.result)
                    }
                }
        }
        goToRegisterScreenBtn.setOnClickListener {
            navigator?.goToRegisterScreen()
        }
    }

    private fun saveTokenInSharedPref(token: String) {
        activity?.getSharedPreferences("FITNESS_SHARED", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("CURRENT_TOKEN", token)
            ?.apply()
    }

    private fun addTextListeners() {
        emailText.addTextChangedListener(textWatcher)
        passwordText.addTextChangedListener(textWatcher)
    }

    private fun createLoginRequest() =
        LoginRequest(emailText.text.toString(), passwordText.text.toString())

    private fun createTextListener(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loginBtn.isEnabled = emailText.text.isNotEmpty() && passwordText.text.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    interface ChangeScreen {
        fun goToRegisterScreen()
        fun goToMainScreen(loginResponse: LoginResponse)
    }
}