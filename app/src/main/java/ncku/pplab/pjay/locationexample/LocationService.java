package ncku.pplab.pjay.locationexample;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by pjay on 2015/4/30.
 */
public class LocationService extends Service implements LocationListener {

    private static final String TAG = "SERVICE";

    public static final String LOCATION_RESULT = "LOCATION_RESULT";
    public static final String LOCATION_MESSAGE = "LOCATION_MESSAGE";

    public static final String INTENT_HTTP_RESP = "HttpResp";
    public static final String INTENT_LATITUDE = "lat";
    public static final String INTENT_LONGITUDE = "lng";
    public static final String INTENT_CURRENT_TIME = "date";

    private void sendResultToMainActivity(String intentName, String megName, Bundle msgBundle){
        Intent intent = new Intent(intentName);
        if (msgBundle != null) {
            intent.putExtra(megName, msgBundle);

            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(intent);
        }
    }


    /* ---------------------- Method about location ----------------------- */

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
    final static double WEIGHT = 0.01;
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

            //Get the time elapse from the device boot
//            String ElapseFromBootInSec = Long.toString(location.getElapsedRealtimeNanos() / 1000000000);
//            timeText.setText(ElapseFromBootInSec
//                    + "(" + rightNow.getTime().toString() + ")");

            //Save position fix in the SQLite database
            //dbHelper.create(ElapseFromBootInSec, Double.toString(lastLatitude), Double.toString(lastLongitude));

        }
    }


    static final private String DEVICEID = "p000";
    private String createFixData() {
        String formattedData;
        int decLat = (int) lastLatitude;
        double minLat = new BigDecimal( (lastLatitude - decLat) * 60 ).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue(); //minute part of latitude
        int decLng = (int) lastLongitude;
        double minLng = new BigDecimal( (lastLongitude - decLng) * 60 ).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue(); //minute part of longitude

        DecimalFormat zeroPadding = new DecimalFormat("00.0000");

        String lat = String.valueOf(decLat) + String.valueOf( zeroPadding.format(minLat));
        String lng = String.valueOf(decLng) + String.valueOf(zeroPadding.format(minLng));

        //Now the format of lat and longitude will be
        //ddmm.mmmmN and dddmm.mmmmE
        lat += (decLat >= 0)? 'N' : 'S';
        lng += (decLng >= 0)? 'E' : 'W';

        //produce the format of current date and UTC time which match the GPS module produced.
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat("ddMMyy");
        SimpleDateFormat time = new SimpleDateFormat("HHmmss", Locale.UK); //24hr format
        time.setTimeZone(TimeZone.getTimeZone("GMT"));

        formattedData = DEVICEID + ", "
                + date.format(cal.getTime()) + ", "
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
            String postDataFormat = "p000, 300315, 065812.000, 2259.8259N, 12013.3452E";

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
                Log.d("THREAD", "thread runDoHttpPOST working!!");
                if (locationChange) {
                    locationChange = false;
                    String resp;
                    resp = doPOST( createFixData() );

                    /* ---- Package the message for sending to MainActivity ---- */
                    Bundle msgBundle = new Bundle();
                    //position fix
                    msgBundle.putString(INTENT_LATITUDE, Double.toString(lastLatitude));
                    msgBundle.putString(INTENT_LONGITUDE, Double.toString(lastLongitude));

                    //Get the current time
                    Calendar rightNow = Calendar.getInstance();
                    msgBundle.putString(INTENT_CURRENT_TIME, rightNow.getTime().toString());

                    //Send the Http POST response to the UI Thread
                    msgBundle.putString(INTENT_HTTP_RESP, resp);

                    //send data bundle to MainActivity
                    sendResultToMainActivity(LOCATION_RESULT, LOCATION_MESSAGE, msgBundle);

                    /* ----------------------------------------------------------- */
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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


    private void setServiceForeground() {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        builder.setContentIntent(pendingIntent)
                .setContentTitle("Tracking...")
                .setSmallIcon(R.drawable.ic_launcher);

        startForeground(1, builder.build());
    }

    // ------- Life Cycle method --------------

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "service onCreate() executed");
        locationInitial();

        tDoHttpPOST = new Thread(runDoHttpPOST);
        tPOST_flag = true;
        tDoHttpPOST.start();

        setServiceForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand() executed");

        lm.requestLocationUpdates(bestProvider, 1000, 2, this);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service onDestroy() executed");

        tPOST_flag = stopThread(tDoHttpPOST);
        tDoHttpPOST = null;

        lm.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /* ---- LocationListener's methods ---- */

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
