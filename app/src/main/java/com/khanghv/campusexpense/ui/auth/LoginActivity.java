package com.khanghv.campusexpense.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khanghv.campusexpense.MainActivity;
import com.khanghv.campusexpense.R;
import com.khanghv.campusexpense.base.BaseActivity;
import com.khanghv.campusexpense.data.database.AppDatabase;
import com.khanghv.campusexpense.data.database.UserDao;
import com.khanghv.campusexpense.data.model.User;

import java.security.MessageDigest;

public class LoginActivity extends BaseActivity {

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private Button btnLogin;
    private UserDao userDao;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView registerText = findViewById(R.id.registerText);
        registerText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);

        btnLogin = findViewById(R.id.btnLogin);
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        btnLogin.setOnClickListener(v -> login());

        if(isLoggedIn()){
            goToMainActivity();
        }
    }

    private boolean isLoggedIn(){
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    private void goToMainActivity(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }

    private void login(){
        String username = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        if (username.isEmpty()){
            usernameLayout.setError(getString(R.string.error_empty_username));
            return;
        }
        if (password.isEmpty()){
            passwordLayout.setError(getString(R.string.error_empty_password));
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.logging_in);

        new Thread(() -> {
            String hashedPassword = hashPassword(password);
            User user = userDao.login(username, hashedPassword);

            runOnUiThread(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login);

                if (user != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("userId", user.getId());
                    editor.putString("username", user.getUsername());
                    editor.apply();
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    usernameLayout.setError(getString(R.string.error_invalid_credentials));
                }
            });
        }).start();
    }
}
