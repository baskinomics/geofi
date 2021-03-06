/**
* @TODO Provide description.
*/

package com.baskinomics.geofi;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements 
    LocationListener, 
    GooglePlayServicesClient.ConnectionCallbacks, 
    GooglePlayServicesClient.OnConnectionFailedListener {

    // Maps and Location
    private GoogleMap mGoogleMap;
    private LocationRequest mLocationRequest;
    private LocationClient mLocationClient;
    private Location mCurrentLocation;
    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private float mCurrentBearing;
    private double mCurrentAltitude;
    private float mCurrentSpeed;
    private long mCurrentUtcTime;
    private float mCurrentAccuracy;
    private String mCurrentProvider;
    public boolean mUpdatesRequested = false;

    // WiFi Attributes
    private WifiManager wifiManager;
    private WifiInfo mCurrentWifiInfo;
    private int mCurrentIpAddress;
    private int mCurrentLinkSpeed;
    private String mCurrentMacAddress;
    private String mCurrentSSID;
    private int mCurrentRSSI;

    // Data structure to store information
    private ArrayList<DataRecord> dataRecords = new ArrayList<DataRecord>();

    // Constants
    private static final String TAG = "GeoFi";
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // UI 
    private boolean inPlayState = false;
    private boolean inStopState = false;
    private boolean inWriteState = false;

    /*
    * Activity lifecycle callbacks
    */

    /**
    * @TODO Add description.
    * 
    * @param savedInstanceState
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Obtain a reference to Google Map
        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        // Enable My Location Layer
        mGoogleMap.setMyLocationEnabled(true);
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        // Set the update interval
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        // Note that location updates are off until the user turns them on
        mUpdatesRequested = true;
        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(this, this, this);

        wifiManager = (WifiManager) getBaseContext()
                            .getSystemService(Context.WIFI_SERVICE);
    }

    /**
    * @TODO Add description.
    */
    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
        checkExternalMedia();
    }

    /**
    * @TODO Add description.
    */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
    * @TODO Add description.
    */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
    * @TODO Add description.
    */
    @Override
    protected void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }

        mLocationClient.disconnect();
        super.onStop();
    }

    /*
    * ActionBar Menu
    */

    /**
    * @TODO Add description.
    *
    * @param menu
    */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu items for use in the action bar
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_activity_actions, menu);
    	return super.onCreateOptionsMenu(menu);
	}

    /**
    * @TODO Add description.
    *
    * @param item
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handles presses on the action bar items
        switch (item.getItemId()) {

            case R.id.action_start:
                if (inPlayState == false && inStopState == false && inWriteState == false) {
                /**
                 * Represents the inital state of the application.
                 */
                    mLocationClient.requestLocationUpdates(mLocationRequest, this);
                    inPlayState = true;
                    Toast.makeText(this, "Beginning new data set!", Toast.LENGTH_LONG).show();
                } else if (inPlayState == false && inStopState == true && inWriteState == false) {
                /**
                 * Represents the state change from a user pausing and then continuing to record.
                 */
                    mLocationClient.requestLocationUpdates(mLocationRequest, this);
                    inPlayState = true;
                    inStopState = false;
                    Toast.makeText(this, "Continuing collection of records for current data set!", Toast.LENGTH_LONG).show();
                } else if (inPlayState == false && inStopState == false && inWriteState == true) {
                /**
                 * Represents the state change from writing to a data file and starting a new set of data.
                 */
                    mLocationClient.requestLocationUpdates(mLocationRequest, this);
                    inPlayState = true;
                    inWriteState = false;
                    Toast.makeText(this, "Beginning new data set!", Toast.LENGTH_LONG).show();
                } else {
                /**
                 * Represents pressing the play button while already in the recording state.
                 */
                    Toast.makeText(this, "Already recording data!", Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.action_stop:
                if (inPlayState == false && inStopState == false && inWriteState == false) {
                /**
                 * Represents a user pressing the pause button in the application's initial state.
                 */
                } else if (inPlayState == false && inStopState == false && inWriteState == true) {
                /**
                 * Represents an illegal transition from the write state to the pause state.
                 */
                    Toast.makeText(this, "No data is currently being recorded", Toast.LENGTH_LONG).show();
                } else {
                /**
                 * Represents an illegal transition from the write state to the pause state.
                 */
                    if (mLocationClient.isConnected())
                        mLocationClient.removeLocationUpdates(this);
                    inPlayState = false;
                    inStopState = true;
                }

                Log.i(TAG, "Stop button was pressed.");
                return true;

            case R.id.action_write:
                if (inPlayState == false && inStopState == false && inWriteState == false) {
                /**
                 * Represents a user pressing the write button in the application's initial state.
                 */
                    Toast.makeText(this, "To begin recording data, press the \"Play\" button", Toast.LENGTH_LONG).show();
                } else if (inPlayState == true && inStopState == false && inWriteState == false) {
                /**
                 * Represents an illegal transition from the write state to the pause state.
                 */
                    Toast.makeText(this, "Press \"Pause\" button before recording data.", Toast.LENGTH_LONG).show();
                } else {
                    writeDataRecordsToFile();
                    inPlayState = false;
                    inStopState = false;
                    inWriteState = true;
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :
                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));
                        // Display the result
                        Toast.makeText(this, R.string.connected, Toast.LENGTH_LONG).show();
                        Toast.makeText(this, R.string.resolved, Toast.LENGTH_LONG).show();
                    break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));
                        // Display the result
                        Toast.makeText(this, R.string.disconnected, Toast.LENGTH_LONG).show();
                        Toast.makeText(this, R.string.no_resolution, Toast.LENGTH_LONG).show();
                    break;
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(LocationUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));
               break;
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Toast.makeText(this, R.string.disconnected, Toast.LENGTH_LONG).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            // showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * 
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        getCurrentLocationWiFiAttr();
        DataRecord dataRecord = new DataRecord();
        dataRecord.setLocationAttributes(mCurrentLatitude, mCurrentLongitude, mCurrentBearing, mCurrentAltitude, mCurrentSpeed, mCurrentAccuracy, mCurrentProvider, mCurrentUtcTime);
        dataRecord.setWifiAttributes(mCurrentIpAddress, mCurrentLinkSpeed, mCurrentMacAddress, mCurrentSSID, mCurrentRSSI);
        // mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(dataRecord.getLat(), dataRecord.getLon())).title("Hello!"));  
        dataRecords.add(dataRecord);

        // Debugging
        String latitudeRecord = Double.toString(dataRecord.getLat());
        String longitudeRecord = Double.toString(dataRecord.getLon());
        String rssiRecord = Integer.toString(dataRecord.getRSSI());
        String debugString = latitudeRecord + " " + longitudeRecord + " " + rssiRecord;
        Log.i(TAG, debugString);
    }

    /**
    * Retrieve the last known location attributes and WiFi information.
    */
    public void getCurrentLocationWiFiAttr() {
        // Location Attributes
        mCurrentLocation = mLocationClient.getLastLocation();
        mCurrentLatitude = mCurrentLocation.getLatitude();
        mCurrentLongitude = mCurrentLocation.getLongitude();
        mCurrentUtcTime = mCurrentLocation.getTime();
        mCurrentProvider = mCurrentLocation.getProvider();

        if (mCurrentLocation.hasBearing())
            mCurrentBearing = mCurrentLocation.getBearing();
        
        if (mCurrentLocation.hasAltitude())
            mCurrentAltitude = mCurrentLocation.getAltitude();

        if (mCurrentLocation.hasSpeed())
            mCurrentSpeed = mCurrentLocation.getSpeed();

        if (mCurrentLocation.hasAccuracy())
            mCurrentAccuracy = mCurrentLocation.getAccuracy();

        // WiFi Attributes
        mCurrentWifiInfo = wifiManager.getConnectionInfo();
        mCurrentRSSI = mCurrentWifiInfo.getRssi();
        mCurrentIpAddress = mCurrentWifiInfo.getIpAddress();
        mCurrentLinkSpeed = mCurrentWifiInfo.getLinkSpeed();
        mCurrentMacAddress = mCurrentWifiInfo.getMacAddress();
        mCurrentSSID = mCurrentWifiInfo.getSSID();

    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /** 
     * Checks and displays the external media status.
     */
    private void checkExternalMedia() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWritable = false;
        String storageState = Environment.getExternalStorageState();
        File sdCard = Environment.getExternalStorageDirectory();
        
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            // Can read and write media
            mExternalStorageAvailable = true;
            mExternalStorageWritable = true;
            Log.i(TAG, "Media Mounted Read-Write");
            Log.i(TAG, "External Media sdCard Directory: " + sdCard.getAbsolutePath());
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
            //
            mExternalStorageAvailable = true;
            mExternalStorageWritable = false;
            Log.i(TAG, "Media Read-Only");
        } else {
            //
            mExternalStorageAvailable = false;
            mExternalStorageWritable = true;
            Log.i(TAG, "Media Write-Only");
        }
    }

    /**
    * Write data records to a text file on the SD card. 
    */
    public void writeDataRecordsToFile() {
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/wifi_location_data_records/");
            if (!directory.exists()) {
                if (!directory.mkdirs())
                    throw new FileNotFoundException("Couldn't make directory");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
            long sysTime = System.currentTimeMillis();
            Date sysDate = new Date(sysTime);
            File dataRecordsFile = new File(directory, "RSSI-observations-" + sdf.format(sysDate) + ".txt");

            //FileOutputStream fileOutputStream = new FileOutputStream(dataRecordsFile);
            PrintWriter printWriter = new PrintWriter(new FileWriter(dataRecordsFile));

            printWriter.println("Latitude,Longitude,Altitude,Bearing,Speed,Accuracy,Provider,UTC_Time,SSID,MAC_Address,IP_Address,RSSI,Link_Speed" + "\r");
            // DEBUG
            String debugHeaders = "Latitude,Longitude,Altitude,Bearing,Speed,Accuracy,Provider,UTC_Time,SSID,MAC_Address,IP_Address,RSSI,Link_Speed" + "\r";
            Log.i(TAG, debugHeaders);
            for (DataRecord record : dataRecords) {
                String latitudeRecord = Double.toString(record.getLat());
                String longitudeRecord = Double.toString(record.getLon());
                String altitudeRecord = Double.toString(record.getAltitude());
                String bearingRecord = Float.toString(record.getBearing());
                String speedRecord = Float.toString(record.getSpeed());
                String accuracyRecord = Float.toString(record.getAccuracy());
                String providerRecord = record.getProvider();
                String utcTimeRecord = Long.toString(record.getUtcTime());
                String ssidRecord = record.getSSID();
                String macAddressRecord = record.getMacAddress();
                String ipAddressRecord = Integer.toString(record.getIpAddress());
                String rssiRecord = Integer.toString(record.getRSSI());
                String linkSpeedRecord = Integer.toString(record.getLinkSpeed());
                // Print values
                printWriter.println(latitudeRecord + "," + 
                                    longitudeRecord + "," + 
                                    altitudeRecord + "," + 
                                    bearingRecord + "," + 
                                    speedRecord + "," + 
                                    accuracyRecord + "," + 
                                    providerRecord + "," + 
                                    utcTimeRecord + "," +
                                    ssidRecord + "," + 
                                    macAddressRecord + "," + 
                                    ipAddressRecord + "," + 
                                    rssiRecord + "," + 
                                    linkSpeedRecord + "\r");
                // DEBUG
                String debugDataRecord = latitudeRecord + "," + 
                                    longitudeRecord + "," + 
                                    altitudeRecord + "," + 
                                    bearingRecord + "," + 
                                    speedRecord + "," + 
                                    accuracyRecord + "," + 
                                    providerRecord + "," + 
                                    utcTimeRecord + "," +
                                    ssidRecord + "," + 
                                    macAddressRecord + "," + 
                                    ipAddressRecord + "," + 
                                    rssiRecord + "," + 
                                    linkSpeedRecord + "\r";

                Log.i(TAG, debugDataRecord);
            }
            // printWriter.flush();
            printWriter.close();
            // fileOutputStream.close();
            // Remove all elements from dataRecords ArrayList for reuse
            dataRecords.clear();
            // Debugging
            String debugString = "Data written to " + dataRecordsFile.getAbsolutePath();
            Log.i(TAG, debugString);
        } catch(FileNotFoundException e) {
            Log.i(TAG, "FileNotFoundException");
            e.printStackTrace();
        } catch(IOException e) {
            Log.i(TAG, "IOException");
            e.printStackTrace();
        }
    }
}