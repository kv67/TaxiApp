package kve.ru.taxiapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static java.lang.Thread.sleep;

public class SplashScreenActivity extends AppCompatActivity {

  private static final String IS_ACTIVE = "IS_ACTIVE";

  private Button buttonPassenger;
  private Button buttonDriver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_screen);

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

    if (savedInstanceState != null && savedInstanceState.containsKey(IS_ACTIVE) && savedInstanceState.getBoolean(IS_ACTIVE)) {
      buttonPassenger.setVisibility(View.VISIBLE);
      buttonDriver.setVisibility(View.VISIBLE);
    } else {
      SplashTask task = new SplashTask();
      task.setOnFinishListener(new SplashTask.OnFinishListener() {
        @Override
        public void onFinish() {
          buttonPassenger.setVisibility(View.VISIBLE);
          buttonDriver.setVisibility(View.VISIBLE);
        }
      });
      task.execute();
    }

  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean(IS_ACTIVE, true);
    super.onSaveInstanceState(outState);
  }

  private static class SplashTask extends AsyncTask<Void, Void, Void> {

    private OnFinishListener onFinishListener;

    public void setOnFinishListener(OnFinishListener onFinishListener) {
      this.onFinishListener = onFinishListener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        sleep(5000);
      } catch (Exception e) {
        e.printStackTrace();
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
