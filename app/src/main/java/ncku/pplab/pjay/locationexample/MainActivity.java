package ncku.pplab.pjay.locationexample;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends ActionBarActivity implements LocationListener{

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private TextView sharpPText;
    private TextView debugText;
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


    private boolean flag_firstFix = true;
    final static double WEIGHT = 0.2;
    double lastLatitude,lastLongitude, currLatitude, currLongitude;
    private void getLocation(Location location){

        if(location != null){

            currLatitude = location.getLatitude();
            currLongitude = location.getLongitude();

            if(flag_firstFix){ //First position fix
                lastLatitude = currLatitude;
                lastLongitude = currLongitude;

                flag_firstFix = false;
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
    private boolean tWD_Flag = true;
    private Runnable runWanderingDetection = new Runnable() {
        @Override
        public void run() {
            try {
                while (tWD_Flag) {
                    Bundle dataBd = new Bundle();
                    Message msg = new Message();

                    Log.v("THREAD", "thread is running");
                    points = new double[6];
                    vectors = new double[4];
                    double angle=0;
                    Cursor cursor = dbHelper.getAll();
                    int rows_num = cursor.getCount();

                    //if(rows_num == 0)break;
                    Log.v("THREAD", "NUM:" + String.valueOf(rows_num) + ", cnt:" + String.valueOf(cnt));
                    dataBd.putString("NUM", "NUM:" + String.valueOf(rows_num) + ", CNT:" + String.valueOf(cnt));


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
                        dataBd.putString("ANGLE", "Angle:" + String.valueOf(angle));

                        if(Math.abs(angle) >= 90) sharpAngle++;
                        //else sharpAngle = 0;

                        Log.d("THREAD", "SharpAngle count:" + String.valueOf(sharpAngle) );

                        cnt = rows_num - 1;
                    }


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
        }
    };


    private String createFixData() {
        String formattedData;
        int decLat = (int) lastLatitude;
        double minLat = new BigDecimal( (lastLatitude - decLat) * 60 ).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue(); //minute part of latitude
        int decLng = (int) lastLongitude;
        double minLng = new BigDecimal( (lastLongitude - decLng) * 60 ).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue(); //minute part of longitude

        String lat = String.valueOf(decLat) + String.valueOf(minLat);
        String lng = String.valueOf(decLng) + String.valueOf(minLng);

        //Now the format of lat and longitude will be
        //ddmm.mmmmN and dddmm.mmmmE
        lat += (decLat >= 0)? 'N' : 'S';
        lng += (decLng >= 0)? 'E' : 'W';

        //produce the format of current date and UTC time which match the GPS module produced.
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat("ddMMyy");
        SimpleDateFormat time = new SimpleDateFormat("HHmmss", Locale.UK); //24hr format
        time.setTimeZone(TimeZone.getTimeZone("GMT"));

        formattedData = date.format(cal.getTime()) + ", "
                + time.format(cal.getTime()) + ".000, "
                + lat + ", "
                + lng;

        Log.v("FIX", formattedData);

        return formattedData;
    }

    private String doPOST(String postData) {
        HttpURLConnection httpConn = null;
        try {
            Log.v("POST", "do post!");
            //Get the data from Views for sending to server
//            String urlMalteseAnn = urlEditText.getText().toString();
//            String postName = nameEditText.getText().toString();
//            String postData = valueEditText.getText().toString();
            String urlMalteseAnn = "http://140.116.156.227:8888/rxfix";
            String postName = "fix";
            String postDataFormat = "300315, 065812.000, 2259.8259N, 12013.3452E";

            if (postDataFormat.length() == postData.length())
                Log.v("FIX", "equal!");
            else
                Log.v("FIX", "not equal!");

            URL url = new URL(urlMalteseAnn);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            httpConn.setUseCaches(false);
            httpConn.connect();

            DataOutputStream dos = new DataOutputStream(httpConn.getOutputStream());

            String postContent = URLEncoder.encode(postName, "UTF-8")
                    + "="
                    + URLEncoder.encode(postData, "UTF-8");

            dos.write(postContent.getBytes());
            dos.flush();
            dos.close();// finish the post request

            //Check the Http Code is 200(HTTP_OK) or NOT
            int respondCode = httpConn.getResponseCode();
            if(respondCode == HttpURLConnection.HTTP_OK) {
                Log.v("POST", "send <" + postName + ", " + postData + "> to " + urlMalteseAnn);
                Log.v("POST", "response OK!");

            }else {
                Log.v("POST", "response fail!");
            }

            //Read the response from the server
            Reader reader = new InputStreamReader(httpConn.getInputStream(), "UTF-8");
            char[] buffer = new char[200];
            int cnt;
            cnt = reader.read(buffer);
            //pick out used bytes in buffer
            String bufferStr = new String(buffer, 0, cnt);

            Log.v("POST", "response msg" + "(" + (cnt) + " bytes)" + ": " + bufferStr);
            return bufferStr;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return new String("Fail, maybe no Internet");
    }

    private Thread tDoHttpPOST;
    private boolean tPOST_flag = true;
    private boolean locationChange = true;
    private Runnable runDoHttpPOST = new Runnable() {
        @Override
        public void run() {
            //set the thread priority lower than UI thread
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            while(tPOST_flag) {
                if (locationChange) {
                    locationChange = false;
                    Bundle bd = new Bundle();
                    String resp;
                    Message msg = new Message();

                    resp = doPOST( createFixData() );

                    //Send the Http POST response to the UI Thread
                    bd.putString("RESP", resp);
                    msg.what = POST_RESP;
                    msg.setData(bd);
                    UI_Handler.sendMessage(msg);
                }
            }
        }
    };

    private static final int SHARP_POINT = 0;
    private static final int POST_RESP = 1;
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

                    String debugMsg;
                    debugMsg = msg.getData().getString("NUM") + "\n" + msg.getData().getString("ANGLE");
                    debugText.setText(debugText.getText() + "\n" + debugMsg);
                    break;
                case POST_RESP:
                    String resp = msg.getData().getString("RESP");
                    debugText.setText(debugText.getText() + resp + '\n');
                    break;
            }
        }
    };

    //Stop thread by set the runnable flag to FLASE
    private boolean stopThread(Thread t){
        //boolean flag = true;
        if(t != null){
            //flag = false;
            t.interrupt();
            Log.d("THREAD", "Thread " + t.getName() + " stop");
            t = null;
        }
        return false;
    }

    private void setViews(){
        latitudeText = (TextView)findViewById(R.id.txtView_Latitude);
        longitudeText = (TextView)findViewById(R.id.txtView_Longitude);

        latitudeText.setText("Locating...");
        longitudeText.setText("Locating...");

        timeText = (TextView)findViewById(R.id.txtView_Time);
        timeText.setText("Getting...");

        sharpPText = (TextView)findViewById(R.id.txtView_sp);
        baseLinearLayout = (LinearLayout) findViewById(R.id.baseLinearLayout);

        debugText = (TextView)findViewById(R.id.txtView_Debug);
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
        tWD_Flag = true;
        //tWanderingDetection.start();

        tDoHttpPOST = new Thread(runDoHttpPOST);
        tPOST_flag = true;
        tDoHttpPOST.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getService){
            lm.requestLocationUpdates(bestProvider, 1000, 2, this);
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

        tWD_Flag = stopThread(tWanderingDetection);
        tWanderingDetection = null;

        dbHelper.close();

        tPOST_flag = stopThread(tDoHttpPOST);
        tDoHttpPOST = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocation(location);
        locationChange = true;
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
