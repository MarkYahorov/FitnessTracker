package com.example.fitnesstracker.screens.loginAndRegister

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import bolts.Task
import com.example.fitnesstracker.App
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.login.LoginRequest
import com.example.fitnesstracker.models.registration.RegistrationRequest
import com.example.fitnesstracker.screens.main.MainActivity
import java.util.regex.Pattern

const val FITNESS_SHARED = "FITNESS_SHARED"
const val CURRENT_TOKEN = "CURRENT_TOKEN"

class LoginAndRegisterActivity : AppCompatActivity() {

    companion object {
        private const val ERROR = "error"
        private const val CURRENT_STATE_OF_BTNS = "CURRENT_STATE_OF_BTNS"
        private const val CURRENT_EMAIL = "CURRENT_EMAIL"
        private const val CURRENT_PASSWORD = "CURRENT_PASSWORD"
        private const val CURRENT_REPEAT_PASSWORD = "CURRENT_REPEAT_PASSWORD"
        private const val CURRENT_FIRST_NAME = "CURRENT_FIRST_NAME"
        private const val CURRENT_LAST_NAME = "CURRENT_LAST_NAME"
        private const val CHECK_EMAIL_VALIDATE =
            "^[_A-Za-z0-9+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        private const val CHECK_PASSWORD_VALIDATE = "^[a-zA-Z0-9]+$"
    }

    private lateinit var welcomeText: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var repeatPassword: EditText
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var openLoginWindowBtn: Button
    private lateinit var registerBtn: Button

    private var isLoadingBtnActive = true
    private var textWatcher:TextWatcher? = null
    private val repositoryImpl = App.INSTANCE.repositoryImpl
    private var dialog: AlertDialog.Builder? = null
    private val emailPattern = Pattern.compile(CHECK_EMAIL_VALIDATE)
    private val passwordPattern = Pattern.compile(CHECK_PASSWORD_VALIDATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_and_register)

        if (getTokenFromSharedPref() != ""){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
        initAllViews()
    }

    private fun getTokenFromSharedPref(): String {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, "").toString()
    }

    private fun initAllViews() {
        welcomeText = findViewById(R.id.first_screen_text)
        email = findViewById(R.id.email_edit_text_to_login)
        password = findViewById(R.id.password_edit_text_to_login)
        repeatPassword = findViewById(R.id.repeat_edit_text)
        firstName = findViewById(R.id.first_name_edit_text)
        lastName = findViewById(R.id.last_name_edit_text)
        openLoginWindowBtn = findViewById(R.id.open_login_views_btn)
        registerBtn = findViewById(R.id.registr_btn)
        dialog = AlertDialog.Builder(this)
        textWatcher = createTextListener()
    }

    override fun onStart() {
        super.onStart()
        addTextListeners()
        setButtonsListeners()
    }

    private fun setButtonsListeners() {
        setLoginBtnListener()
        setRegistrBtnListener()
    }

    private fun setRegistrBtnListener() {
        registerBtn.setOnClickListener {
            if (isLoadingBtnActive) {
                setVisibilityViews(isVisible = true)
                welcomeText.text = getString(R.string.welcome_registr_text)
                openLoginWindowBtn.isEnabled = true
                registerBtn.isEnabled = false
            } else {
                if (checkEmailIsEqualsRegex() && checkPasswordIsEqualsRegex() && checkPasswordMatching()) {
                    repositoryImpl.registration(registrationRequest = createRegistrationRequest())
                        .continueWith({
                            when {
                                it.error != null -> {
                                    createAlertDialog(it.error.message)?.show()
                                }
                                it.result.status == ERROR -> {
                                    createAlertDialog(it.result.error)?.show()
                                }
                                else -> {
                                    saveTokenInSharedPref(token = it.result.token)
                                    createIntent()
                                }
                            }
                        }, Task.UI_THREAD_EXECUTOR)

                } else {
                    createErrorMessageForRegistration()
                }
            }
            isLoadingBtnActive = false
        }
    }

    private fun setLoginBtnListener() {
        openLoginWindowBtn.setOnClickListener {
            if (isLoadingBtnActive) {
                if (checkEmailIsEqualsRegex() && checkPasswordIsEqualsRegex()) {
                    repositoryImpl.login(loginRequest = createLoginRequest())
                        .continueWith({ response ->
                            when {
                                response.error != null -> {
                                    createAlertDialog(response.error.message)?.show()
                                }
                                response.result.status == ERROR -> {
                                    createAlertDialog(response.result.error)?.show()
                                }
                                else -> {
                                    saveTokenInSharedPref(token = response.result.token)
                                    createIntent()
                                }
                            }
                        }, Task.UI_THREAD_EXECUTOR)

                } else {
                    createErrorMessageForLogin()
                }
            } else {
                setVisibilityViews(isVisible = false)
                welcomeText.text = getString(R.string.welcome_login_text)
                openLoginWindowBtn.isEnabled = false
                registerBtn.isEnabled = true
            }
            isLoadingBtnActive = true
        }
    }

    private fun createErrorMessageForRegistration() {
        if (!checkEmailIsEqualsRegex() && !checkPasswordIsEqualsRegex() && !checkPasswordMatching()) {
            email.error = getString(R.string.error_message_email)
            password.error = getString(R.string.error_message_password)
            repeatPassword.error = getString(R.string.error_message_repeat_password)
        } else if (!checkEmailIsEqualsRegex()) {
            email.error = getString(R.string.error_message_email)
        } else if (!checkPasswordIsEqualsRegex()) {
            password.error = getString(R.string.error_message_password)
        } else if (!checkPasswordMatching()) {
            repeatPassword.error = getString(R.string.error_message_repeat_password)
        }
    }

    private fun createErrorMessageForLogin() {
        if (!checkEmailIsEqualsRegex() && !checkPasswordIsEqualsRegex()) {
            email.error = getString(R.string.error_message_email)
            password.error = getString(R.string.error_message_password)
        } else if (!checkEmailIsEqualsRegex()) {
            email.error = getString(R.string.error_message_email)
        } else if (!checkPasswordIsEqualsRegex()) {
            password.error = getString(R.string.error_message_password)
        }
    }

    private fun checkEmailIsEqualsRegex() = emailPattern.matcher(email.text.toString()).matches()

    private fun checkPasswordMatching() = password.text.toString() == repeatPassword.text.toString()

    private fun checkPasswordIsEqualsRegex() =
        passwordPattern.matcher(password.text.toString()).matches()

    private fun createAlertDialog(error: String?): AlertDialog.Builder? {
        return dialog?.setTitle(getString(R.string.error))
            ?.setMessage(error)
            ?.setIcon(R.drawable.ic_baseline_error_outline_24)
            ?.setPositiveButton(getString(R.string.Ok)) { dialog, _ ->
                dialog.dismiss()
                dialog.cancel()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CURRENT_STATE_OF_BTNS, isLoadingBtnActive)
        outState.putString(CURRENT_EMAIL, email.text.toString())
        outState.putString(CURRENT_PASSWORD, password.text.toString())
        outState.putString(CURRENT_REPEAT_PASSWORD, repeatPassword.text.toString())
        outState.putString(CURRENT_FIRST_NAME, firstName.text.toString())
        outState.putString(CURRENT_LAST_NAME, lastName.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isLoadingBtnActive = savedInstanceState.getBoolean(CURRENT_STATE_OF_BTNS)
        email.setText(savedInstanceState.getString(CURRENT_EMAIL))
        password.setText(savedInstanceState.getString(CURRENT_PASSWORD))
        repeatPassword.setText(savedInstanceState.getString(CURRENT_REPEAT_PASSWORD))
        firstName.setText(savedInstanceState.getString(CURRENT_FIRST_NAME))
        lastName.setText(savedInstanceState.getString(CURRENT_LAST_NAME))
    }

    private fun createLoginRequest() =
        LoginRequest(email = email.text.toString(), password = password.text.toString())

    private fun addTextListeners() {
        email.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)
        repeatPassword.addTextChangedListener(textWatcher)
        firstName.addTextChangedListener(textWatcher)
        lastName.addTextChangedListener(textWatcher)
    }

    private fun createRegistrationRequest() = RegistrationRequest(
        email.text.toString(),
        password.text.toString(),
        firstName.text.toString(),
        lastName.text.toString()
    )

    private fun saveTokenInSharedPref(token: String?) {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit()
            .putString(CURRENT_TOKEN, token)
            .apply()
    }

    private fun createTextListener(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isLoadingBtnActive) {
                    registerBtn.isEnabled = email.text.isNotEmpty()
                            && password.text.isNotEmpty()
                            && repeatPassword.text.isNotEmpty()
                            && firstName.text.isNotEmpty()
                            && lastName.text.isNotEmpty()
                }
                if (isLoadingBtnActive) {
                    openLoginWindowBtn.isEnabled = email.text.isNotEmpty()
                            && password.text.isNotEmpty()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    private fun setVisibilityViews(isVisible: Boolean) {
        repeatPassword.isVisible = isVisible
        firstName.isVisible = isVisible
        lastName.isVisible = isVisible
    }

    private fun createIntent() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        openLoginWindowBtn.setOnClickListener(null)
        registerBtn.setOnClickListener(null)
        dialog = null
        email.removeTextChangedListener(textWatcher)
        password.removeTextChangedListener(textWatcher)
        repeatPassword.removeTextChangedListener(textWatcher)
        firstName.removeTextChangedListener(textWatcher)
        lastName.removeTextChangedListener(textWatcher)
        textWatcher = null
    }
}