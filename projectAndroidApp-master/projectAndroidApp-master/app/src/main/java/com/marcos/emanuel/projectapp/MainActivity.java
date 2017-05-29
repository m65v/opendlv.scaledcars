package com.marcos.emanuel.projectapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Properties;

/**
 * ProjectAp - A simple android application to interface with OpenDaVINCI scaledcars
 * Version: 1.0
 * Authos: Emanuel Marcos
 * <p>
 * License: BSD
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <p>
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * <p>
 * Neither the name of the (AUTHOR/ORGANIZATION) nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 */

public class MainActivity extends AppCompatActivity {
    private String command, ip, password, user, output;
    private SharedPreferences toSend;
    private SharedPreferences.Editor editor;
    private Button stop, park, videostream;
    private boolean stop_ = false, parked = false, streaming = false;
    private EditText IP, input, userText, passText;
    private TextView shell;
    private UdpServerThread udpServerThread;
    private ProgressDialog pDialog;
    private ImageView image;
    private DatagramSocket socket;
    private BitmapFactory.Options options;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View mainView = findViewById(R.id.activity_main);
        View root = mainView.getRootView();
        root.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().hide();

        stop = (Button) findViewById(R.id.Stop);
        park = (Button) findViewById(R.id.Parker);
        stop.setBackgroundColor(Color.GREEN);
        park.setText("Park");
        toSend = getSharedPreferences("myPreferences", 0);
        IP = (EditText) findViewById(R.id.IPedit);
        input = (EditText) findViewById(R.id.sshText);
        videostream = (Button) findViewById(R.id.Stream);
        videostream.setText("Video Stream");
        shell = (TextView) findViewById(R.id.shell);

        userText = (EditText) findViewById(R.id.userText);
        passText = (EditText) findViewById(R.id.passText);
        shell.setMovementMethod(new ScrollingMovementMethod());

        image = (ImageView) findViewById(R.id.imageView);

    }

    public void onClick(View v) {

        editor = toSend.edit();
        ip = IP.getText().toString().trim();
        password = passText.getText().toString().trim();
        user = userText.getText().toString().trim();
        editor.putString("IP", ip);

        switch (v.getId()) {
            case R.id.Stop:
                if (stop_) {
                    stop_ = false;
                    stop.setBackgroundColor(Color.GREEN);
                    stop.setText("Stop");
                    editor.putString("Function", "move");
                } else {
                    stop_ = true;
                    stop.setBackgroundColor(Color.RED);
                    stop.setText("Stopped!");
                    editor.putString("Function", "stop");
                }
                startService(v);
                break;
            case R.id.Overtaker:
                editor.putString("Function", "overtake");
                startService(v);
                break;
            case R.id.Parker:
                if (parked) {
                    parked = false;
                    park.setText("Park");
                    editor.putString("Function", "unpark");
                } else {
                    parked = true;
                    park.setText("Unpark");
                    editor.putString("Function", "park");
                }
                startService(v);
                break;
            case R.id.Connect:
                command = input.getText().toString().trim();
                new ExecuteRemoteCommand().execute();
                break;
            case R.id.Stream:
                if (streaming) {
                    streaming = false;
                    videostream.setText("Video Stream");
                    Stop();
                } else {
                    streaming = true;
                    videostream.setText("Stop Stream");
                    Start();
                }
                break;
            default:
                throw new RuntimeException("Button ID unknown");
        }
        editor.commit();
        stopService(v);
    }

    public void startService(View view) {
        startService(new Intent(getBaseContext(), UDPSender.class));
    }

    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), UDPSender.class));
    }


    protected void Start() {
        udpServerThread = new UdpServerThread(1234);
        udpServerThread.start();
    }

    protected void Stop() {
        if (udpServerThread != null) {
            udpServerThread.setRunning(false);
            udpServerThread = null;
            socket.close();
        }
    }

    private void updateState(final Bitmap img) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                image.setImageBitmap(img);
            }
        });
    }

    private void updatePrompt(final String prompt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shell.setText(prompt);
            }
        });
    }

    private class UdpServerThread extends Thread {

        int serverPort;


        boolean running;

        public UdpServerThread(int serverPort) {
            super();
            this.serverPort = serverPort;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            running = true;
            String TAG = MainActivity.class.getSimpleName();
            try {
                socket = new DatagramSocket(serverPort);

                Log.e(TAG, "UDP Server is running");

                while (running) {
                    byte[] buf = new byte[60000];

                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);     //this code block the program flow

                    buf = packet.getData();

                    final Bitmap new_img = BitmapFactory.decodeByteArray(buf, 0,
                            buf.length);

                    updateState(new_img);
                    updatePrompt("Stream started");
                }

                Log.e(TAG, "UDP Server ended");

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                    Log.e(TAG, "socket.close()");
                }
            }
        }
    }

    /**
     * Async class responsible for ssh
     */
    private class ExecuteRemoteCommand extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Processing command...");
            pDialog.setCancelable(false);
            pDialog.show();
        }


        @Override
        protected String doInBackground(String... params) {
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, ip, 22);
                session.setPassword(password);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();


                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);

                channel.setInputStream(null);

                InputStream in = channel.getInputStream();
                ((ChannelExec) channel).setErrStream(System.err);


                channel.connect();

                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }

                output = total.toString();
                System.out.println(output);

                channel.disconnect();
                session.disconnect();


            } catch (Exception e) {
                e.printStackTrace();
            }

            return output;
        }

        @Override
        protected void onPostExecute(String result) {
            if (pDialog.isShowing())
                pDialog.dismiss();
            shell.setText(output);
        }
    }
}
