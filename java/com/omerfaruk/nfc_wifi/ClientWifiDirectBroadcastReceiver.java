package com.omerfaruk.nfc_wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by asd on 11.4.2018.
 *              ########## ÖNEMLİ ##########
 *  BU SINIF TÜM WİFİ-DİRECT UYGULAMALARINDA OLMASI GEREKEN BROADCASRECEİVER'I İÇERİYOR. ÖZEL BİR EKLENTİSİ YOKTUR, ANDROİD-DEVELOPER 'DA ANLATILDIĞI KADARIYLA OLUŞTURULMUŞTUR.
 *
 */

public class ClientWifiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ClientWifiBroadcast";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivityWifi activity;
    private static WifiP2pDeviceList deviceList;

    public ClientWifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivityWifi activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;

        activity.setClientStatus("Client Broadcast Receiver oluşturuldu.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                activity.setClientWifiStatus("Wifi Direct Etkin");
            }else {
                activity.setClientWifiStatus("Wifi Direct Devre Dışı");
            }

        }else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                    activity.displayPeers(wifiP2pDeviceList);
                    setDeviceList(wifiP2pDeviceList);
                    System.out.println(wifiP2pDeviceList);
                }
            });
        }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo wifiInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

            WifiP2pConfig config = null;
            config = new WifiP2pConfig();
            Log.d(TAG, "#########\nGROUP OWNER IP ADRESİ : " + wifiInfo.groupOwnerAddress+"\n"+"\n##########");
//            System.out.println("#########\nGROUP OWNER IP ADRESİ : " + wifiInfo.groupOwnerAddress+"\n"+"\n##########");
//            System.out.println(config.deviceAddress); wifi ağında olan direct özelliğine sahip tüm cihazların detalı listesini döndürür. -> deviceAddress methodu

            if (networkInfo.isConnected()){
                activity.setClientStatus("Bağlantı Durumu: Bağlandı");
                activity.isGroupOwner(wifiInfo);
                activity.setConnected(true);
            }else {
                manager.cancelConnect(channel,null);
                activity.setClientStatus("Bağlantı Durumu: Henüz Bir Bağlantı Sağlanamadı");
            }

        }else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){

        }
    }

    //region GET-SET

    public static WifiP2pDeviceList getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(WifiP2pDeviceList deviceList) {
        this.deviceList = deviceList;
    }
    //endregion
}
