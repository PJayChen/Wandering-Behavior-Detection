package ncku.pplab.pjay.locationexample;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.util.Calendar;


public class MainActivity extends ActionBarActivity implements LocationListener{

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private TextView sharpPText;
    private LinearLayout baseLinearLayout;

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
//    private double findRACentroid(double[] points, double newPoint)
//    {
//        double sum = 0;
//
//        //shift elements
//        for(int i=points.length-1; i>0; i--){
//            sum += points[i-1];
//            points[i] = points[i-1];
//        }
//        points[0] = newPoint;
//
//        sum += newPoint;
//        return (sum / points.length);
//    }

//    private static final String TAG = "TAPS";
    private boolean flag_firstFix = true;
    final static double WEIGHT = 0.1;
    double lastLatitude,lastLongitude, currLatitude, currLongitude;
//    int intLatitude, intLongitude;
//    private double tapsLatitude[] = new double[10];
//    private double tapsLongitude[] = new double[10];
//    private double centroidOfLa;
//    private double centroidOfLo;
    private void getLocation(Location location){

        if(location != null){

//            intLatitude = (int)(location.getLatitude() * 1000000);
//            intLongitude = (int)(location.getLongitude() * 1000000);
//            currLatitude = (double)intLatitude/1000000;
//            currLongitude = (double)intLongitude/1000000;
            currLatitude = location.getLatitude();
            currLongitude = location.getLongitude();

            if(flag_firstFix){ //First position fix
                lastLatitude = currLatitude;
                lastLongitude = currLongitude;

                //initial taps
//                for(int i=0; i < tapsLongitude.length; i++){
//                    tapsLongitude[i] = lastLongitude;
//                    tapsLatitude[i] = lastLatitude;
//                }

                flag_firstFix = false;
            }else{ //Use running average to reduce the error
                lastLatitude = lastLatitude * WEIGHT + (1-WEIGHT) * currLatitude;
                lastLongitude = lastLongitude * WEIGHT + (1-WEIGHT) * currLongitude;
            }


//            Log.v(TAG, "tapsLongitude Before Shift");
//            for(int i=tapsLongitude.length-1; i>=0; i--){
//                Log.v(TAG, Double.toString(tapsLongitude[i]));
//            }

            //find centroid of continuous 10 position fix
            //centroidOfLa = findRACentroid(tapsLatitude, lastLatitude);
            //centroidOfLo = findRACentroid(tapsLongitude, lastLongitude);

//            Log.v(TAG, "tapsLongitude After Shift");
//            for(int i=tapsLongitude.length-1; i>=0; i--){
//                Log.v(TAG, Double.toString(tapsLongitude[i]));
//            }

//            intLatitude = (int)(centroidOfLa * 1000000);
//            intLongitude = (int)(centroidOfLo * 1000000);
//            centroidOfLa = (double)intLatitude/1000000;
//            centroidOfLo = (double)intLongitude/1000000;

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

    //(y1, x1, y2, x2), 1: last vector, 2: next vector
    private double getAngleOfTwoVectors(double laP1, double loP1, double laP2, double loP2){
        double angle = 0;
        double []vector;
        vector = new double[2];
        vector[0] = laP2 - laP1;
        vector[1] = loP2 - loP1;

        //angle = Math.atan2(laP2 - laP1, loP2 - loP1);// - Math.atan2(laP1, loP1);
        angle = Math.atan2(laP2, loP2) - Math.atan2(laP1, loP1);
        return Math.toDegrees(angle);
    }

    private Thread tWanderingDetection;
    private int cnt = 0;
    private double[] points;
    private double[] vectors;
    private int sharpAngle = 0;
    private Runnable runWanderingDetection = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    Log.v("THREAD", "thread is running");
                    points = new double[6];
                    vectors = new double[4];
                    double angle;
                    Cursor cursor = dbHelper.getAll();
                    int rows_num = cursor.getCount();

                    //if(rows_num == 0)break;
                    Log.v("THREAD", "NUM:" + String.valueOf(rows_num) + ", cnt:" + String.valueOf(cnt));


                    if((rows_num != 0) && ((rows_num - cnt) >= 3)){
                        cursor.moveToFirst();
                        for(int i=1; i<cnt; i++){
                            cursor.moveToNext();
                        }
                        for(int i=0; i<5; i+=2) {
                            points[i] = Double.valueOf(cursor.getString(2)); //latitude
                            points[i+1] = Double.valueOf(cursor.getString(3)); //longitude
                            Log.i("THREAD", cursor.getString(2) + ", " + cursor.getString(3));
                            //if(i >= 2)break;
                            cursor.moveToNext();
                        }
                        vectors[0] = points[2] - points[0]; //latitude of vector1
                        vectors[1] = points[3] - points[1]; //longitude of vector1
                        vectors[2] = points[4] - points[2]; //latitude of vector2
                        vectors[3] = points[5] - points[3]; //longitude of vector2

                        angle = getAngleOfTwoVectors(vectors[0], vectors[1], vectors[2], vectors[3]);
                        Log.d("THREAD", String.valueOf(vectors[0]) + ", " + String.valueOf(vectors[0]) + ". " + String.valueOf(vectors[2]) + ", " + String.valueOf(vectors[3]) );
                        Log.d("THREAD", "Angle:" + String.valueOf(angle));

                        if(Math.abs(angle) >= 90) sharpAngle++;
                        Log.d("THREAD", "SharpAngle count:" + String.valueOf(sharpAngle) );

                        cnt = rows_num - 1;
                    }

                    Bundle dataBd = new Bundle();
                    Message msg = new Message();
                    dataBd.putInt("Sharp", sharpAngle);
                    msg.what = SHARP_POINT;
                    msg.setData(dataBd);
                    UI_Handler.sendMessage(msg);

                    cursor.close();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            Cursor cursor = dbHelper.getAll();
//            int rows_num = cursor.getCount();
//            if(rows_num != 0){
//                cursor.moveToFirst();
//                for(int i=0; i<rows_num; i++) {
//                    int id = cursor.getInt(0);	//取得第0欄的資料，根據欄位type使用適當語法
//                    String time = cursor.getString(1);
//                    String latitude = cursor.getString(2);
//                    String longitude = cursor.getString(3);
//                    cursor.moveToNext();		//將指標移至下一筆資料
//                    Log.v("THREAD",Integer.toString(id) + ")" + time + ", " + latitude + ", " + longitude);
//                }
//            }
//            cursor.close();
        }
    };
    private static final int SHARP_POINT = 0, WANDERING = 1;
    //UI(main) Thread handler
    private Handler UI_Handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case SHARP_POINT:
                    int sharpCnt = msg.getData().getInt("Sharp");
                    sharpPText.setText(String.valueOf(sharpCnt));
                    if(sharpCnt >= 3) baseLinearLayout.setBackgroundColor(Color.RED);
                    else baseLinearLayout.setBackgroundColor(Color.rgb(155,155,155));
                    break;
            }
        }
    };

    private void setViews(){
        latitudeText = (TextView)findViewById(R.id.txtView_Latitude);
        longitudeText = (TextView)findViewById(R.id.txtView_Longitude);

        latitudeText.setText("Locating...");
        longitudeText.setText("Locating...");

        timeText = (TextView)findViewById(R.id.txtView_Time);
        timeText.setText("Getting...");

        sharpPText = (TextView)findViewById(R.id.txtView_sp);
        baseLinearLayout = (LinearLayout) findViewById(R.id.baseLinearLayout);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViews();

        dbHelper = new SQLite(this);
        dbHelper.upgrade();
        Toast.makeText(this, "Database Created!", Toast.LENGTH_SHORT).show();

        testLocationProvider();

        tWanderingDetection = new Thread(runWanderingDetection);
        tWanderingDetection.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getService){
            lm.requestLocationUpdates(bestProvider, 5000, 3, this);
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
