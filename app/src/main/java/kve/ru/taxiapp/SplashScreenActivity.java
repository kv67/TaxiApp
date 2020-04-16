package kve.ru.taxiapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import kve.ru.taxiapp.maps.DriverMapsActivity;

import static java.lang.Thread.sleep;

public class SplashScreenActivity extends AppCompatActivity {

  public static final String IS_ACTIVE = "IS_ACTIVE";

  private Button buttonPassenger;
  private Button buttonDriver;

  private FirebaseAuth auth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_screen);

    auth = FirebaseAuth.getInstance();

    buttonPassenger = findViewById(R.id.buttonPassenger);
    buttonDriver = findViewById(R.id.buttonDriver);

    buttonPassenger.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(SplashScreenActivity.this, PassengerSignInActivity.class));
      }
    });

    buttonDriver.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(SplashScreenActivity.this, DriverSignInActivity.class));
      }
    });

    if ((savedInstanceState != null && savedInstanceState.containsKey(IS_ACTIVE) && savedInstanceState.getBoolean(IS_ACTIVE)) || (getIntent() != null && getIntent().getBooleanExtra(IS_ACTIVE, false))) {
      initUi();
    } else {
      SplashTask task = new SplashTask();
      task.setOnFinishListener(new SplashTask.OnFinishListener() {
        @Override
        public void onFinish() {
          initUi();
        }
      });
      task.execute();
    }

  }

  private void initUi() {
    buttonPassenger.setVisibility(View.VISIBLE);
    buttonDriver.setVisibility(View.VISIBLE);
    if (auth.getCurrentUser() != null) {
      startActivity(new Intent(SplashScreenActivity.this, DriverMapsActivity.class));
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean(IS_ACTIVE, true);
    super.onSaveInstanceState(outState);
  }

  private static class SplashTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "SplashTask";

    private OnFinishListener onFinishListener;

    public void setOnFinishListener(OnFinishListener onFinishListener) {
      this.onFinishListener = onFinishListener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        sleep(5000);
      } catch (Exception e) {
        Log.e(TAG, "Sleep error", e);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (onFinishListener != null) {
        onFinishListener.onFinish();
      }
    }

    public interface OnFinishListener {
      void onFinish();
    }
  }
}
