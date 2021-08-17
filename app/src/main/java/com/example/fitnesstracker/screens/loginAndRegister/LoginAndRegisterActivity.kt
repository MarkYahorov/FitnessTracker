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

    private lateinit var welcomeText: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var repeatPassword: EditText
    private lateinit var firstNameText: EditText
    private lateinit var lastNameText: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

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
        registerBtn.paint.isUnderlineText = true
    }

    private fun getTokenFromSharedPref(): String? {
        return getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .getString(CURRENT_TOKEN, null)
    }

    private fun initAllViews() {
        welcomeText = findViewById(R.id.first_screen_text)
        email = findViewById(R.id.email_edit_text_to_login)
        password = findViewById(R.id.password_edit_text_to_login)
        repeatPassword = findViewById(R.id.repeat_edit_text)
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
        registerBtn.setOnClickListener {
            if (isLoadingBtnActive) {
                loginBtn.paint.isUnderlineText = true
                registerBtn.paint.isUnderlineText = false
                setVisibilityViews(isVisible = true)
                welcomeText.text = getString(R.string.welcome_registr_text)
                loginBtn.isEnabled = true
                registerBtn.isEnabled = false
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
        loginBtn.setOnClickListener {
            if (isLoadingBtnActive) {
                if (checkEmailIsEqualsRegex() && checkPasswordIsEqualsRegex()) {
                    login()
                } else {
                    createErrorMessageForLogin()
                }
            } else {
                registerBtn.paint.isUnderlineText = true
                loginBtn.paint.isUnderlineText = false
                setVisibilityViews(isVisible = false)
                welcomeText.text = getString(R.string.welcome_login_text)
                loginBtn.isEnabled = false
                registerBtn.isEnabled = true
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
        outState.putString(CURRENT_EMAIL, email.text.toString())
        outState.putString(CURRENT_PASSWORD, password.text.toString())
        outState.putString(CURRENT_REPEAT_PASSWORD, repeatPassword.text.toString())
        outState.putString(CURRENT_FIRST_NAME, firstNameText.text.toString())
        outState.putString(CURRENT_LAST_NAME, lastNameText.text.toString())
        outState.putBoolean(UNDERLINE_LOGIN, loginBtn.paint.isUnderlineText)
        outState.putBoolean(UNDERLINE_REGISTR, registerBtn.paint.isUnderlineText)
        outState.putBoolean(ENABLED_LOGIN_BTN, loginBtn.isEnabled)
        outState.putBoolean(ENABLED_REGISTR_BTN, registerBtn.isEnabled)
        outState.putBoolean(REPEAT_PASSWORD_VISIBLE, repeatPassword.isVisible)
        outState.putBoolean(FIRST_NAME_VISIBLE, firstNameText.isVisible)
        outState.putBoolean(LAST_NAME_VISIBLE, lastNameText.isVisible)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isLoadingBtnActive = savedInstanceState.getBoolean(CURRENT_STATE_OF_BTNS)
        email.setText(savedInstanceState.getString(CURRENT_EMAIL))
        password.setText(savedInstanceState.getString(CURRENT_PASSWORD))
        repeatPassword.setText(savedInstanceState.getString(CURRENT_REPEAT_PASSWORD))
        firstNameText.setText(savedInstanceState.getString(CURRENT_FIRST_NAME))
        lastNameText.setText(savedInstanceState.getString(CURRENT_LAST_NAME))
        loginBtn.paint.isUnderlineText = savedInstanceState.getBoolean(UNDERLINE_LOGIN)
        registerBtn.paint.isUnderlineText = savedInstanceState.getBoolean(UNDERLINE_REGISTR)
        loginBtn.isEnabled = savedInstanceState.getBoolean(ENABLED_LOGIN_BTN)
        registerBtn.isEnabled = savedInstanceState.getBoolean(ENABLED_REGISTR_BTN)
        repeatPassword.isVisible = savedInstanceState.getBoolean(REPEAT_PASSWORD_VISIBLE)
        firstNameText.isVisible = savedInstanceState.getBoolean(FIRST_NAME_VISIBLE)
        lastNameText.isVisible = savedInstanceState.getBoolean(LAST_NAME_VISIBLE)
    }

    private fun createLoginRequest() =
        LoginRequest(email = email.text.toString(), password = password.text.toString())

    private fun addTextListeners() {
        email.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)
        repeatPassword.addTextChangedListener(textWatcher)
        firstNameText.addTextChangedListener(textWatcher)
        lastNameText.addTextChangedListener(textWatcher)
    }

    private fun createRegistrationRequest() = RegistrationRequest(
        email.text.toString(),
        password.text.toString(),
        firstNameText.text.toString(),
        lastNameText.text.toString()
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
                            && firstNameText.text.isNotEmpty()
                            && lastNameText.text.isNotEmpty()
                }
                if (isLoadingBtnActive) {
                    loginBtn.isEnabled = email.text.isNotEmpty()
                            && password.text.isNotEmpty()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    private fun setVisibilityViews(isVisible: Boolean) {
        repeatPassword.isVisible = isVisible
        firstNameText.isVisible = isVisible
        lastNameText.isVisible = isVisible
    }

    private fun createIntent() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        loginBtn.setOnClickListener(null)
        registerBtn.setOnClickListener(null)
        alertDialog = null
        email.removeTextChangedListener(textWatcher)
        password.removeTextChangedListener(textWatcher)
        repeatPassword.removeTextChangedListener(textWatcher)
        firstNameText.removeTextChangedListener(textWatcher)
        lastNameText.removeTextChangedListener(textWatcher)
        textWatcher = null
    }
}