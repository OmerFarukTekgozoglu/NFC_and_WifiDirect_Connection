package com.omerfaruk.nfc_wifi;

import android.app.Activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;

import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;

import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.app.NavUtils;
import android.os.Bundle;

import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivityWifi extends Activity implements NfcAdapter.CreateNdefMessageCallback{
    /*

        * BU ACTİVİTY'DE WİFİ DİRECT CONN. İÇİN OLUŞTURULMUŞTUR AYRICA İÇERİSİNDE NFC İLE CİHAZLARIN IPV4 ADRESLERİNİ BİRBİRLERİNE GÖNDERDİKLERİ BİR KISIM DAHA VARDIR.
        * IPV4 ADRESLERİ İLE HEDEFLENEN WİFİ-DİRECT İÇİN SERVİCECLİENT SERVİCE SINIFINDAKİ TARGET-IP KISMINI GENERİC OLARAK AYARLAMAKTI. ANCAK WİFİ-DİRECT'DE KULLANILAN
        * IP ADRESİ CİHAZLARIN KENDİ IPV4 ADRESLERİ DEĞİLDİR.
        * BUNU ANLAMAK İÇİN LG-G3 CİHAZDA
        * Wİ-Fİ SETTİNG (sağdaki 3 noktalı yerden) --> GELİŞMİŞ WİFİ  --> Wİ-Fİ DİRECT (sağdaki 3 noktalı yerden) --> GELİŞMİŞ --> IP ADRESİ GÖRÜLMEKTEDİR. DİKKAT NORMAL IPV4 ADRESİNDEN FARKLIDIR!

        ONEMLİ NOT
        *Bu kod parçası github da bulunan WiFiDirectFileTransfer dan esinlenerek hazırlanmıştır. Eğer silinmediyse Bilgisayarın Download klasöründe bulunmaktadır.
        *
        * WARNING : Wifi-Direct Request Peers override methodu MAC adreslerinin ilk kısmını yanlış döndürüyor.
        * Örneğin LG-G3'ün MAC Adresi 34:4d.... şeklinde iken söz konusu request methodu 36:4d.... şeklinde geri
        * dönüş sağlamaktadır. Bu sorun ilerde sadece iki cihazın bağlantı kurması ile ilgili problem oluşturacaktır. Çünkü Main Activity de bulunan
        * getMacAddr methodu cihazın gerçek MAC adresini vermektedir. Bu durum Main Activity ' de getMacAddr methodunun düzeltilmesi ile giderilmiştir.
        * Wifi-Direct'de kullanılan adres ipv6 adresine benzemektedir.
        *
     */


    private static final String MIME_TYPE = "*/*"; //Tüm Tipleri Kapsadığı anlamına geliyor. (MIME type hatası oluşursa, text/plain daha önce denendi çalışıyor.) NOT : */* BU DA DÜZGÜN ÇALIŞIYOR. BURADAN HATA OLUŞMAZ.

    private static final String TAG = "MainActivityWifi";

    EditText etSendNumber;
    TextView tvDisplayReceivingData;

    public final int port = 7950;

    boolean isIPSend;

    private static String otherDeviceIP;

    private static String MAC;

    private WifiP2pManager wifiManager;
    private WifiP2pManager.Channel wifiChannel;
    private BroadcastReceiver wifiClientReceiver;

    private IntentFilter wifiClientReceiverIntentFilter;

    private NfcAdapter nfcAdapter;
    private WifiP2pDevice targetDevice;

    private Intent serviceServerIntent;

    private Intent serviceClientIntent;

    private IntentFilter[] intentFiltersArray;

    private String[][] techListArray;

    private PendingIntent pendingIntent;

    public boolean isConnected;

    private WifiP2pInfo wifiInfo;

    public static boolean isStopButtonClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wifi);

        wifiManager = (WifiP2pManager) getApplicationContext().getSystemService(WIFI_P2P_SERVICE);

        wifiChannel = wifiManager.initialize(this,getMainLooper(),null);
        wifiClientReceiver = new ClientWifiDirectBroadcastReceiver(wifiManager,wifiChannel,this);

        wifiClientReceiverIntentFilter = new IntentFilter();
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        registerReceiver(wifiClientReceiver,wifiClientReceiverIntentFilter);

        isIPSend = false;

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        nfcAdapter.setNdefPushMessageCallback(this,this);

        wifiManager.discoverPeers(wifiChannel,null);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        setStopButtonClicked(false);

        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};
        techListArray = new String[][]{new String[]{NfcF.class.getName()}};


    }

    //region MENU-OPTİONS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_wifi,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region STOP-RECEİVER
    private void stopClientReceiver(){
        try {
            unregisterReceiver(wifiClientReceiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }
    //endregion

    //region VERİ GONDER-AL

    public void receiveFile(View view){

        serviceServerIntent = new Intent(this, ServiceServer.class);
        serviceServerIntent.putExtra("port",port);
        serviceServerIntent.putExtra("serverResult", new ResultReceiver(null){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == port){
                    if (resultData == null){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "###########\nrun: Receive File Log : " + ServiceServer.getDisplayData() + " verisini aldı");
                                tvDisplayReceivingData = findViewById(R.id.tvDisplayReceivingData);
                                tvDisplayReceivingData.setText(ServiceServer.getDisplayData());
                            }
                        });
                    }
                }
            }
        });
        this.startService(serviceServerIntent);
    }


    public void sendFile(View view) {

        etSendNumber = findViewById(R.id.etSendNumber);
        String dataToSend = etSendNumber.getText().toString();

        serviceClientIntent = new Intent(this, ServiceClient.class);
        serviceClientIntent.putExtra("dataToSend", dataToSend);
        serviceClientIntent.putExtra("port", new Integer(port));
        //serviceClientIntent.putExtra("targetDevice", targetDevice);
        serviceClientIntent.putExtra("wifiInfo", wifiInfo);
        serviceClientIntent.putExtra("clientResult",new ResultReceiver(null){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "###########\nrun: Send File Log : " + ServiceClient.getReceivingData() + " verisini gönderdi.\n###########");
                        tvDisplayReceivingData = findViewById(R.id.tvDisplayReceivingData);
                        tvDisplayReceivingData.setText(ServiceClient.getReceivingData());
                    }
                });
            }
        });
        this.startService(serviceClientIntent);
    }

//endregion

    //region Stop-Button
    public void btnStop(View view){
        setStopButtonClicked(true);
    }
    //endregion

    //region Life Cycle  Metotları
    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListArray);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            stopClientReceiver();
            unregisterReceiver(wifiClientReceiver);
        }catch (Exception e){
            //Do nothing!
        }

    }
    //endregion

    //region TÜM TEXTVİEW VE LİSTVİEW
    public void setClientWifiStatus(String message){
        TextView tvConnectionStatus = findViewById(R.id.tvClientWifiStatus);
        tvConnectionStatus.setText(message);
    }


    public void setClientStatus(String message){
        TextView tvClientStatus = findViewById(R.id.tvClientStatus);
        tvClientStatus.setText(message);
    }

    public void displayPeers(final WifiP2pDeviceList peers){
        ListView peerView = findViewById(R.id.lvPeers);

        ArrayList<String> peerStringList = new ArrayList<String>();

        for (WifiP2pDevice wd : peers.getDeviceList()){
            peerStringList.add(wd.deviceName);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, peerStringList);
        peerView.setAdapter(adapter);
    }
    //endregion

    //region CONNECT PEER
    public void connectToPeer(final WifiP2pDevice wifiPeer){

        this.targetDevice = wifiPeer;

        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = targetDevice.deviceAddress;
        wifiManager.connect(wifiChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                setClientStatus(config.deviceAddress + " Cihazına Bağlantı Başarılı");
            }

            @Override
            public void onFailure(int i) {
                setClientStatus(config.deviceAddress + " Cihazına Bağlantı Başarısız");
            }
        });
    }
    public void connectionWifi(){
        WifiP2pDevice device = null;
        String address = getMAC();
        device = ClientWifiDirectBroadcastReceiver.getDeviceList().get(address); //36:4d:f7:5a:a6:c3 Wi-Fi direct Adresi G3 Cihazına ait, G3'ün Mac Adresi 34:4d:f7:5a:a6:c3 ilk satır bloğunun değiştiğine dikkat edin!
        if (device != null){
            connectToPeer(device);
        }else {
            setClientStatus("Bağlantı Kurulan Cihaz Bulunamıyor");
        }
    }
    //endregion

    //region IP Adresinin Gönderildiği Kısım Burası Ancak Bu IP adresi Wi-Fİ Direct'te kullanılamıyor.
    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String message = getDeviceIP();
        if (message != null){
            Log.d(TAG, "#########\ncreateNdefMessage: Gönderilen IP adresi --> " + message + "\n#########");
            NdefRecord ndefRecord = NdefRecord.createMime(MIME_TYPE, message.getBytes());
            NdefMessage ndefMessage = new NdefMessage(ndefRecord);
            return ndefMessage;
        }else{
            return null;
        }
    }
    //endregion

    //region Handle NFC Intent
    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0];

            setOtherDeviceIP(new String(message.getRecords()[0].getPayload()));
            Log.d(TAG, "##########\nonNewIntent: Soket Haberleşmesi için diğer cihazın IP adresi ulaştı: " + getOtherDeviceIP() + "\n#########");
        }
        if (!isConnected()){
            connectionWifi();
        }

    }
    //endregion

    //region GET-SET Metotları

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public static String getMAC() {
        return MAC;
    }

    public static void setMAC(String MAC) {
        MainActivityWifi.MAC = MAC;
    }

    public String getDeviceIP() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String messageIP;

        if (manager != null & manager.isWifiEnabled()){
            messageIP = Formatter.formatIpAddress(manager.getConnectionInfo().getIpAddress());
        }else {
            messageIP = null;
        }
        return messageIP;
    }

    public static String getOtherDeviceIP() {
        return otherDeviceIP;
    }

    public static void setOtherDeviceIP(String otherDeviceIP) {
        MainActivityWifi.otherDeviceIP = otherDeviceIP;
    }

    public void isGroupOwner(WifiP2pInfo info){
        this.wifiInfo = info;
        if (info.isGroupOwner){
            Toast.makeText(MainActivityWifi.this,"Cihaz GroupOwner : Yalnızca Veri Alabilir",Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isStopButtonClicked() {
        return isStopButtonClicked;
    }

    public void setStopButtonClicked(boolean stopButtonClicked) {
        isStopButtonClicked = stopButtonClicked;
    }

    //endregion
}
