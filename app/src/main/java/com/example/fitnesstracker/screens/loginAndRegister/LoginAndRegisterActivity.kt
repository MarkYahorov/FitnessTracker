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
import com.example.fitnesstracker.data.database.FitnessDatabase
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
        private const val UNDERLINE_LOGIN = "UNDERLINE_LOGIN"
        private const val UNDERLINE_REGISTR = "UNDERLINE_REGISTR"
        private const val ENABLED_LOGIN_BTN = "ENABLED_LOGIN_BTN"
        private const val ENABLED_REGISTR_BTN = "ENABLED_REGISTR_BTN"
        private const val REPEAT_PASSWORD_VISIBLE = "REPEAT_PASSWORD_VISIBLE"
        private const val FIRST_NAME_VISIBLE = "FIRST_NAME_VISIBLE"
        private const val LAST_NAME_VISIBLE = "LAST_NAME_VISIBLE"
        private const val CHECK_EMAIL_VALIDATE =
            "^[_A-Za-z0-9+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        private const val CHECK_PASSWORD_VALIDATE = "^[a-zA-Z0-9]+$"
    }

    private var welcomeText: TextView? = null
    private var emailText: EditText? = null
    private var passwordText: EditText? = null
    private var repeatPasswordText: EditText? = null
    private var firstNameText: EditText? = null
    private var lastNameText: EditText? = null
    private var loginBtn: Button? = null
    private var registerBtn: Button? = null

    private var isLoadingBtnActive = true
    private var textWatcher: TextWatcher? = null
    private val repositoryImpl = App.INSTANCE.repositoryFromServerImpl
    private var alertDialog: AlertDialog.Builder? = null
    private val emailPattern = Pattern.compile(CHECK_EMAIL_VALIDATE)
    private val passwordPattern = Pattern.compile(CHECK_PASSWORD_VALIDATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_and_register)

        if (getTokenFromSharedPref() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        initAllViews()
        registerBtn?.paint?.isUnderlineText = true
    }

    private fun getTokenFromSharedPref(): String? {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, null)
    }

    private fun initAllViews() {
        welcomeText = findViewById(R.id.first_screen_text)
        emailText = findViewById(R.id.email_edit_text_to_login)
        passwordText = findViewById(R.id.password_edit_text_to_login)
        repeatPasswordText = findViewById(R.id.repeat_edit_text)
        firstNameText = findViewById(R.id.first_name_edit_text)
        lastNameText = findViewById(R.id.last_name_edit_text)
        loginBtn = findViewById(R.id.open_login_views_btn)
        registerBtn = findViewById(R.id.registr_btn)
        alertDialog = AlertDialog.Builder(this)
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
        registerBtn?.setOnClickListener {
            if (isLoadingBtnActive) {
                loginBtn?.paint?.isUnderlineText = true
                registerBtn?.paint?.isUnderlineText = false
                setVisibilityViews(isVisible = true)
                welcomeText?.text = getString(R.string.welcome_registr_text)
                loginBtn?.isEnabled = true
                registerBtn?.isEnabled = false
            } else {
                if (checkEmailIsEqualsRegex() && checkPasswordIsEqualsRegex() && checkPasswordMatching()) {
                    registration()
                } else {
                    createErrorMessageForRegistration()
                }
            }
            isLoadingBtnActive = false
        }
    }

    private fun setLoginBtnListener() {
        loginBtn?.setOnClickListener {
            if (isLoadingBtnActive) {
                if (checkEmailIsEqualsRegex() && checkPasswordIsEqualsRegex()) {
                    login()
                } else {
                    createErrorMessageForLogin()
                }
            } else {
                registerBtn?.paint?.isUnderlineText = true
                loginBtn?.paint?.isUnderlineText = false
                setVisibilityViews(isVisible = false)
                welcomeText?.text = getString(R.string.welcome_login_text)
                loginBtn?.isEnabled = false
                registerBtn?.isEnabled = true
            }
            isLoadingBtnActive = true
        }
    }

    private fun registration() {
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
                        FitnessDatabase(applicationContext).onCreate(App.INSTANCE.myDataBase)
                    }
                }
            }, Task.UI_THREAD_EXECUTOR)
    }

    private fun login() {
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
                        FitnessDatabase(applicationContext).onCreate(App.INSTANCE.myDataBase)
                    }
                }
            }, Task.UI_THREAD_EXECUTOR)

    }

    private fun createErrorMessageForRegistration() {
        if (!checkEmailIsEqualsRegex() && !checkPasswordIsEqualsRegex() && !checkPasswordMatching()) {
            emailText?.error = getString(R.string.error_message_email)
            passwordText?.error = getString(R.string.error_message_password)
            repeatPasswordText?.error = getString(R.string.error_message_repeat_password)
        } else if (!checkEmailIsEqualsRegex()) {
            emailText?.error = getString(R.string.error_message_email)
        } else if (!checkPasswordIsEqualsRegex()) {
            passwordText?.error = getString(R.string.error_message_password)
        } else if (!checkPasswordMatching()) {
            repeatPasswordText?.error = getString(R.string.error_message_repeat_password)
        }
    }

    private fun createErrorMessageForLogin() {
        if (!checkEmailIsEqualsRegex() && !checkPasswordIsEqualsRegex()) {
            emailText?.error = getString(R.string.error_message_email)
            passwordText?.error = getString(R.string.error_message_password)
        } else if (!checkEmailIsEqualsRegex()) {
            emailText?.error = getString(R.string.error_message_email)
        } else if (!checkPasswordIsEqualsRegex()) {
            passwordText?.error = getString(R.string.error_message_password)
        }
    }

    private fun checkEmailIsEqualsRegex() = emailPattern.matcher(emailText?.text.toString()).matches()

    private fun checkPasswordMatching() = passwordText?.text.toString() == repeatPasswordText?.text.toString()

    private fun checkPasswordIsEqualsRegex() =
        passwordPattern.matcher(passwordText?.text.toString()).matches()

    private fun createAlertDialog(error: String?): AlertDialog.Builder? {
        return alertDialog?.setTitle(getString(R.string.error))
            ?.setMessage(error)
            ?.setIcon(R.drawable.ic_baseline_error_outline_24)
            ?.setPositiveButton(getString(R.string.Ok)) { dialog, _ ->
                dialog.dismiss()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(CURRENT_STATE_OF_BTNS, isLoadingBtnActive)
        outState.putString(CURRENT_EMAIL, emailText?.text.toString())
        outState.putString(CURRENT_PASSWORD, passwordText?.text.toString())
        outState.putString(CURRENT_REPEAT_PASSWORD, repeatPasswordText?.text.toString())
        outState.putString(CURRENT_FIRST_NAME, firstNameText?.text.toString())
        outState.putString(CURRENT_LAST_NAME, lastNameText?.text.toString())
        loginBtn?.paint?.let { outState.putBoolean(UNDERLINE_LOGIN, it.isUnderlineText) }
        registerBtn?.paint?.let { outState.putBoolean(UNDERLINE_REGISTR, it.isUnderlineText) }
        loginBtn?.isEnabled?.let { outState.putBoolean(ENABLED_LOGIN_BTN, it) }
        registerBtn?.let { outState.putBoolean(ENABLED_REGISTR_BTN, it.isEnabled) }
        repeatPasswordText?.let { outState.putBoolean(REPEAT_PASSWORD_VISIBLE, it.isVisible) }
        firstNameText?.let { outState.putBoolean(FIRST_NAME_VISIBLE, it.isVisible) }
        lastNameText?.let { outState.putBoolean(LAST_NAME_VISIBLE, it.isVisible) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        isLoadingBtnActive = savedInstanceState.getBoolean(CURRENT_STATE_OF_BTNS)
        emailText?.setText(savedInstanceState.getString(CURRENT_EMAIL))
        passwordText?.setText(savedInstanceState.getString(CURRENT_PASSWORD))
        repeatPasswordText?.setText(savedInstanceState.getString(CURRENT_REPEAT_PASSWORD))
        firstNameText?.setText(savedInstanceState.getString(CURRENT_FIRST_NAME))
        lastNameText?.setText(savedInstanceState.getString(CURRENT_LAST_NAME))
        loginBtn?.paint?.isUnderlineText = savedInstanceState.getBoolean(UNDERLINE_LOGIN)
        registerBtn?.paint?.isUnderlineText = savedInstanceState.getBoolean(UNDERLINE_REGISTR)
        loginBtn?.isEnabled = savedInstanceState.getBoolean(ENABLED_LOGIN_BTN)
        registerBtn?.isEnabled = savedInstanceState.getBoolean(ENABLED_REGISTR_BTN)
        repeatPasswordText?.isVisible = savedInstanceState.getBoolean(REPEAT_PASSWORD_VISIBLE)
        firstNameText?.isVisible = savedInstanceState.getBoolean(FIRST_NAME_VISIBLE)
        lastNameText?.isVisible = savedInstanceState.getBoolean(LAST_NAME_VISIBLE)
    }

    private fun createLoginRequest() =
        LoginRequest(email = emailText?.text.toString(), password = passwordText?.text.toString())

    private fun addTextListeners() {
        emailText?.addTextChangedListener(textWatcher)
        passwordText?.addTextChangedListener(textWatcher)
        repeatPasswordText?.addTextChangedListener(textWatcher)
        firstNameText?.addTextChangedListener(textWatcher)
        lastNameText?.addTextChangedListener(textWatcher)
    }

    private fun createRegistrationRequest() = RegistrationRequest(
        emailText?.text.toString(),
        passwordText?.text.toString(),
        firstNameText?.text.toString(),
        lastNameText?.text.toString()
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
                    registerBtn?.isEnabled = emailText?.text?.isNotEmpty() == true
                            && passwordText?.text?.isNotEmpty() == true
                            && repeatPasswordText?.text?.isNotEmpty()== true
                            && firstNameText?.text?.isNotEmpty()== true
                            && lastNameText?.text?.isNotEmpty()== true
                }
                if (isLoadingBtnActive) {
                    loginBtn?.isEnabled = emailText?.text?.isNotEmpty()== true
                            && passwordText?.text?.isNotEmpty()== true
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    private fun setVisibilityViews(isVisible: Boolean) {
        repeatPasswordText?.isVisible = isVisible
        firstNameText?.isVisible = isVisible
        lastNameText?.isVisible = isVisible
    }

    private fun createIntent() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()

        loginBtn?.setOnClickListener(null)
        registerBtn?.setOnClickListener(null)
        emailText?.removeTextChangedListener(textWatcher)
        passwordText?.removeTextChangedListener(textWatcher)
        repeatPasswordText?.removeTextChangedListener(textWatcher)
        firstNameText?.removeTextChangedListener(textWatcher)
        lastNameText?.removeTextChangedListener(textWatcher)
    }

    override fun onDestroy() {
        super.onDestroy()

        welcomeText = null
        emailText = null
        passwordText = null
        repeatPasswordText = null
        firstNameText = null
        lastNameText = null
        loginBtn = null
        registerBtn = null
        alertDialog = null
        textWatcher = null
    }
}