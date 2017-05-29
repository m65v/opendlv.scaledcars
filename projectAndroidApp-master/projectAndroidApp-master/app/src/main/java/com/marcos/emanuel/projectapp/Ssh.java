package com.marcos.emanuel.projectapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @author Emanuel on 05/05/2017.
 *
 *
 * This class is no longer in use
 */

public class Ssh extends Activity {
    private String command, username, password, ip;
    private int PORT = 22;
    private String output;
    private MainActivity main;
    private ProgressDialog pDialog;


    public Ssh(String ip, String command, String password, String user, MainActivity main){
        this.ip = ip;
        this.command = command;
        this.password = password;
        this.username = user;  //change user if needed
        this.main = main;
        ExecuteRemoteCommand ex = new ExecuteRemoteCommand();
        ex.execute();
    }


    private class ExecuteRemoteCommand extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(main);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }
        TextView shell = (TextView) findViewById(R.id.shell);

        @Override
        protected String doInBackground(String... params) {
                try {
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(username, ip, PORT);
                    session.setPassword(password);

                    Properties prop = new Properties();
                    prop.put("StrictHostKeyChecking", "no");
                    session.setConfig(prop);

                    session.connect();


                    Channel channel=session.openChannel("exec");
                    ((ChannelExec)channel).setCommand(command);

                    channel.setInputStream(null);

                    InputStream in=channel.getInputStream();
                    ((ChannelExec)channel).setErrStream(System.err);


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


            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            shell.setText(result);
           // txtH.setText(result.getAtmosphereHumidity());
        }



    }





 }
