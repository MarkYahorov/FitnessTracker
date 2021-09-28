package com.example.loginandregistr.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.example.base.screen.base.activity.BaseActivity
import com.example.core.Constants.CURRENT_TOKEN
import com.example.core.Constants.FITNESS_SHARED
import com.example.core.provideBaseComponent
import com.example.loginandregistr.R
import com.example.loginandregistr.di.DaggerLoginAndRegistrComponent
import com.example.loginandregistr.presenter.LoginAndRegistrContract

internal class LoginAndRegisterActivity :
    BaseActivity<LoginAndRegistrContract.LoginAndRegistrPresenter, LoginAndRegistrContract.LoginAndRegistrView>(),
    LoginAndRegistrContract.LoginAndRegistrView {

    companion object {
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
    }

    private var welcomeText: TextView? = null
    private var emailText: EditText? = null
    private var passwordText: EditText? = null
    private var repeatPasswordText: EditText? = null
    private var firstNameText: EditText? = null
    private var lastNameText: EditText? = null
    private var loginBtn: Button? = null
    private var registerBtn: Button? = null

    private var emailTextWatcher: TextWatcher? = null
    private var passwordTextWatcher: TextWatcher? = null
    private var repeatPasswordTextWatcher: TextWatcher? = null
    private var nameTextWatcher: TextWatcher? = null
    private var lastNameTextWatcher: TextWatcher? = null

    private var isLoadingBtnActive = true
    private var alertDialog: AlertDialog.Builder? = null
    private val loginAndRegistrComponent by lazy {
        DaggerLoginAndRegistrComponent.factory()
            .create(provideBaseComponent(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_and_register)

        loginAndRegistrComponent.inject(this)
        initAllViews()
        registerBtn?.paint?.isUnderlineText = true
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
        registerBtn?.setOnClickListener { getPresenter().registration() }
    }

    private fun setLoginBtnListener() {
        loginBtn?.setOnClickListener { getPresenter().login() }
    }

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

    private fun addTextListeners() {
        emailTextWatcher = emailText?.addTextChangedListener { email ->
            getPresenter().setLoginText(email.toString())
        }
        passwordTextWatcher = passwordText?.addTextChangedListener { password ->
            getPresenter().setPasswordText(password.toString())
        }
        repeatPasswordTextWatcher = repeatPasswordText?.addTextChangedListener { repeatPassword ->
            getPresenter().setRepeatText(repeatPassword.toString())
        }
        nameTextWatcher = firstNameText?.addTextChangedListener { name ->
            getPresenter().setName(name.toString())
        }
        lastNameTextWatcher = lastNameText?.addTextChangedListener { lastName ->
            getPresenter().setLastName(lastName.toString())
        }
    }

    private fun saveTokenInSharedPref(token: String?) {
        getSharedPreferences(FITNESS_SHARED, Context.MODE_PRIVATE)
            .edit {
                this.putString(CURRENT_TOKEN, token)
                .apply()
            }
    }

    private fun setVisibility(isVisible: Boolean) {
        repeatPasswordText?.isVisible = isVisible
        firstNameText?.isVisible = isVisible
        lastNameText?.isVisible = isVisible
    }

    override fun showAllRegistrationError() {
        emailText?.error = getString(R.string.error_message_email)
        passwordText?.error = getString(R.string.error_message_password)
        repeatPasswordText?.error = getString(R.string.error_message_repeat_password)
    }

    override fun showAllLoginError() {
        emailText?.error = getString(R.string.error_message_email)
        passwordText?.error = getString(R.string.error_message_password)
    }

    override fun showEmailError() {
        emailText?.error = getString(R.string.error_message_email)
    }

    override fun showPasswordError() {
        passwordText?.error = getString(R.string.error_message_password)
    }

    override fun showPasswordMatchingError() {
        repeatPasswordText?.error = getString(R.string.error_message_repeat_password)
    }

    private fun createIntent() {
        val intent = Intent(this, Class.forName("com.example.main.screens.MainActivity"))
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()

        loginBtn?.setOnClickListener(null)
        registerBtn?.setOnClickListener(null)
        emailText?.removeTextChangedListener(emailTextWatcher)
        passwordText?.removeTextChangedListener(passwordTextWatcher)
        repeatPasswordText?.removeTextChangedListener(repeatPasswordTextWatcher)
        firstNameText?.removeTextChangedListener(nameTextWatcher)
        lastNameText?.removeTextChangedListener(lastNameTextWatcher)
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
        emailTextWatcher = null
        passwordTextWatcher = null
        repeatPasswordTextWatcher = null
        nameTextWatcher = null
        lastNameTextWatcher = null
    }

    override fun goToNextScreen(token: String?) {
        saveTokenInSharedPref(token = token)
        createIntent()
        //FitnessDatabase(applicationContext).onCreate(App.INSTANCE.myDataBase)
    }

    override fun showError(error: String?) {
        createAlertDialog(error)?.show()
    }

    override fun setVisibilityViews(isLogin: Boolean) {
        loginBtn?.paint?.isUnderlineText = isLogin
        registerBtn?.paint?.isUnderlineText = !isLogin
        registerBtn?.invalidate()
        loginBtn?.invalidate()
        setVisibility(isVisible = isLogin)
    }

    override fun showLoading() {
        TODO("Not yet implemented")
    }

    override fun hideLoading() {
        TODO("Not yet implemented")
    }

    override fun createPresenter(): LoginAndRegistrContract.LoginAndRegistrPresenter {
        return loginAndRegistrComponent.presenter()
    }
}