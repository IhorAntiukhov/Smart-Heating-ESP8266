package com.arduinoworld.smartheating

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_user_profile.*


@SuppressLint("InflateParams", "SetTextI18n")
@Suppress("DEPRECATION", "SameParameterValue")
class UserProfile : Fragment() {
    private var isHapticFeedbackEnabled = true
    private var passwordToggle = false
    private var isUserLogged = false

    lateinit var buttonDeleteUser : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance().reference.child("users")

        val sharedPreferences = requireActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val inputUserEmail : EditText = requireActivity().findViewById(R.id.inputUserEmail)
        val inputUserPassword : EditText = requireActivity().findViewById(R.id.inputUserPassword)
        val inputLayoutUserEmail : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutUserEmail)
        val inputLayoutUserPassword : TextInputLayout = requireActivity().findViewById(R.id.inputLayoutUserPassword)
        val buttonSignIn : Button = requireActivity().findViewById(R.id.buttonSignIn)
        val buttonResetPassword : Button = requireActivity().findViewById(R.id.buttonResetPassword)
        val buttonUpdateUser : Button = requireActivity().findViewById(R.id.buttonUpdateUser)
        val buttonLogout : Button = requireActivity().findViewById(R.id.buttonLogout)
        val userAccountIcon : ImageView = requireActivity().findViewById(R.id.userAccountIcon)
        val buttonPasswordToggle : ImageButton = requireActivity().findViewById(R.id.buttonPasswordToggle)
        val labelEmailPassword : TextView = requireActivity().findViewById(R.id.labelEmailPassword)
        buttonDeleteUser = requireActivity().findViewById(R.id.buttonDeleteUser)

        isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        inputUserEmail.setText(sharedPreferences.getString("UserEmail", ""))
        inputUserPassword.setText(sharedPreferences.getString("UserPassword", ""))

        if (isUserLogged) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as AppCompatActivity).supportActionBar?.title = "Ваш Профиль"
            }
            buttonLogout.visibility = View.VISIBLE
            buttonUpdateUser.visibility = View.VISIBLE
            buttonDeleteUser.visibility = View.VISIBLE
            userAccountIcon.visibility = View.VISIBLE
            layoutEmailPassword.visibility = View.VISIBLE
            labelEmailPassword.text = "${inputUserEmail.text}\n${hidePassword(inputUserPassword.text.toString())}"
            buttonSignIn.visibility = View.GONE
            buttonResetPassword.visibility = View.GONE
            inputLayoutUserEmail.visibility = View.GONE
            inputLayoutUserPassword.visibility = View.GONE
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as AppCompatActivity).supportActionBar?.title = "Вход"
            }
        }

        buttonSignIn.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isNotEmpty()) {
                    hideKeyboard()
                    val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                    alertDialogBuilder.setView(layoutInflater.inflate(R.layout.sign_in_progress_bar, null))
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.show()
                    firebaseAuth.signInWithEmailAndPassword(inputUserEmail.text.toString(), inputUserPassword.text.toString())
                        .addOnCompleteListener(requireActivity()) { signInTask ->
                            if (signInTask.isSuccessful) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    (activity as AppCompatActivity).supportActionBar?.title = "Ваш Профиль"
                                }
                                buttonLogout.visibility = View.VISIBLE
                                buttonUpdateUser.visibility = View.VISIBLE
                                buttonDeleteUser.visibility = View.VISIBLE
                                userAccountIcon.visibility = View.VISIBLE
                                layoutEmailPassword.visibility = View.VISIBLE
                                labelEmailPassword.text = "${inputUserEmail.text}\n${hidePassword(inputUserPassword.text.toString())}"
                                buttonSignIn.visibility = View.GONE
                                buttonResetPassword.visibility = View.GONE
                                inputLayoutUserEmail.visibility = View.GONE
                                inputLayoutUserPassword.visibility = View.GONE
                                editPreferences.putBoolean("UserLogged", true)
                                editPreferences.putString("UserEmail", inputUserEmail.text.toString())
                                editPreferences.putString("UserPassword", inputUserPassword.text.toString())
                                editPreferences.apply()
                                if (sharedPreferences.getBoolean("TemperatureModeStarted", false)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        val builder = JobInfo.Builder(9428, ComponentName(requireActivity(), TemperatureJob::class.java))
                                        builder.setMinimumLatency((5 * 1000).toLong())
                                        builder.setOverrideDeadline((15 * 1000).toLong())
                                        val jobScheduler = requireActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                        jobScheduler.schedule(builder.build())
                                    }
                                }
                                if (!sharedPreferences.getBoolean(inputUserEmail.text.toString(), false)) {
                                    val userReference = hashMapOf(
                                            "heatingStarted" to false,
                                            "boilerStarted" to false,
                                            "heatingElements" to 0,
                                            "heatingTimerTime" to 0,
                                            "boilerTimerTime" to 0,
                                            "heatingTime" to "0",
                                            "heatingArray" to 0,
                                            "boilerTime" to "0",
                                            "temperature" to 0,
                                            "temperatureRange" to "",
                                    )
                                    realtimeDatabase.child(firebaseAuth.currentUser!!.uid).setValue(userReference)
                                    editPreferences.putBoolean(inputUserEmail.text.toString(), true).apply()
                                }
                                Toast.makeText(activity, "Вы вошли в пользователя!", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    throw signInTask.exception!!
                                } catch (e: FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(activity, "Введён неверный пароль!", Toast.LENGTH_LONG).show()
                                } catch (e: FirebaseAuthInvalidUserException) {
                                    when (e.errorCode) {
                                        "ERROR_USER_NOT_FOUND" -> {
                                            Toast.makeText(activity, "Введённая почта не обнаружена!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                buttonSignIn.visibility = View.VISIBLE
                                buttonResetPassword.visibility = View.VISIBLE
                            }
                            alertDialog.cancel()
                    }
                } else {
                    if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isNotEmpty()) {
                        Toast.makeText(activity, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                    } else if (inputUserEmail.text.isNotEmpty() && inputUserPassword.text.isEmpty()) {
                        Toast.makeText(activity, "Введите пароль пользователя!", Toast.LENGTH_LONG).show()
                    } else if (inputUserEmail.text.isEmpty() && inputUserPassword.text.isEmpty()) {
                        Toast.makeText(activity, "Введите почту и пароль пользователя!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonResetPassword.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (inputUserEmail.text.isNotEmpty()) {
                    if (isValidEmail(inputUserEmail.text.toString())) {
                        val alertDialogResetPasswordBuilder = androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                        alertDialogResetPasswordBuilder.setTitle("Сброс Пароля")
                        alertDialogResetPasswordBuilder.setMessage("Отправка письма для сброса пароля на почту ${inputUserEmail.text}")
                        alertDialogResetPasswordBuilder.setPositiveButton("Продолжить") { _, _ ->
                            vibrate()
                            firebaseAuth.sendPasswordResetEmail(inputUserEmail.text.toString()).addOnCompleteListener(requireActivity()) { resetPasswordTask ->
                                if (resetPasswordTask.isSuccessful) {
                                    Toast.makeText(activity, "Письмо для сброса \n пароля отправлено!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(activity, "Не удалось отправить \n письмо для сброса пароля!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        alertDialogResetPasswordBuilder.setNegativeButton("Отмена") { _, _ ->
                            vibrate()
                        }
                        val alertDialogResetPassword = alertDialogResetPasswordBuilder.create()
                        alertDialogResetPassword.show()
                    } else {
                        Toast.makeText(activity, "Вы ввели неправильную \n почту пользователя!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, "Введите почту пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonPasswordToggle.setOnClickListener {
            vibrate()
            passwordToggle = !passwordToggle
            if (passwordToggle) {
                labelEmailPassword.text = "${inputUserEmail.text}\n${inputUserPassword.text}"
                buttonPasswordToggle.setImageResource(R.drawable.hide_password_icon)
            } else {
                labelEmailPassword.text = "${inputUserEmail.text}\n${hidePassword(inputUserPassword.text.toString())}"
                buttonPasswordToggle.setImageResource(R.drawable.show_password_icon)
            }
        }

        buttonLogout.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                hideKeyboard()
                editPreferences.putBoolean("UserLogged", false).apply()
                if (sharedPreferences.getBoolean("TemperatureModeStarted", false)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val jobScheduler = requireActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                        jobScheduler.cancel(9428)
                    }
                }
                firebaseAuth.signOut()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (activity as AppCompatActivity).supportActionBar?.title = "Вход"
                }
                layoutEmailPassword.visibility = View.GONE
                buttonLogout.visibility = View.GONE
                buttonUpdateUser.visibility = View.GONE
                buttonDeleteUser.visibility = View.GONE
                userAccountIcon.visibility = View.GONE
                inputLayoutUserEmail.visibility = View.VISIBLE
                inputLayoutUserPassword.visibility = View.VISIBLE
                buttonSignIn.visibility = View.VISIBLE
                buttonResetPassword.visibility = View.VISIBLE
                Toast.makeText(activity, "Вы вышли из пользователя!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonUpdateUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (activity as AppCompatActivity).supportActionBar?.title = "Обновить Пользователя"
                }
                editPreferences.putBoolean("UpdateUser", true).apply()
                val fragmentManager = requireActivity().supportFragmentManager.beginTransaction()
                fragmentManager.replace(R.id.frameLayout, RegisterUser()).commit()
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        buttonDeleteUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                val alertDialogDeleteUserBuilder = androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                alertDialogDeleteUserBuilder.setTitle("Сброс Пароля")
                alertDialogDeleteUserBuilder.setMessage("Отправка письма для сброса пароля на почту ${inputUserEmail.text}")
                alertDialogDeleteUserBuilder.setPositiveButton("Продолжить") { _, _ ->
                    vibrate()
                    val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                    alertDialogBuilder.setView(layoutInflater.inflate(R.layout.delete_user_progress_bar, null))
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.show()
                    if (sharedPreferences.getBoolean("TemperatureModeStarted", false)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val jobScheduler = requireActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                            jobScheduler.cancel(9428)
                        }
                    }
                    firebaseAuth.currentUser!!.reauthenticate(
                    EmailAuthProvider.getCredential(inputUserEmail.text.toString(),
                    inputUserPassword.text.toString())).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            firebaseAuth.currentUser!!.delete().addOnCompleteListener { deleteUserTask ->
                                if (deleteUserTask.isSuccessful) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        (activity as AppCompatActivity).supportActionBar?.title = "Вход"
                                    }
                                    layoutEmailPassword.visibility = View.GONE
                                    buttonLogout.visibility = View.GONE
                                    buttonUpdateUser.visibility = View.GONE
                                    buttonDeleteUser.visibility = View.GONE
                                    userAccountIcon.visibility = View.GONE
                                    inputLayoutUserEmail.visibility = View.VISIBLE
                                    inputLayoutUserPassword.visibility = View.VISIBLE
                                    buttonSignIn.visibility = View.VISIBLE
                                    buttonResetPassword.visibility = View.VISIBLE
                                    alertDialog.cancel()
                                    editPreferences.putBoolean("UserLogged", false)
                                    editPreferences.putString("UserEmail", "")
                                    editPreferences.putString("UserPassword", "")
                                    editPreferences.putBoolean(inputUserEmail.text.toString(), false).apply()
                                    inputUserEmail.setText("")
                                    inputUserPassword.setText("")
                                    Toast.makeText(activity, "Пользователь удалён!", Toast.LENGTH_LONG).show()
                                } else {
                                    alertDialog.cancel()
                                    layoutEmailPassword.visibility = View.GONE
                                    buttonLogout.visibility = View.GONE
                                    buttonUpdateUser.visibility = View.GONE
                                    buttonDeleteUser.visibility = View.GONE
                                    userAccountIcon.visibility = View.GONE
                                    inputLayoutUserEmail.visibility = View.VISIBLE
                                    inputLayoutUserPassword.visibility = View.VISIBLE
                                    buttonSignIn.visibility = View.VISIBLE
                                    buttonResetPassword.visibility = View.VISIBLE
                                    editPreferences.putBoolean("UserLogged", false).apply()
                                    Toast.makeText(activity, "Не удалось удалить пользователя!", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            alertDialog.cancel()
                            layoutEmailPassword.visibility = View.GONE
                            buttonLogout.visibility = View.GONE
                            buttonUpdateUser.visibility = View.GONE
                            buttonDeleteUser.visibility = View.GONE
                            userAccountIcon.visibility = View.GONE
                            inputLayoutUserEmail.visibility = View.VISIBLE
                            inputLayoutUserPassword.visibility = View.VISIBLE
                            buttonSignIn.visibility = View.VISIBLE
                            buttonResetPassword.visibility = View.VISIBLE
                            editPreferences.putBoolean("UserLogged", false).apply()
                            Toast.makeText(activity, "Не удалось переавторизироваться!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                alertDialogDeleteUserBuilder.setNegativeButton("Отмена") { _, _ ->
                    vibrate()
                }
                val alertDialogDeleteUser = alertDialogDeleteUserBuilder.create()
                alertDialogDeleteUser.show()
            } else {
                Toast.makeText(activity, "Вы не подключены к Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    private fun hidePassword(inputText: String) : String {
        var hiddenPassword = ""
        for (i in inputText.indices) {
            hiddenPassword += "•"
        }
        return hiddenPassword
    }

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun isValidEmail(inputText: CharSequence) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(inputText).matches()
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
    }

    private fun vibrate() {
        if (isHapticFeedbackEnabled) {
            buttonDeleteUser.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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