package com.omerfaruk.nfc_wifi;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ServiceClient extends Service {
    private static final String TAG = "ServiceClient";

     /*
            KAYNAK : http://android-er.blogspot.com.tr/2014/02/android-sercerclient-example-server.html

                    https://developer.android.com/guide/topics/connectivity/wifip2p.html
     */

     /*
            BU SERVİS SUNUCU CİHAZA VERİ GÖNDERİLMESİ (CLİENT) İŞLEVİNDEN SORUMLUDUR.

      */

    private int port;
    private String dataToSend;
    private ResultReceiver clientResult;
    private WifiP2pDevice targetDevice;
    private WifiP2pInfo wifiInfo;
    Socket clientSocket = null;

    static String receivingData;

    public ServiceClient() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        port = ((Integer) intent.getExtras().get("port")).intValue();
        //targetDevice = (WifiP2pDevice) intent.getExtras().get("targetDevice");
        wifiInfo = (WifiP2pInfo) intent.getExtras().get("wifiInfo");
        clientResult = (ResultReceiver) intent.getExtras().get("clientResult");
        dataToSend = (String) intent.getExtras().get("dataToSend");
        Thread thread = new Thread(new SocketClientThread());
        thread.start();
        return START_STICKY;
    }

    private class SocketClientThread extends Thread{
        @Override
        public void run() {
            super.run();
            InetAddress hostIP = getWifiInfo().groupOwnerAddress;
            Log.d(TAG, "##########\nrun: Client Service Veri Gönderim Thread İşlemi Başladı\n##########");
            try {
                clientSocket = new Socket(hostIP,getPort());
                while (true){
                    OutputStream os = clientSocket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);
                    String number = getDataToSend();

                    String sendMessage = number + "\n";
                    bw.write(sendMessage);
                    bw.flush();
                    Log.d(TAG, "#########\nClient Server'a veriyi gönderdi\n##########");
//                    System.out.println("#########\nClient Server'a veriyi gönderdi\n##########");

                    InputStream is = clientSocket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    String message = br.readLine();
                    Log.d(TAG, "##########\nServer tarafından gönderilen veri Client Cihaza ulaştı\n########");
                    Log.d(TAG, "##########\nServerdan gelen veri: " + message + "\n##########");
//                    System.out.println("##########\nServer tarafından gönderilen veri Client Cihaza ulaştı\n########");
//                    System.out.println("##########\nServerdan gelen veri: " + message + "\n##########");
                    setReceivingData(message);
                    getClientResult().send(getPort(), null);
                    if (MainActivityWifi.isStopButtonClicked()){
                        try {
                            clientSocket.close();
                            Log.d(TAG, "##########\nrun: Client Soketi Kapatıldı\n#########");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public int getPort() {
        return port;
    }

    public String getDataToSend() {
        return dataToSend;
    }

    public ResultReceiver getClientResult() {
        return clientResult;
    }
/*
    public WifiP2pDevice getTargetDevice() {
        return targetDevice;
    }
*/
    public WifiP2pInfo getWifiInfo() {
        return wifiInfo;
    }

    public static String getReceivingData() {
        return receivingData;
    }

    public static void setReceivingData(String receivingData) {
        ServiceClient.receivingData = receivingData;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
