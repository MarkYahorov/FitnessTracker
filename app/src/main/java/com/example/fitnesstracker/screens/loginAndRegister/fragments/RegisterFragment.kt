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
import android.widget.TextView
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.models.registration.RegistrationResponse


class RegisterFragment : Fragment() {

    private lateinit var welcomeText: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var repeatPassword: EditText
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var openLoginWindowBtn: Button
    private lateinit var registerBtn: Button

    private var navigator: ChangeScreen? = null
    private val textWatcher = createTextListener()
    private val repositoryImpl = App.INSTANCE.repositoryImpl

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as ChangeScreen
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        initAllViews(view)
        return view
    }

    private fun initAllViews(view: View) {
        welcomeText = view.findViewById(R.id.first_screen_text)
        email = view.findViewById(R.id.email_edit_text_to_login)
        password = view.findViewById(R.id.password_edit_text_to_login)
        repeatPassword = view.findViewById(R.id.repeat_edit_text)
        firstName = view.findViewById(R.id.first_name_edit_text)
        lastName = view.findViewById(R.id.last_name_edit_text)
        openLoginWindowBtn = view.findViewById(R.id.open_login_views_btn)
        registerBtn = view.findViewById(R.id.registr_btn)
    }

    override fun onStart() {
        super.onStart()
        addListeners()
        setListeners()
    }

    private fun setListeners() {
        openLoginWindowBtn.setOnClickListener {
            navigator?.goToLoginScreen()
        }
        registerBtn.setOnClickListener {
            repositoryImpl.registration(createRegistrationRequest())
                .continueWith {
                    if (it.error != null) {
                        //alertDialog
                    } else {
                        saveTokenInSharedPref(it.result.token)
                        navigator?.goToMainScreen(it.result,
                            firstName.text.toString(),
                            lastName.text.toString())
                    }
                }
        }
    }

    private fun createRegistrationRequest() = RegistrationRequest(
        email.text.toString(),
        password.text.toString(),
        firstName.text.toString(),
        lastName.text.toString()
    )

    private fun addListeners() {
        email.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)
        repeatPassword.addTextChangedListener(textWatcher)
        firstName.addTextChangedListener(textWatcher)
        lastName.addTextChangedListener(textWatcher)
    }

    private fun createTextListener(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                registerBtn.isEnabled = email.text.isNotEmpty()
                        && password.text.isNotEmpty()
                        && repeatPassword.text.isNotEmpty()
                        && firstName.text.isNotEmpty()
                        && lastName.text.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    private fun saveTokenInSharedPref(token: String) {
        activity?.getSharedPreferences("FITNESS_SHARED", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("CURRENT_TOKEN", token)
            ?.apply()
    }

    interface ChangeScreen {
        fun goToLoginScreen()
        fun goToMainScreen(
            registrationResponse: RegistrationResponse,
            firstName: String,
            lastName: String,
        )
    }

}