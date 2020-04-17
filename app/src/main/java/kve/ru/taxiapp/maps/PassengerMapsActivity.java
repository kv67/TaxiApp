package kve.ru.taxiapp.maps;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import kve.ru.taxiapp.BuildConfig;
import kve.ru.taxiapp.R;
import kve.ru.taxiapp.SplashScreenActivity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE;
import static kve.ru.taxiapp.SplashScreenActivity.IS_ACTIVE;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

  private static final int CHECK_SETTINGS_CODE = 111;
  private static final int REQUEST_LOCATION_PERMISSION = 222;
  private static final String TAG = "PassengerMapsActivity";
  private static final String PASSENGERS = "passengers";

  private GoogleMap mMap;

  private FusedLocationProviderClient fusedLocationClient;
  private SettingsClient settingsClient;
  private LocationRequest locationRequest;
  private LocationSettingsRequest locationSettingsRequest;
  private LocationCallback locationCallback;
  private Location currentLocation;

  private FirebaseAuth auth;
  private FirebaseUser currentUser;

  private boolean isLocationActive = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_passenger_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    settingsClient = LocationServices.getSettingsClient(this);

    auth = FirebaseAuth.getInstance();
    currentUser = auth.getCurrentUser();

    FloatingActionButton buttonExit = findViewById(R.id.buttonExit);
    FloatingActionButton buttonSettings = findViewById(R.id.buttonSettings);

    buttonExit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        auth.signOut();
        signOutPassenger();
      }
    });

    buttonSettings.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //
      }
    });

    buildLocationRequest();
    buildLocationCallback();
    buildLocationSettingsRequest();

    startLocationUpdates();
  }

  private void signOutPassenger() {
    String passengerUserId = currentUser.getUid();
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(PASSENGERS);
    GeoFire geoFire = new GeoFire(ref);
    geoFire.removeLocation(passengerUserId);

    Intent intent = new Intent(PassengerMapsActivity.this, SplashScreenActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra(IS_ACTIVE, true);
    startActivity(intent);
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    updateLocationUi();
  }

  private void updateLocationUi() {
    if (currentLocation != null) {
      LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
          currentLocation.getLongitude());
      mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
      mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
      mMap.addMarker(new MarkerOptions().position(passengerLocation).title(getString(R.string.passenger_position_title)));

      String passengerUserId = currentUser.getUid();
      DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(PASSENGERS);
      GeoFire geoFire = new GeoFire(ref);
      geoFire.setLocation(passengerUserId, new GeoLocation(currentLocation.getLatitude(),
          currentLocation.getLongitude()));
    }
  }

  private void stopLocationUpdates() {
    if (isLocationActive) {
      fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(this,
          new OnCompleteListener<Void>() {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
          isLocationActive = false;
        }
      });
    }
  }

  private void startLocationUpdates() {
    isLocationActive = true;

    settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(this,
        new OnSuccessListener<LocationSettingsResponse>() {
      @Override
      public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
            Looper.myLooper());
        updateLocationUi();
      }
    }).addOnFailureListener(this, new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        int statusCode = ((ApiException) e).getStatusCode();

        switch (statusCode) {
          case RESOLUTION_REQUIRED:
            try {
              ResolvableApiException resolvableApiException = (ResolvableApiException) e;
              resolvableApiException.startResolutionForResult(PassengerMapsActivity.this,
                  CHECK_SETTINGS_CODE);
            } catch (IntentSender.SendIntentException ex) {
              ex.printStackTrace();
            }
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            Toast.makeText(PassengerMapsActivity.this, R.string.adjust_settings_msg,
                Toast.LENGTH_LONG).show();
            isLocationActive = false;
            break;
          default:
            break;
        }
        updateLocationUi();
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CHECK_SETTINGS_CODE) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          Log.d(TAG, "User has agreed to change location settings");
          startLocationUpdates();
          break;
        case Activity.RESULT_CANCELED:
          Log.d(TAG, "User has NOT agreed to change location settings");
          isLocationActive = false;
          break;
        default:
          break;
      }
    }
  }

  private void buildLocationSettingsRequest() {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(locationRequest);
    locationSettingsRequest = builder.build();
  }

  private void buildLocationCallback() {
    locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        currentLocation = locationResult.getLastLocation();
        updateLocationUi();
      }
    };
  }

  private void buildLocationRequest() {
    locationRequest = new LocationRequest();
    locationRequest.setInterval(10000);
    locationRequest.setFastestInterval(3000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopLocationUpdates();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isLocationActive && checkLocationPermission()) {
      startLocationUpdates();
    } else
      if (!checkLocationPermission()) {
        requestLocationPermission();
      }
  }

  private void requestLocationPermission() {
    boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
        ACCESS_FINE_LOCATION);
    if (shouldProvideRationale) {
      showSnackBar(getString(R.string.permission_reason_msg), getString(R.string.ok_title),
          new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          ActivityCompat.requestPermissions(PassengerMapsActivity.this,
              new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
      });
    } else {
      ActivityCompat.requestPermissions(PassengerMapsActivity.this,
          new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }
  }

  private void showSnackBar(final String mainText, final String action,
      View.OnClickListener listener) {
    Snackbar.make(findViewById(android.R.id.content), mainText, LENGTH_INDEFINITE).setAction(action, listener).show();
  }

  private boolean checkLocationPermission() {
    int permissionState = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
    return permissionState == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == REQUEST_LOCATION_PERMISSION) {
      if (grantResults.length <= 0) {
        Log.d(TAG, "Permission request was canceled");
      } else
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (isLocationActive) {
            startLocationUpdates();
          }
        } else {
          showSnackBar(getString(R.string.turn_on_settings_msg),
              getString(R.string.settings_title), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent intent = new Intent();
              intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
              intent.setData(uri);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }
          });
        }
    }
  }
}
