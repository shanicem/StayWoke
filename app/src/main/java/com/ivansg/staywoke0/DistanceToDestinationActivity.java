package com.ivansg.staywoke0;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

public class DistanceToDestinationActivity extends AppCompatActivity {
    Location targetLocation;
    LocationManager locationManager;
    LocationListener locationListener;
    Boolean userAlerted = false;
    // define minimum distance to destination needed before alerting user
    int MIN_DISTANCE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_to_destination);

        // Get the intent that started this activity and extract the values
        Intent intent = getIntent();
        Double STOP_LAT = Double.parseDouble(intent.getStringExtra("STOP_LAT"));
        Double STOP_LON = Double.parseDouble(intent.getStringExtra("STOP_LON"));

        // set target location
        targetLocation = new Location("");
        targetLocation.setLatitude(STOP_LAT);
        targetLocation.setLongitude(STOP_LON);

        trackLocation();
    }

    // Continuously track user's location
    private void trackLocation() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // yell at user if they have not provided location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            return;
        }

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        // get the last known cached location so we don't have to wait for first location update
        String LocationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationProvider);
        if (lastKnownLocation != null) {
            makeUseOfNewLocation(lastKnownLocation);
        }
    }

    // display distance to target after each location update
    private void makeUseOfNewLocation(Location location) {
        Float distance = location.distanceTo(targetLocation) / 1000;
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(String.format("%.2f", distance) + " km");

        // alert the user when they are within a certain km of their destination
        if (distance <= MIN_DISTANCE && !userAlerted) {
            vibratePhone();
            userAlerted = true;
        }
    }

    // vibrate the phone when the user is near their stop
    private void vibratePhone() {

        // vibrate the phone
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator.hasVibrator()) {

            // Pattern to use for vibration
            // [0] - Delay in seconds
            // [1] - Duration of vibration
            // [...] - Pattern repeats
            long [] vibratePattern = new long[]{400, 800, 400, 800, 400, 800, 400, 800, 400, 800};

            // Amplitude pattern to follow for vibration
            // Amplitude will slowly increase until reaching maximum
            int[] amplitudes = new int[] {0, 50, 0, 100, 0, 150, 0, 200, 0, 255};

            if (Build.VERSION.SDK_INT >= 26) {
                VibrationEffect effect = VibrationEffect.createWaveform(vibratePattern, amplitudes, 8);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(vibratePattern, 0);
            }
        }

        displayDialog(vibrator);
    }

    // display a dialog to alert the user when they are near their destination
    private void displayDialog(final Vibrator vibrator) {
        // define dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.alert_dialog, null));

        // Add an OK button, that when selected will stop vibration
        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                vibrator.cancel();
            }
        });

        // create and display dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
