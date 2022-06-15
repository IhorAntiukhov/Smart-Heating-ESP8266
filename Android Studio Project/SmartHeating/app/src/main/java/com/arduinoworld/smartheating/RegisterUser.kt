package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*


@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits", "InflateParams")
class RegisterUser : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var isUserLogged = true
    private var updateUser = false
    private var userEmailUpdated = false
    private var userPasswordUpdated = false
    private var userEmail = ""
    private var userPassword = ""
    private var userCredentialNumber = 0

    lateinit var buttonCreateUser : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val inputUserEmail : EditText = requireActivity().findViewById(R.id.inputNewUserEmail)
        val inputUserPassword : EditText = requireActivity().findViewById(R.id.inputNewUserPassword)
        val inputConfirmPassword : EditText = requireActivity().findViewById(R.id.inputConfirmPassword)
        val inputLayoutUserEmail : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutNewUserEmail)
        val inputLayoutUserPassword : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutNewUserPassword)
        val inputLayoutConfirmPassword : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutConfirmPassword)
        val buttonBack : Button = requireActivity().findViewById(R.id.buttonBack)
        val buttonForward : Button = requireActivity().findViewById(R.id.buttonForward)
        val buttonUpdateUser : Button = requireActivity().findViewById(R.id.buttonUpdateNewUser)
        buttonCreateUser = requireActivity().findViewById(R.id.buttonCreateUser)

        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        updateUser = sharedPreferences.getBoolean("UpdateUser", false)
        editPreferences.putBoolean("UpdateUser", false).apply()

        if (updateUser) {
            userEmail = sharedPreferences.getString("UserEmail", "").toString()
            userPassword = sharedPreferences.getString("UserPassword", "").toString()
            inputUserEmail.setText(userEmail)
            inputUserPassword.setText(userPassword)
            inputConfirmPassword.setText(userPassword)
        }

        buttonForward.setOnClickListener {
            vibrate()
            if (userCredentialNumber == 1) {
                if (inputUserPassword.text.isNotEmpty()) {
                    if (inputUserPassword.text.length >= 6) {
                        if (containsNumber(inputUserPassword.text.toString())) {
                            hideKeyboard()
                            buttonForward.visibility = View.GONE
                            if (!updateUser) {
                                buttonCreateUser.visibility = View.VISIBLE
                            } else {
                                buttonUpdateUser.visibility = View.VISIBLE
                            }
                            inputLayoutConfirmPassword.visibility = View.VISIBLE
                            inputLayoutUserPassword.visibility = View.GONE
                            userCredentialNumber += 1
                        } else {
                            Toast.makeText(activity, "Пароль пользователя \n должен содержать хотя \n бы одну цифру!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(activity, "Пароль пользователя \n должен содержать не \n меньше 6 символов!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, "Введите пароль пользователя!", Toast.LENGTH_LONG).show()
                }
            }
            if (userCredentialNumber == 0) {
                if (inputUserEmail.text.isNotEmpty()) {
                    if (isValidEmail(inputUserEmail.text.toString())) {
                        hideKeyboard()
                        buttonBack.visibility = View.VISIBLE
                        inputLayoutUserPassword.visibility = View.VISIBLE
                        inputLayoutUserEmail.visibility = View.GONE
                        userCredentialNumber += 1
                    } else {
                        Toast.makeText(activity, "Вы ввели неправильную \n почту пользователя!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                }
            }
        }

        buttonBack.setOnClickListener {
            vibrate()
            hideKeyboard()
            if (userCredentialNumber == 1) {
                buttonBack.visibility = View.GONE
                inputLayoutUserEmail.visibility = View.VISIBLE
                inputLayoutUserPassword.visibility = View.GONE
                userCredentialNumber -= 1
            }
            if (userCredentialNumber == 2) {
                buttonForward.visibility = View.VISIBLE
                buttonCreateUser.visibility = View.GONE
                buttonUpdateUser.visibility = View.GONE
                inputLayoutConfirmPassword.visibility = View.GONE
                inputLayoutUserPassword.visibility = View.VISIBLE
                userCredentialNumber -= 1
            }
        }

        buttonCreateUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (inputConfirmPassword.text.isNotEmpty()) {
                    if (inputConfirmPassword.text.toString() == inputUserPassword.text.toString()) {
                        hideKeyboard()
                        val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                        alertDialogBuilder.setView(layoutInflater.inflate(R.layout.create_user_progress_bar, null))
                        val alertDialog = alertDialogBuilder.create()
                        alertDialog.setCanceledOnTouchOutside(false)
                        alertDialog.show()
                        firebaseAuth.createUserWithEmailAndPassword(inputUserEmail.text.toString(), inputUserPassword.text.toString())
                                .addOnCompleteListener(requireActivity()) { createUserTask ->
                                    if (createUserTask.isSuccessful) {
                                        Toast.makeText(activity, "Пользователь успешно зарегистрирован!", Toast.LENGTH_SHORT).show()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            if (isUserLogged) {
                                                (activity as AppCompatActivity).supportActionBar?.title = "Ваш Профиль"
                                            } else {
                                                (activity as AppCompatActivity).supportActionBar?.title = "Вход"
                                            }
                                        }
                                        requireActivity().navigationView.setCheckedItem(R.id.buttonProfile)
                                        val fragmentManager = requireActivity().supportFragmentManager.beginTransaction()
                                        fragmentManager.replace(R.id.frameLayout, UserProfile()).commit()
                                    } else {
                                        Toast.makeText(activity, "Пользователь ${inputUserEmail.text} уже существует!", Toast.LENGTH_LONG).show()
                                    }
                                    alertDialog.cancel()
                                }
                    } else {
                        Toast.makeText(activity, "Введённый пароль не совпадает!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, "Подтвердите пароль пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonUpdateUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (inputConfirmPassword.text.isNotEmpty()) {
                    if (inputConfirmPassword.text.toString() == inputUserPassword.text.toString()) {
                        if (userEmail == inputUserEmail.text.toString() && userPassword == inputUserPassword.text.toString()) {
                            Toast.makeText(activity, "Введите новую почту, или пароль пользователя!", Toast.LENGTH_LONG).show()
                        } else {
                            firebaseAuth.currentUser!!.reauthenticate(EmailAuthProvider.getCredential(userEmail, userPassword)).addOnCompleteListener { reauthTask ->
                                if (reauthTask.isSuccessful) {
                                    if (userEmail != inputUserEmail.text.toString()) {
                                        firebaseAuth.currentUser!!.updateEmail(inputUserEmail.text.toString())
                                        userEmailUpdated = true
                                        editPreferences.putString("UserEmail", inputUserEmail.text.toString()).apply()
                                    }
                                    if (userPassword != inputUserPassword.text.toString()) {
                                        firebaseAuth.currentUser!!.updatePassword(inputUserPassword.text.toString())
                                        userPasswordUpdated = true
                                        editPreferences.putString("UserPassword", inputUserPassword.text.toString()).apply()
                                    }

                                    if (userEmailUpdated && !userPasswordUpdated) {
                                        Toast.makeText(activity, "Почта пользователя обновлена!", Toast.LENGTH_SHORT).show()
                                    } else if (!userEmailUpdated && userPasswordUpdated) {
                                        Toast.makeText(activity, "Пароль пользователя обновлён!", Toast.LENGTH_SHORT).show()
                                    } else if (userEmailUpdated && userPasswordUpdated) {
                                        Toast.makeText(activity, "Почта и пароль пользователя обновлены!", Toast.LENGTH_SHORT).show()
                                    }
                                    hideKeyboard()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        (activity as AppCompatActivity).supportActionBar?.title = "Ваш Профиль"
                                    }
                                    val fragmentManager = requireActivity().supportFragmentManager.beginTransaction()
                                    fragmentManager.replace(R.id.frameLayout, UserProfile()).commit()
                                } else {
                                    Toast.makeText(activity, "Не удалось переавторизироваться!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(activity, "Введённый пароль не совпадает!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, "Подтвердите пароль пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register_user, container, false)
    }

    private fun isValidEmail(inputText: CharSequence) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(inputText).matches()
    }

    private fun containsNumber(inputText: String) : Boolean {
        return inputText.contains("1") || inputText.contains("2") || inputText.contains("3") || inputText.contains("4") ||
            inputText.contains("5") || inputText.contains("6") || inputText.contains("7") || inputText.contains("8") ||
            inputText.contains("9") || inputText.contains("0")
    }

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }

    private fun vibrate() {
        if (isHapticFeedbackEnabled) {
            buttonCreateUser.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        } else {
            val vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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