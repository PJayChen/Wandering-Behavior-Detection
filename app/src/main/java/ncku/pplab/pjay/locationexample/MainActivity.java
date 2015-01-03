package ncku.pplab.pjay.locationexample;

import android.content.Context;
import android.database.Cursor;
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
    private SQLite dbHelper;

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
            String laStr = Double.toString(location.getLatitude());
            String loStr = Double.toString(location.getLongitude());
            latitudeText.setText(laStr);
            longitudeText.setText(loStr);

            Calendar rightNow = Calendar.getInstance();

            String ElapseFromBootInSec = Long.toString(location.getElapsedRealtimeNanos() / 1000000000);
            timeText.setText(ElapseFromBootInSec
                    + "(" + rightNow.getTime().toString() + ")");

            //Save position fix in the SQLite database
            dbHelper.create(ElapseFromBootInSec, laStr + "," + loStr);

//           Cursor cursor = dbHelper.getAll();
//            int rows_num = cursor.getCount();
//            if(rows_num != 0) {
//                cursor.moveToFirst();			//將指標移至第一筆資料
//                for(int i=0; i<rows_num; i++) {
//                    int id = cursor.getInt(0);	//取得第0欄的資料，根據欄位type使用適當語法
//                    String name = cursor.getString(1);
//                    int value = cursor.getInt(2);
//                    Toast.makeText(this, name + " " + Integer.valueOf(name), Toast.LENGTH_SHORT).show();
//                    cursor.moveToNext();		//將指標移至下一筆資料
//                }
//            }
//            cursor.close();
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

        dbHelper = new SQLite(this);
        Toast.makeText(this, "Database Created!", Toast.LENGTH_SHORT).show();

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
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
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
