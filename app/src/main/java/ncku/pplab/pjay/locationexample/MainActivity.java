package ncku.pplab.pjay.locationexample;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Calendar;


public class MainActivity extends ActionBarActivity implements LocationListener{

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private boolean getService = false;


    private void testLocationProvider() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            getService = true;
            locationInitial();
        }else{
            Toast.makeText(this, "Please open GPS and Network for location", Toast.LENGTH_SHORT).show();
            //TO-DO finish bellow function
            //startActivities(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    private LocationManager lm;
    private String bestProvider = LocationManager.GPS_PROVIDER;
    private void locationInitial(){
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        bestProvider = lm.getBestProvider(criteria, true);
        Location location = lm.getLastKnownLocation(bestProvider);
        getLocation(location);
    }

    private void getLocation(Location location){
        if(location != null){
            latitudeText.setText(Double.toString(location.getLatitude()));
            longitudeText.setText(Double.toString(location.getLongitude()));

            Calendar rightNow = Calendar.getInstance();

            timeText.setText(Long.toString(location.getElapsedRealtimeNanos()/1000000000)
                    + "(" + rightNow.getTime().toString() + ")");


        }
    }


    private void setViews(){
        latitudeText = (TextView)findViewById(R.id.txtView_Latitude);
        longitudeText = (TextView)findViewById(R.id.txtView_Longitude);

        latitudeText.setText("Locating...");
        longitudeText.setText("Locating...");

        timeText = (TextView)findViewById(R.id.txtView_Time);
        timeText.setText("Getting...");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViews();

        testLocationProvider();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getService){
            lm.requestLocationUpdates(bestProvider, 1000, 1, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(getService){
            lm.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
