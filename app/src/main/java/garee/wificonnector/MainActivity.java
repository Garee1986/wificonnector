package garee.wificonnector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;
import android.support.annotation.NonNull;

import garee.wificonnector.R.id;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0;

    // String
    // intentの引数にSSIDが設定されていた場合にここに格納する
    @NonNull String SSID;
    // intentの引数にWIFI_STATEが設定されていた場合にここに格納する
    @NonNull String WIFI_STATE;

    String ToastText;

    // boolean
    // Access Pointに接続した際のサクセスチェック
    boolean AP_SuccessCheck;

    // WifiManager
    WifiManager wm;

    // RecycleView
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private String[] myDataset;

    private List<ScanResult> scanResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.WifiList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView.setLayoutManager(mLayoutManager);

        // 既にpermissionが許可されているか確認
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            return;
        }

        //インテントよりSSIDを取得する
        try{
            SSID = getIntent().getExtras().getString("SSID");
        }catch(NullPointerException e){
            SSID = null;
        }

        //インテントよりWIFI_STATEを取得する
        try{
            WIFI_STATE = getIntent().getExtras().getString("WIFI_STATE");
        }catch(NullPointerException e){
            WIFI_STATE = null;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // AccessPointスキャン結果
        boolean AP_Scan_result = false;

        // Wi-Fiアクセスを管理する WifiManager を取得
        wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        // APをスキャン
        wm.startScan();
        // スキャン結果を取得
        scanResults = wm.getScanResults();

        if((SSID != null)) {

            int WifiState = wm.getWifiState();

            if(WifiState == WifiManager.WIFI_STATE_ENABLED) {

                for(int i=0; i<scanResults.size(); i++) {
                    if(SSID.equals(scanResults.get(i).SSID)){
                        ChangeWifiProcess();
                        AP_Scan_result = true;
                        break;
                    }
                }

                if(!AP_Scan_result) {
                    //スキャン結果と引数が一致しなかった場合の処理
                    ToastText = SSID + "が見つかりませんでした。";
                    Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();
                }


            }else if (WifiState == WifiManager.WIFI_STATE_DISABLED) {
                // WiFiがOFFであった時の処理
                ToastText =  "WiFiがOFFになっています。";
                Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();
            }

        }

        if( WIFI_STATE != null && WIFI_STATE.equals("discon")) {
            DisableWifiProcess();
        }

        List<WifiConfiguration> WifiConfiguration = wm.getConfiguredNetworks();
        myDataset = new String[scanResults.size()];
        for(int i=0; i<myDataset.length; i++) {
            for(int j=0; j<WifiConfiguration.size();j++) {
                if(('"'+scanResults.get(i).SSID+'"').equals(WifiConfiguration.get(j).SSID))
                myDataset[i] = scanResults.get(i).SSID;
            }
        }

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);


    }

    void DisableWifiProcess(){
        WifiInfo wifiInfo = wm.getConnectionInfo();

        boolean succeeded = wm.disableNetwork(wifiInfo.getNetworkId());

        if(succeeded){

            // WiFiがOFFになった時の処理
            ToastText =  "WiFiを切断しました。";
            Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();

        }

    }


    void ChangeWifiProcess(){

        boolean AP_Conn_result = false;

        // targetSSID が既に登録済みの場合
        WifiConfiguration targetSSID = null;

        // getConfiguredNetworksで登録済みの Access Pointを取得しconfigに格納
        // config中に接続したいSSIDがあった場合、config情報をtargetSSIDに引き渡す。
        for (WifiConfiguration config : wm.getConfiguredNetworks()) {
            if (config.SSID.equals('"' + SSID + '"')) {
                targetSSID = config;
                break;
            }
        }

        if (targetSSID != null) {
            for (WifiConfiguration c0 : wm.getConfiguredNetworks()) {
                wm.enableNetwork(c0.networkId, false);
            }

            //Access Pointに接続する。成功可否をAP_SuccessCheckに格納
            AP_SuccessCheck = wm.enableNetwork(targetSSID.networkId, true);


            for(int i = 0; i <= 10; i++){

                WifiInfo WifiInfo;
                String CurrentSSID;
                int CurrentIpAddr;

                try {
                    Thread.sleep(2000); //2000ミリ秒Sleepする
                } catch (InterruptedException e) { }
                WifiInfo = wm.getConnectionInfo();
                CurrentSSID = WifiInfo.getSSID();
                CurrentIpAddr = WifiInfo.getIpAddress();

                if (CurrentSSID.equals('"' + SSID + '"') & (CurrentIpAddr != 0) ){
                    ToastText =  "SSID " + SSID + "への接続に成功しました。";
                    Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();
                    AP_Conn_result = true;
                    break;
                }
            }

            if(!AP_Conn_result) {
                ToastText =  "SSID " + SSID + "への接続に失敗しました。";
                Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();
            }

        } else {
            // 登録されてなかった
            ToastText = "SSID " + SSID + "の設定が見つかりませんでした。";
            Toast.makeText(this, ToastText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 許可された場合
            //logScanResults();
        } else {
            // 許可されなかった場合
            // 何らかの対処が必要
        }
    }

}