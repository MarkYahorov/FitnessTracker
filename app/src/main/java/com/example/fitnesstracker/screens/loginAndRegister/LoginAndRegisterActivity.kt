package com.example.fitnesstracker.screens.loginAndRegister

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.fitnesstracker.R
import com.example.fitnesstracker.models.login.LoginResponse
import com.example.fitnesstracker.models.registration.RegistrationResponse
import com.example.fitnesstracker.screens.loginAndRegister.fragments.LoginFragment
import com.example.fitnesstracker.screens.loginAndRegister.fragments.RegisterAndLoginFragment
import com.example.fitnesstracker.screens.loginAndRegister.fragments.RegisterFragment
import com.example.fitnesstracker.screens.main.MainActivity

class LoginAndRegisterActivity : AppCompatActivity(), RegisterAndLoginFragment.ChangeScreen,
    LoginFragment.ChangeScreen, RegisterFragment.ChangeScreen {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_and_register)

        addRegisterAndLoginFragment()
    }

    private fun addRegisterAndLoginFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragments_container, RegisterAndLoginFragment())
            .addToBackStack("RegisterAndLogin")
            .commit()

    }

    override fun goToLoginScreen() {
        supportFragmentManager.popBackStackImmediate("Register", POP_BACK_STACK_INCLUSIVE)
        replaceFragment(LoginFragment(), "Login")
    }

    override fun goToMainScreen(
        registrationResponse: RegistrationResponse, firstName: String,
        lastName: String,
    ) {
        createIntent()
    }

    override fun goToRegisterScreen() {
        supportFragmentManager.popBackStackImmediate("Login", POP_BACK_STACK_INCLUSIVE)
        replaceFragment(RegisterFragment(), "Register")
    }

    override fun goToMainScreen(loginResponse: LoginResponse) {
        createIntent()
    }

    private fun createIntent() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


//    override fun goToMainScreen() {
//        startActivity(Intent(this, MainActivity::class.java))
//        supportFragmentManager.popBackStackImmediate("Login", POP_BACK_STACK_INCLUSIVE)
//        supportFragmentManager.popBackStackImmediate("Register", POP_BACK_STACK_INCLUSIVE)
//        supportFragmentManager.popBackStackImmediate("RegisterAndLogin", POP_BACK_STACK_INCLUSIVE)
//        finish()
//    }

    private fun replaceFragment(fragment: Fragment, backStackName: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragments_container, fragment)
            .addToBackStack(backStackName)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack("", POP_BACK_STACK_INCLUSIVE)
        }
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        }
        super.onBackPressed()
    }
}