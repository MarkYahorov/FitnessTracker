package com.example.loginandregistr.presenter

import com.example.base.presenter.base.BasePresenter
import com.example.core.models.login.LoginRequest
import com.example.core.models.registration.RegistrationRequest
import com.example.loginandregistr.repository.Repository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.regex.Pattern
import javax.inject.Inject

class LoginAndRegistrPresenter @Inject constructor(private val repository: Repository) :
    BasePresenter<LoginAndRegistrContract.LoginAndRegistrView>(),
    LoginAndRegistrContract.LoginAndRegistrPresenter {

    companion object {
        private const val ERROR = "error"
        private const val CHECK_EMAIL_VALIDATE =
            "^[_A-Za-z0-9+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        private const val CHECK_PASSWORD_VALIDATE = "^[a-zA-Z0-9]+$"
    }

    private val emailPattern = Pattern.compile(CHECK_EMAIL_VALIDATE)
    private val passwordPattern = Pattern.compile(CHECK_PASSWORD_VALIDATE)
    private var isLoadingBtnActive = true
    private var emailText = ""
    private var passwordText = ""
    private var repeatPassword = ""
    private var nameText = ""
    private var lastName = ""

    override fun setLoginText(email: String) {
        emailText = email
    }

    override fun setPasswordText(password: String) {
        passwordText = password
    }

    override fun setRepeatText(repeatPassword: String) {
        this.repeatPassword = repeatPassword
    }

    override fun setName(name: String) {
        nameText = name
    }

    override fun setLastName(lastName: String) {
        this.lastName = lastName
    }

    override fun login() {
        val emailValidate = checkEmailIsEqualsRegex(emailText)
        val passwordValidate = checkPasswordIsEqualsRegex(passwordText)
        if (isLoadingBtnActive) {
            if (emailValidate && passwordValidate) {
                login(emailText, passwordText)
            } else {
                createErrorMessageForLogin(emailValidate, passwordValidate)
            }
        } else {
            getView().setVisibilityViews(false)
        }
        isLoadingBtnActive = true
    }

    override fun registration(
    ) {
        if (isLoadingBtnActive) {
            getView().setVisibilityViews(isLogin = true)
        } else {
            val emailValidate = checkEmailIsEqualsRegex(emailText)
            val passwordValidate = checkPasswordIsEqualsRegex(passwordText)
            val validatePassword = checkPasswordMatching(passwordText, repeatPassword)

            if (emailValidate && passwordValidate && validatePassword) {
                registration(emailText, passwordText, nameText, lastName)
            } else {
                createErrorMessageForRegistration(emailValidate, passwordValidate, validatePassword)
            }
        }
        isLoadingBtnActive = false
    }

    private fun login(email: String, password: String){
        getCompositeDisposable().add(
            repository.login(loginRequest = loginRequest(email, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { loginResponse ->
                        if (loginResponse.status == ERROR) {
                            getView().showError(loginResponse.error)
                        } else {
                            getView().goToNextScreen(loginResponse.token)
                        }
                    }, { error ->
                        getView().showError(error.message)
                    })
        )
    }

    private fun registration(
        email: String,
        password: String,
        name: String,
        lastName: String
    ) {
        getCompositeDisposable().add(
            repository.registration(
                registrationRequest = registrationRequest(
                    email,
                    password,
                    name,
                    lastName
                )
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { registrationResponse ->
                        if (registrationResponse.status == ERROR) {
                            getView().showError(registrationResponse.error)
                        } else {
                            getView().goToNextScreen(registrationResponse.token)
                        }
                    }, { error ->
                        getView().showError(error.message)
                    })
        )
    }

    private fun createErrorMessageForRegistration(
        validateEmail: Boolean,
        validatePassword: Boolean,
        validatePasswords: Boolean
    ) {
        if (!validateEmail && !validatePassword && !validatePasswords) {
            getView().showAllRegistrationError()
        } else if (!validatePasswords) {
            getView().showPasswordMatchingError()
        } else {
            checkGeneral(validateEmail, validatePassword)
        }
    }

    private fun createErrorMessageForLogin(validateEmail: Boolean, validatePassword: Boolean) {
        if (!validateEmail && !validatePassword) {
            getView().showAllLoginError()
        } else {
            checkGeneral(validateEmail, validatePassword)
        }
    }

    private fun checkGeneral(validateEmail: Boolean, validatePassword: Boolean){
        if (!validateEmail) {
            getView().showEmailError()
        } else if (!validatePassword) {
            getView().showPasswordError()
        }
    }

    private fun loginRequest(email: String, password: String) = LoginRequest(email, password)

    private fun registrationRequest(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) = RegistrationRequest(email, password, firstName, lastName)

    private fun checkEmailIsEqualsRegex(emailText: String) =
        emailPattern.matcher(emailText).matches()

    private fun checkPasswordMatching(passwordText: String, repeatPasswordText: String) =
        passwordText == repeatPasswordText

    private fun checkPasswordIsEqualsRegex(passwordText: String) =
        passwordPattern.matcher(passwordText).matches()
}