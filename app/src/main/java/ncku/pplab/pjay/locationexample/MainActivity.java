package ncku.pplab.pjay.locationexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private TextView sharpPText;
    private TextView debugText;
    private LinearLayout baseLinearLayout;

    private Button startButton, stopButton;

    //private boolean getService = false;
    private SQLite dbHelper;

    private void testLocationProvider() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            //getService = true;
            startButton.setClickable(true);
//            locationInitial();
        }else{
            Toast.makeText(this, "Please open GPS and Network for location", Toast.LENGTH_SHORT).show();
            //TO-DO finish bellow function
            //startActivities(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        startButton.setClickable(false);
        stopButton.setClickable(false);
    }


    BroadcastReceiver receiver;

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

        //receive message from LocationService and show them on UI
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle meg = intent.getBundleExtra(LocationService.LOCATION_MESSAGE);
                latitudeText.setText(meg.getString(LocationService.INTENT_LATITUDE));
                longitudeText.setText(meg.getString(LocationService.INTENT_LONGITUDE));
                timeText.setText(meg.getString(LocationService.INTENT_CURRENT_TIME));
                String resp = meg.getString(LocationService.INTENT_HTTP_RESP);
                debugText.setText(debugText.getText() + "\n" + resp + "\n");
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart executed");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(LocationService.LOCATION_RESULT)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        tWD_Flag = stopThread(tWanderingDetection);
        tWanderingDetection = null;

        dbHelper.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                stopButton.setClickable(true);
                startButton.setClickable(false);
                Intent startIntent = new Intent(this, LocationService.class);
                startService(startIntent);
                break;
            case R.id.stopButton:
                stopButton.setClickable(false);
                startButton.setClickable(true);
                Intent stopIntent = new Intent(this, LocationService.class);
                stopService(stopIntent);
                break;
            default:break;
        }
    }
}
