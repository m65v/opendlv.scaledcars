package com.marcos.emanuel.projectapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author Emanuel on 04/05/2017.
 */

public class UDPSender extends Service{
    private int PORT = 8888 ;   // change this

    int mStartMode;
    private SharedPreferences toSend;
    private String function, ip;


    @Override
    public void onCreate() {
        toSend = getSharedPreferences("myPreferences",0);
        this.function = toSend.getString("Function", "0");
        this.ip = toSend.getString("IP", "0");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String tosend = encodedNetString(function);
            byte[] message = tosend.getBytes();

            InetAddress address = InetAddress.getByName(ip);

            DatagramPacket packet = new DatagramPacket(message, message.length,
                    address, PORT);

            // Create a datagram socket, send the packet through it, close it.
            DatagramSocket dsocket = new DatagramSocket();
            dsocket.send(packet);
            dsocket.close();

            System.out.println("sending "+ tosend);
            Toast.makeText(this, "Sent",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            System.err.println(e);
        }
        return mStartMode;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    String encodedNetString(String plainInput){
        //returns a NetString in the form of [len]":"[string]","
        return  Integer.toString(plainInput.length()) + ":" + plainInput + ",";
    }



}
