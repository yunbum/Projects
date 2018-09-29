package com.example.ybbaek.self_driving;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    EditText edtxt_ip, edtxt_port, edtxt_cmd;

    SeekBar sb_speed, sb_steer, sb_test;
    ProgressBar progressBar;

    public String sIP = "192.168.1.100";
    public int sPORT = 8011;

    public SendData mSenddata = null;
    public TextView tv_ack = null;
    //public static final String sIP = "192.168.1.19";
    //public static final int sPORT = 8011;

    private int scale_speed=1, scale_steer=1;

    byte[] buf = (" Wego ERP ini").getBytes();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_send = findViewById(R.id.btn_send);
        Log.v("button ok", "UDP command");

        tv_ack = findViewById(R.id.tv_ack);
        edtxt_ip = findViewById(R.id.edtxt_ip);
        edtxt_port = findViewById(R.id.edtxt_port);
        edtxt_cmd = findViewById(R.id.edtxt_cmd);

        sb_steer = findViewById(R.id.sb_steer);
        sb_speed = findViewById(R.id.sb_speed);

        btn_send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                scale_speed=0;
                scale_steer=0;

                buf = (edtxt_cmd.getText().toString() + "////").getBytes();

                mSenddata = new SendData();
                mSenddata.start();
            }
        });


        sb_steer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scale_steer = (int) progress;
                buf = ("ST_" + String.valueOf(scale_steer) +"_T////").getBytes();

                mSenddata = new SendData();
                mSenddata.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scale_speed = (int) progress;
                buf = ("SP_" + String.valueOf(scale_speed) +"_T////").getBytes();

                mSenddata = new SendData();
                mSenddata.start();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    class SendData extends Thread {
        public void run(){
            try{
                DatagramSocket socket = new DatagramSocket();

                sIP = edtxt_ip.getText().toString();
                InetAddress serverAddr = InetAddress.getByName(sIP);

                String tmp_port;
                tmp_port = edtxt_port.getText().toString();
                sPORT = Integer.parseInt(tmp_port);

                //byte[] buf = ("Wego ERP ini....").getBytes();
                //buf = ("Wego ERP start-up.").getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, sPORT);

                socket.send(packet);
                Log.d("sendmsg.", "송신");

                socket.receive(packet);
                Log.d("getmsg.", "수신");

                String msg = new String(packet.getData());

                tv_ack.setText(msg);
                Log.v("getmsg.", msg);

            }
            catch (Exception e){

            }

        }
    }
}
