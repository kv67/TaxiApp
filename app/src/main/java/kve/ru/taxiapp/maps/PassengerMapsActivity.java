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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import kve.ru.taxiapp.BuildConfig;
import kve.ru.taxiapp.R;
import kve.ru.taxiapp.SplashScreenActivity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE;
import static kve.ru.taxiapp.SplashScreenActivity.IS_ACTIVE;
import static kve.ru.taxiapp.maps.DriverMapsActivity.DRIVERS_GEO_FIRE;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

  private static final int CHECK_SETTINGS_CODE = 111;
  private static final int REQUEST_LOCATION_PERMISSION = 222;
  private static final String TAG = "PassengerMapsActivity";
  private static final String PASSENGERS_GEO_FIRE = "passengersGeoFire";
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
  FloatingActionButton buttonBookTaxi;
  TextView textViewState;
  private DatabaseReference driversGeoFire =
      FirebaseDatabase.getInstance().getReference().child(DRIVERS_GEO_FIRE);

  private boolean isLocationActive = false;
  private int searchRadius = 1;
  private boolean isDriverFound = false;
  private String nearestDriverId;

  private Marker curMarker;
  private Marker driverMarker;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_passenger_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    settingsClient = LocationServices.getSettingsClient(this);

    auth = FirebaseAuth.getInstance();
    currentUser = auth.getCurrentUser();

    textViewState = findViewById(R.id.textViewState);
    FloatingActionButton buttonExit = findViewById(R.id.buttonExit);
    FloatingActionButton buttonSettings = findViewById(R.id.buttonSettings);
    buttonBookTaxi = findViewById(R.id.buttonBookTaxi);

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

    buttonBookTaxi.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        buttonBookTaxi.setImageResource(R.drawable.ic_local_taxi_gray_24dp);
        buttonBookTaxi.setEnabled(false);
        textViewState.setText(R.string.getting_taxi_state);
        isDriverFound = false;
        gettingNearestTaxi();
      }
    });

    buildLocationRequest();
    buildLocationCallback();
    buildLocationSettingsRequest();

    startLocationUpdates();
  }

  private void gettingNearestTaxi() {
    GeoFire geoFire = new GeoFire(driversGeoFire);
    GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(currentLocation.getLatitude(),
        currentLocation.getLongitude()), searchRadius);
    geoQuery.removeAllListeners();
    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
      @Override
      public void onKeyEntered(String key, GeoLocation location) {
        if (!isDriverFound) {
          isDriverFound = true;
          nearestDriverId = key;

          getNearestDriverLocation();
        }
      }

      @Override
      public void onKeyExited(String key) {

      }

      @Override
      public void onKeyMoved(String key, GeoLocation location) {

      }

      @Override
      public void onGeoQueryReady() {
        if (!isDriverFound) {
          searchRadius++;
          gettingNearestTaxi();
        }
      }

      @Override
      public void onGeoQueryError(DatabaseError error) {

      }
    });
  }

  private void getNearestDriverLocation() {
    textViewState.setText(R.string.getting_driver_location_state);
    driversGeoFire.child(nearestDriverId).child("l").addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
          List<Object> driverLocationParameters = (List<Object>) dataSnapshot.getValue();
          double latitude = 0;
          double longitude = 0;
          if (driverLocationParameters.get(0) != null) {
            latitude = (Double) driverLocationParameters.get(0);
          }
          if (driverLocationParameters.get(1) != null) {
            longitude = (Double) driverLocationParameters.get(1);
          }
          LatLng driverLatLng = new LatLng(latitude, longitude);
          if (driverMarker != null) {
            driverMarker.remove();
          }

          Location driverLocation = new Location("");
          driverLocation.setLatitude(latitude);
          driverLocation.setLongitude(longitude);

          double distance = driverLocation.distanceTo(currentLocation);
          textViewState.setText(String.format(getString(R.string.distance_to_driver_state),
              distance));

          driverMarker =
              mMap.addMarker(new MarkerOptions().position(driverLatLng).title(getString(R.string.driver_here_msg))  //);
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_marker_100)));

          buttonBookTaxi.setImageResource(R.drawable.ic_local_taxi_orange_24dp);
          buttonBookTaxi.setEnabled(true);
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

      }
    });

  }

  private void signOutPassenger() {
    String passengerUserId = currentUser.getUid();
    DatabaseReference ref =
        FirebaseDatabase.getInstance().getReference().child(PASSENGERS_GEO_FIRE);
    GeoFire geoFire = new GeoFire(ref);
    geoFire.removeLocation(passengerUserId);

    Intent intent = new Intent(PassengerMapsActivity.this, SplashScreenActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra(IS_ACTIVE, true);
    startActivity(intent);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    updateLocationUi();
  }

  private void updateLocationUi() {
    if (currentLocation != null) {
      LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
          currentLocation.getLongitude());

      if (curMarker != null) {
        curMarker.remove();
        ;
      }

      mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
      mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
      curMarker =
          mMap.addMarker(new MarkerOptions().position(passengerLocation).title(getString(R.string.passenger_position_title)));

      String passengerUserId = currentUser.getUid();
      DatabaseReference passengersRef =
          FirebaseDatabase.getInstance().getReference().child(PASSENGERS);
      passengersRef.setValue(true);
      DatabaseReference geoFireRef =
          FirebaseDatabase.getInstance().getReference().child(PASSENGERS_GEO_FIRE);
      GeoFire geoFire = new GeoFire(geoFireRef);
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
