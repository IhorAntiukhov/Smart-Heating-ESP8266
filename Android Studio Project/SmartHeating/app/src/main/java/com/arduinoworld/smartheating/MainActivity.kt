package com.arduinoworld.smartheating

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_content.*

@Suppress("DEPRECATION", "SetTextI18n")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var fragmentNumber = 0
    lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Ручное Управление"

        sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)

        val actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
        loadFragment(ManualControl())
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        hideKeyboard()
        when (item.itemId) {
            R.id.buttonManualControl -> {
                vibrate()
                if (fragmentNumber != 0) {
                    fragmentNumber = 0
                    supportActionBar!!.title = "Ручное Управление"
                    loadFragment(ManualControl())
                }
            }
            R.id.buttonTimer -> {
                vibrate()
                if (fragmentNumber != 1) {
                    fragmentNumber = 1
                    supportActionBar!!.title = "Управление по Таймеру"
                    loadFragment(TimerControl())
                }
            }
            R.id.buttonTimeControl -> {
                vibrate()
                if (fragmentNumber != 2) {
                    fragmentNumber = 2
                    supportActionBar!!.title = "Управление по Времени"
                    loadFragment(TimeControl())
                }
            }
            R.id.buttonTemperatureControl -> {
                vibrate()
                if (fragmentNumber != 3) {
                    fragmentNumber = 3
                    supportActionBar!!.title = "Управление по Температуре"
                    loadFragment(TemperatureControl())
                }
            }
            R.id.buttonProfile -> {
                vibrate()
                if (fragmentNumber != 4) {
                    fragmentNumber = 4
                    supportActionBar!!.title = "Ваш Профиль"
                    loadFragment(UserProfile())
                }
            }
            R.id.buttonRegister -> {
                vibrate()
                if (fragmentNumber != 5) {
                    fragmentNumber = 5
                    supportActionBar!!.title = "Регистрация"
                    loadFragment(RegisterUser())
                }
            }
            R.id.buttonNotifications -> {
                vibrate()
                if (fragmentNumber != 6) {
                    fragmentNumber = 6
                    supportActionBar!!.title = "Уведомления"
                    loadFragment(Notifications())
                }
            }
            R.id.buttonSettings -> {
                vibrate()
                if (fragmentNumber != 7) {
                    fragmentNumber = 7
                    supportActionBar!!.title = "Настройки"
                    loadFragment(Settings())
                }
            }
        }
        return true
    }

    private fun loadFragment(fragment : Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun vibrate() {
        if (sharedPreferences.getBoolean("HapticFeedbackEnabled", true)) {
            navigationView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(20)
                }
            }
        }
    }
}