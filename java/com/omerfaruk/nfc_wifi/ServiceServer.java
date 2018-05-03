package com.omerfaruk.nfc_wifi;

import android.app.Service;
import android.content.Intent;
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceServer extends Service {
    private static final String TAG = "ServiceServer";

    /*
            KAYNAK : http://android-er.blogspot.com.tr/2014/02/android-sercerclient-example-server.html

                    https://developer.android.com/guide/topics/connectivity/wifip2p.html
     */

    /*

            *   BU SERVİS HİZMETİ GELEN VERİLERİN ALINIP (SUNUCU) TEKRAR CLİENT CİHAZA GÖNDERİLMESİ İŞLEVİNDEN SORUMLUDUR.
     */


    private int port;
    private ResultReceiver serverResult;
    public static String displayData;
    ServerSocket serverSocket = null;
    Socket socket = null;

    public ServiceServer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        port = ((Integer) intent.getExtras().get("port")).intValue();
        serverResult = (ResultReceiver) intent.getExtras().get("serverResult");
        Log.d(TAG, "###########\nonStartCommand: " + "Server port :" +port+" üzerinden dinliyor.\n###########");
//        System.out.println("###########\nServer port :" +port+" üzerinden dinliyor.\n###########");
        Thread thread = new Thread(new SocketServerThread());
        thread.start();
        return START_STICKY;
    }

    private class SocketServerThread extends Thread{
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(getPort()));
                serverSocket.setReuseAddress(true);

                while (true){
                    socket = serverSocket.accept();
                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    String number = br.readLine();
                    Log.d(TAG, "###########\nClient cihazdan veri alındı : " + number + "\n############");
                    setDisplayData(number);

                    Thread thread2 = new Thread(new SocketServerReplyThread(number,socket));
                    thread2.start();
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class SocketServerReplyThread extends Thread{
        //constructor vs
        String number;
        String returnMessage;
        Socket socket;

        public SocketServerReplyThread(String number, Socket socket) {
            this.number = number;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                int numberIntFormat = Integer.parseInt(number);
                int returnValue = numberIntFormat * 2;
                returnMessage = String.valueOf(returnValue) + "\n";

                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(returnMessage);
                Log.d(TAG, "###########\nVeri serverden client cihaza gönderildi: " + returnMessage + "\n##########");
//                System.out.println("###########\nVeri serverden client cihaza gönderildi: " + returnMessage + "\n##########");
                bw.flush();
                getServerResult().send(getPort(), null);
                if (MainActivityWifi.isStopButtonClicked()){
                    try {
                        serverSocket.close();
                        Log.d(TAG, "#########\nrun: Server Soketi Kapatıldı\n#########");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //region GET-SET

    public int getPort() {
        return port;
    }

    public ResultReceiver getServerResult() {
        return serverResult;
    }

    public static String getDisplayData() {
        return displayData;
    }

    public static void setDisplayData(String displayData) {
        ServiceServer.displayData = displayData;
    }

    //endregion


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
