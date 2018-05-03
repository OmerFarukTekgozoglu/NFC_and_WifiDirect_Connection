package com.omerfaruk.nfc_wifi;


import android.app.Activity;
import android.app.PendingIntent;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.net.wifi.WifiManager;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;

import java.util.Collections;

import java.util.List;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback {

    /*
            MAİN CLASS İÇERİSİNDE FOTOĞRAF PAYLAŞIMINDA KULLANILAN NFC - BEAM İLE WİFİ DİRECT İÇİN GEREKLİ CİHAZ ADRESİ GİBİ BAZI METHODLAR BULUNMAKTADIR.
     */

    private NfcAdapter mNfcAdapter;

    public static final String TAG = "MainActivity";

    private static final String MIME_TYPE = "*/*"; //Tüm Tipleri Kapsadığı anlamına geliyor. (MIME type hatası oluşursa, text/plain daha önce denendi çalışıyor.) NOT : */* BU DA DÜZGÜN ÇALIŞIYOR. BURADAN HATA OLUŞMAZ.

    private IntentFilter[] intentFiltersArray;

    private String[][] techListArray;

    private PendingIntent pendingIntent;

    TextView tvMyTextView;

    String otherDeviceMAC;

    private static final int REQUEST_CODE = 19; //dummy number HERHANGİ BİR RAKAM OLABİLİR BİR ÖNEMİ YOK. ANCAK KULLANILIYOR!
    private Uri sendingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        tvMyTextView = (TextView) findViewById(R.id.tvMyTextView);

        PackageManager pm = this.getPackageManager();

        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //WİFİ Etkinliğinin Kontrolü
        if (mWifiManager.isWifiEnabled()){
            Toast.makeText(this,"WiFi Etkin",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"WiFi Etkin Değil, Lütfen Etkinleştiriniz.",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        //Cihazın Direct özelliği kontrolü
        if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Toast.makeText(this, "Bu cihazda WiFi Direct Özelliği Mevcut", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Bu cihazda WiFi Direct Özelliği Mevcut Değildir", Toast.LENGTH_LONG).show();
        }
        // Cihazdaki NFC Özelliği kontrolü
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, "Cihazda NFC bulunmuyor", Toast.LENGTH_SHORT).show();
            return;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Toast.makeText(this, "Android Beam bu cihazda desteklenmiyor", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Android Beam - NFC kullanılabilir", Toast.LENGTH_SHORT).show();

            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }

        //NFC ' nin Kontrolü
        if (mNfcAdapter.isEnabled()){
            Toast.makeText(this,"NFC Etkin",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"NFC Özelliğini Etkinleştiriniz",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        mNfcAdapter.setNdefPushMessageCallback(this, this);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};
        techListArray = new String[][]{new String[]{NfcF.class.getName()}};

    }

    //region LİFE-CYCLE METHODS

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListArray);
    }

    //endregion

    //region TÜM BUTONLAR

    public void btnDisplayFilesDir(View view){
        Intent intent = new Intent(MainActivity.this, DisplayFilesDirectory.class);
        startActivity(intent);
    }

    public void btnBrowse(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("*/*");

        startActivityForResult(intent, REQUEST_CODE);
    }

    public void btnSendPicture(View view) {
        mNfcAdapter.setBeamPushUris(new Uri[]{getSendingData()}, this);
    }

    public void btnWifi(View view) {
        Intent intent = new Intent(this, MainActivityWifi.class);
        startActivity(intent);
    }

    //endregion

    //region TÜM GET-SET METOTLARI


    public String getOtherDeviceMAC() {
        return otherDeviceMAC;
    }

    public void setOtherDeviceMAC(String otherDeviceMAC) {
        this.otherDeviceMAC = otherDeviceMAC;
    }

    public void setSendingData(Uri sendingData) {
        this.sendingData = sendingData;
    }

    public Uri getSendingData() {
        return sendingData;
    }
    //endregion

    //region Karşı Cihaza MAC Adresinin Gönderildiği Kısım

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String message;

        if (manager != null & manager.isWifiEnabled()) {
            message = getMacAddr();
        } else {
            message = null;
        }
        Log.d(TAG, "##########\nWiFi Direct MAC Adresi: " + message + "\n##########");

        NdefRecord ndefRecord = NdefRecord.createMime(MIME_TYPE, message.getBytes());

        NdefMessage ndefMessage = new NdefMessage(ndefRecord);

        return ndefMessage;
    }
    //endregion

    //region INTENT HANDLER VE NFC Fotoğraf için RESULT FOR ACTİVİTY

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "#########\nForeground Dispatch ile gelen intent: " + intent + "\n#########");
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0];

            //Buradaki method önemli silinmemesi gerekiyor.
            setOtherDeviceMAC(new String(message.getRecords()[0].getPayload()));
            MainActivityWifi.setMAC(new String(message.getRecords()[0].getPayload())); //Silinmemeli Wi-Fi Conn. için cihaz adresi set ediliyor.

            tvMyTextView.setText(getOtherDeviceMAC());

        } else {
            tvMyTextView.setText(R.string.NdefBekleme);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                Log.d(TAG, "#########\nUri: " + uri.toString() + "\n#########");
                setSendingData(uri);
            }
        }
    }

    //endregion

    //region Mac Address
    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();

                String interfaceAdresses = nif.getInterfaceAddresses().get(0).toString();
                int indexOfFE80 = interfaceAdresses.indexOf("fe80::");
                int indexOfWlan = interfaceAdresses.indexOf("%wlan");
                interfaceAdresses = interfaceAdresses.substring(indexOfFE80,indexOfWlan);
                String deleteString = "fe80::";
                interfaceAdresses = interfaceAdresses.substring(deleteString.length(),interfaceAdresses.length());

                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                res1.delete(0,2);
                res1.insert(0,interfaceAdresses.substring(0,2),0,2);
//                System.out.println(res1.toString());

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }

                Log.d(TAG, "##########\ngetMacAddr: Cihazın Wi-Fi Direct Adresi -> " + res1.toString() + "\n#########");

                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

    //endregion

}