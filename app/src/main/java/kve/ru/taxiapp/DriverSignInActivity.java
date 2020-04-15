package kve.ru.taxiapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class DriverSignInActivity extends AppCompatActivity {

  private TextInputLayout textInputEmail;
  private TextInputLayout textInputName;
  private TextInputLayout textInputPassword;
  private TextInputLayout textInputConfirmPassword;
  private Button buttonLogin;
  private TextView textViewLogin;

  private boolean isLogInModeActive = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_driver_sign_in);

    textInputEmail = findViewById(R.id.textInputEmail);
    textInputName = findViewById(R.id.textInputName);
    textInputPassword = findViewById(R.id.textInputPassword);
    textInputConfirmPassword = findViewById(R.id.textInputConfirmPassword);

    buttonLogin = findViewById(R.id.buttonLogin);
    textViewLogin = findViewById(R.id.textViewLogin);

  }

  private boolean validateEmail() {
    String email = textInputEmail.getEditText().getText().toString().trim();
    if (email == null || email.isEmpty()) {
      textInputEmail.setError(getString(R.string.empty_email_msg));
      return false;
    } else {
      textInputEmail.setError("");
      return true;
    }
  }

  private boolean validateName() {
    String name = textInputName.getEditText().getText().toString().trim();
    if (name == null || name.isEmpty()) {
      textInputName.setError(getString(R.string.empty_name_msg));
      return false;
    } else
      if (name.length() > 15) {
        textInputName.setError(getString(R.string.name_length_msg));
        return false;
      } else {
        textInputName.setError("");
        return true;
      }
  }

  private boolean validatePassword() {
    String password = textInputPassword.getEditText().getText().toString().trim();
    String confirmPassword = textInputConfirmPassword.getEditText().getText().toString().trim();
    if (password == null || password.isEmpty()) {
      textInputPassword.setError(getString(R.string.empty_password_msg));
      return false;
    } else
      if (password.length() < 6) {
        textInputPassword.setError(getString(R.string.password_length_msg));
        return false;
      } else
        if (!password.equals(confirmPassword)) {
          textInputPassword.setError(getString(R.string.passwords_match_msg));
          return false;
        } else {
          textInputPassword.setError("");
          return true;
        }
  }

  public void signInLogInUser(View view) {
    if (!validateEmail() | !validateName() | !validatePassword()) {
      return;
    } else {
      Toast.makeText(this, "OK", Toast.LENGTH_LONG).show();
    }
  }

  public void toggleSignInLogIn(View view) {
    if (isLogInModeActive) {
      isLogInModeActive = false;
      buttonLogin.setText(R.string.sign_up_btn);
      textViewLogin.setText(R.string.log_in_link);
      textInputName.setVisibility(View.VISIBLE);
      textInputConfirmPassword.setVisibility(View.VISIBLE);
    } else {
      isLogInModeActive = true;
      buttonLogin.setText(R.string.log_in_link);
      textViewLogin.setText(R.string.sign_up_btn);
      textInputName.setVisibility(View.GONE);
      textInputConfirmPassword.setVisibility(View.GONE);
    }
  }
}
