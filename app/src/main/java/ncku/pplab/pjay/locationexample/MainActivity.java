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

    //Running average
    private double findRACentroid(double[] points, double newPoint)
    {
        double sum = 0;
        //shift elements
        for(int i=points.length;i>0;i--){
            sum += points[i-1];
            points[i] = points[i-1];
        }
        points[0] = newPoint;
        sum += newPoint;
        return sum /= 10;
    }

    private boolean flag_firstFix = true;
    final static double WEIGHT = 0.95;
    double lastLatitude,lastLongitude, currLatitude, currLongitude;
    int intLatitude, intLongitude;
    private double tapsLatitude[];
    private double tapsLongitude[];
    private void getLocation(Location location){
        tapsLatitude = new double[10];
        tapsLongitude = new double[10];

        if(location != null){

            intLatitude = (int)(location.getLatitude() * 1000000);
            intLongitude = (int)(location.getLongitude() * 1000000);
            currLatitude = (double)intLatitude/1000000;
            currLongitude = (double)intLongitude/1000000;

            if(flag_firstFix){ //First position fix
                lastLatitude = currLatitude;
                lastLongitude = currLongitude;
            }else{ //Use running average to reduce the error
                lastLatitude = lastLatitude * WEIGHT + (1-WEIGHT) * currLatitude;
                lastLongitude = lastLongitude * WEIGHT + (1-WEIGHT) * currLongitude;
            }

            //show position fix on the TextView
            latitudeText.setText(Double.toString(lastLatitude));
            longitudeText.setText(Double.toString(lastLongitude));

            //Get the current time
            Calendar rightNow = Calendar.getInstance();

            //Get the time elapse from the device boot
            String ElapseFromBootInSec = Long.toString(location.getElapsedRealtimeNanos() / 1000000000);
            timeText.setText(ElapseFromBootInSec
                    + "(" + rightNow.getTime().toString() + ")");

            //Save position fix in the SQLite database
            dbHelper.create(ElapseFromBootInSec, Double.toString(lastLatitude), Double.toString(lastLongitude));

            //Read the data store in the database
//            Cursor cursor = dbHelper.getAll();
//            int rows_num = cursor.getCount();
//            if(rows_num != 0) {
//                cursor.moveToFirst();			//將指標移至第一筆資料
//                for(int i=0; i<rows_num; i++) {
//                    int id = cursor.getInt(0);	//取得第0欄的資料，根據欄位type使用適當語法
//                    String time = cursor.getString(1);
//                    String latitude = cursor.getString(2);
//                    String longitude = cursor.getString(3);
//                    Toast.makeText(this, time + " " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
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
            lm.requestLocationUpdates(bestProvider, 100, 1, this);
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
