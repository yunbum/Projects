package com.example.ybbaek.gyrotcpsocket;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Delayed;

public class MainActivity extends AppCompatActivity {

    TextView xVal, yVal, zVal, tv_pitch, tv_pitch2, tv_roll,tv_roll2, tv_yaw;
    String str_xVal, str_yVal, str_zVal, str_pitch, str_roll, str_yaw;

    TextView recieveText, tv_dt;
    EditText editTextAddress, messageText;
    Button connectBtn, clearBtn, closeBtn;
    Button btn_go, btn_back, btn_left, btn_right;

    Socket socket = null;

    private SensorManager sensorManager = null;
    private UserSensorListner userSensorListner;
    private Sensor gyroscopeSensor = null;
    private Sensor accelerometer = null;
    //private SensorEventListener gyroscopeEventListener;

    /*Sensor variables*/
    private float[] mGyroValues = new float[3];
    private float[] mAccValues = new float[3];
    private double mAccPitch, mAccRoll, mAccYaw;

    //private double RAD2DGR = 180 / Math.PI;
    private float a = 0.2f;
    private static final float NS2S = 1.0f/1000000000.0f;
    private double pitch=0, pitch2 =0;
    private double roll=0, roll2;
    private double yaw=0;

    private double timestamp;
    private double dt;
    private double temp;
    private double scale_speed=1, scale_steer=1;
    private boolean running;
    private boolean gyroRunning;
    private boolean accRunning;

    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis();
    long tcpTime=0;

    SeekBar sb_scale, sb_steer;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setElevation(0);

        connectBtn = findViewById(R.id.btn_conn);
        clearBtn = findViewById(R.id.btn_clear);
        closeBtn = findViewById(R.id.btn_close);

        editTextAddress = findViewById(R.id.edtxt_ip);
        recieveText = findViewById(R.id.txtview_receive);
        messageText = findViewById(R.id.edtxt_message);
        tv_dt = findViewById(R.id.tv_dt);


        btn_go = findViewById(R.id.btn_go);
        btn_back = findViewById(R.id.btn_back);
        btn_left = findViewById(R.id.btn_left);
        btn_right = findViewById(R.id.btn_right);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        userSensorListner = new UserSensorListner();
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //sensorManager.SENSOR_DELAY_NORMAL;

        sensorManager.registerListener(userSensorListner, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(userSensorListner, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        tv_pitch = findViewById(R.id.tv_pitch);
        tv_pitch2 = findViewById(R.id.tv_pitch2);
        tv_roll = findViewById(R.id.tv_roll);
        tv_roll2 = findViewById(R.id.tv_roll2);
        tv_yaw = findViewById(R.id.tv_yaw);

        sb_scale = findViewById(R.id.sb_scale);
        sb_steer = findViewById(R.id.sb_steer);

        progressBar = findViewById(R.id.progressBar);

        sb_scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scale_speed = (float)progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sb_steer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scale_steer = (float)progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        if (gyroscopeSensor == null){
            Toast.makeText(this, "No Gyroscope", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            Toast.makeText(this, "Gyroscope detected", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* 실행 중이지 않을 때 -> 실행 */
                if(!running){
                    running = true;
                    sensorManager.registerListener(userSensorListner, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    sensorManager.registerListener(userSensorListner, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

                    /*
                    빠른 순서로
                        1.SENSOR_DELAY_FASTEST   0 ms 최대한 빠르게
                        2.SENSOR_DELAY_GAME       20,000ms 게임에 적합한 속도
                        3.SENSOR_DELAY_UI             60,000ms UI 수정에 적합한 속도
                        4.SENSOR_DELAY_NORMAL   200,000ms 화면 방향 변화를 모니터링하기에 적합한 속도
                     */

                }

                /* 실행 중일 때 -> 중지 */
                else if(running)
                {
                    running = false;
                    sensorManager.unregisterListener(userSensorListner);
                }
            }
        });


        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,messageText.getText().toString());
                myClientTask.execute();
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recieveText.setText("");
                messageText.setText("");
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,"C");
                myClientTask.execute();
            }
        });

        btn_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,"HG_100T");
                myClientTask.execute();
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,"HB_100T");
                myClientTask.execute();
            }
        });

        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,"HL_200T");
                myClientTask.execute();
            }
        });

        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(),7777,"HR_200T");
                myClientTask.execute();
            }
        });
    }


    private void complementaty(double new_ts){

        /* 자이로랑 가속 해제 */
        gyroRunning = false;
        accRunning = false;

        /*센서 값 첫 출력시 dt(=timestamp - event.timestamp)에 오차가 생기므로 처음엔 break */
        if(timestamp == 0){
            timestamp = new_ts;
            return;
        }
        dt = (new_ts - timestamp) * NS2S; // ns->s 변환
        timestamp = new_ts;

        /* degree measure for accelerometer */
        mAccPitch = -Math.atan2(mAccValues[0], mAccValues[2]) * 180.0 / Math.PI; // Y 축 기준
        mAccRoll = Math.atan2(mAccValues[1], mAccValues[2]) * 180.0 / Math.PI; // X 축 기준

        /**
         * 1st complementary filter.
         *  mGyroValuess : 각속도 성분.
         *  mAccPitch : 가속도계를 통해 얻어낸 회전각.
         */

        temp = (1/a) * (mAccRoll - roll) + mGyroValues[0];
        roll = roll + (temp*dt);

        temp = (1/a) * (mAccPitch - pitch) + mGyroValues[1];
        pitch = pitch + (temp*dt);

        pitch2 = (pitch +45) * scale_speed;
        roll2 = (roll) * scale_steer;


        if (tcpTime >1000) {


            if (pitch2 < 0) {
                pitch2 = pitch2 * (-1);
                if (pitch2 > 250){
                    pitch2 = 250;
                }

                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(), 7777, "HB_" + String.valueOf((int) pitch2) + "T");
                myClientTask.execute();

            } else {
                if (pitch2 > 250){
                    pitch2 = 250;
                }

                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(), 7777, "HG_" + String.valueOf((int) pitch2) + "T");
                myClientTask.execute();

            }

            if (roll2 < 0) {
                roll2 = roll2 * (-1);
                if (roll2 > 250){
                    roll2 = 250;
                }
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(), 7777, "HL_" + String.valueOf((int) roll2) + "T");
                myClientTask.execute();
            } else {
                if (roll2 > 250){
                    roll2 = 250;
                }
                MyClientTask myClientTask = new MyClientTask(editTextAddress.getText().toString(), 7777, "HR_" + String.valueOf((int) roll2) + "T");
                myClientTask.execute();

            }

            endTime = System.currentTimeMillis();

            progressBar.setProgress((int) pitch2);

        }

        //temp = (1/a) * (mAccYaw - yaw) + mGyroValues[2];
        //yaw = yaw + (temp*dt);

        tv_roll.setText("roll1: " + String.format("%3.01f", roll));
        if (roll2 <0){
            roll2 = roll2 * (-1.0);
        }

        tv_roll2.setText("roll2: " + String.format("%3.01f", roll2));
        tv_pitch.setText("pitch1: "+ String.format("%3.01f",pitch));

        if (pitch2 <0){
            pitch2 = pitch2 *(-1.0);
        }
        tv_pitch2.setText("pitch2: " + String.format("%3.01f", pitch2));
    }

    public class MyClientTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;
        String response = "";
        String myMessage = "";

        //constructor
        MyClientTask(String addr, int port, String message){
            dstAddress = addr;
            dstPort = port;
            myMessage = message;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;
            myMessage = myMessage.toString();
            try {
                socket = new Socket(dstAddress, dstPort);
                //송신
                OutputStream out = socket.getOutputStream();
                out.write(myMessage.getBytes());

                //수신
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inputStream = socket.getInputStream();
                /*
                 * notice:
                 * inputStream.read() will block if no data return
                 */
                while ((bytesRead = inputStream.read(buffer)) != -1){
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }
                response = "Pi ack: " + response;

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            recieveText.setText(response);
            super.onPostExecute(result);
        }
    }


    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(userSensorListner, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(userSensorListner, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(userSensorListner);
    }



    public class UserSensorListner implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()){

                /** GYROSCOPE */
                case Sensor.TYPE_GYROSCOPE:

                    /*센서 값을 mGyroValues에 저장*/
                    mGyroValues = event.values;

                    if(!gyroRunning)
                        gyroRunning = true;

                    break;

                /** ACCELEROMETER */
                case Sensor.TYPE_ACCELEROMETER:

                    /*센서 값을 mAccValues에 저장*/
                    mAccValues = event.values;

                    if(!accRunning)
                        accRunning = true;

                    break;
            }

            /**두 센서 새로운 값을 받으면 상보필터 적용*/
            if(gyroRunning && accRunning){

                startTime = System.currentTimeMillis();
                tcpTime = startTime - endTime;

                tv_dt.setText("dt: " + String.valueOf(tcpTime));

                complementaty(event.timestamp);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    }

}
