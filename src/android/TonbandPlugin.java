package cordova.plugin.tonband;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.Call;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class echoes a string called from JavaScript.
 */
public class TonbandPlugin extends CordovaPlugin {
    CallbackContext scanCallback = null;
    CallbackContext connectionCallback = null;
    CallbackContext disconnectCallback = null;
    CallbackContext dataCallback = null;
    CallbackContext reconnectionCallback = null;

    Intent intentBluetooth = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(serviceBroadcastReceiver != null && this.cordova != null && this.cordova.getActivity() != null) {
            this.cordova.getActivity().unregisterReceiver(serviceBroadcastReceiver);
            serviceBroadcastReceiver = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(this.cordova.getActivity() == null || (this.cordova.getActivity() != null && this.cordova.getActivity().isDestroyed())) {
            try{
                BluetoothService.getInstance().stopNotification();
                this.cordova.getActivity().stopService(intentBluetooth);
            }catch(Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("exitApp")){
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }else if(action.equals("startService")) {
            try {
                this.startService(callbackContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if(action.equals("stopService")){
            try{
                BluetoothService.getInstance().stopNotification();
                this.cordova.getActivity().stopService(intentBluetooth);
            }catch(Exception e){
                e.printStackTrace();
            }
            return true;
        } else if(action.equals("startScan")) {
            try {
                this.startScan(callbackContext);
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("startScan error");
            }
            return true;
        } else if(action.equals("stopScan")){
            try{
                this.stopScan(callbackContext);
            }catch(Exception e){
                e.printStackTrace();
                callbackContext.error("stopScan error");
            }
        } else if(action.equals("connect")) {
            try {
                String message = args.getString(0);
                this.connect(message, args, callbackContext);
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("connect error");
            }
            return true;
        } else if(action.equals("disconnect")){
            try{
                this.disconnect(callbackContext);
            }catch (Exception e){
                e.printStackTrace();
            }
            return true;
        } else if(action.equals("startLoop")){
            try{
                String message = args.getString(0);
                this.startLoop(callbackContext, message);
            }catch(Exception e){
                e.printStackTrace();
                callbackContext.error("startLoop error");
            }
            return true;
        } else if(action.equals("resetSettings")){
            try {
                String message = args.getString(0);
                resetSettings(callbackContext, message);
            } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("resetSettings error");
            }
            return true;
        } else if (action.equals("setAlarmTemperature")){
            String temp = args.getString(0);
            this.setAlarmTemperature(temp);
            return true;
        } else if (action.equals("requestBattery")) {
            this.requestBattery();
            return true;
        } else if (action.equals("reconnectionStart")){
            try{
                this.reconnectionStart(callbackContext);
            }catch(Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void startService(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> startService");
        intentBluetooth = new Intent(this.cordova.getActivity().getApplicationContext(), BluetoothService.class);
        if(Build.VERSION.SDK_INT >= 28) {
            this.cordova.getActivity().requestPermissions(new String[]{Manifest.permission.INSTANT_APP_FOREGROUND_SERVICE}, 1);
        }
        startBluetoothService();
        LocalBroadcastManager.getInstance(this.cordova.getActivity().getApplicationContext()).registerReceiver(serviceBroadcastReceiver, new IntentFilter("tonband_channel"));
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            this.cordova.getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
//            this.cordova.getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            checkPermissions();
//        }
        callbackContext.success();
    }

    private void startBluetoothService(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this.cordova != null && this.cordova.getActivity().getApplicationContext() != null) {
            this.cordova.getActivity().getApplicationContext().stopService(intentBluetooth);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    cordova.getActivity().getApplicationContext().startForegroundService(intentBluetooth);
                }
            }, 2000);
        }
        else {
            this.cordova.getActivity().startService(intentBluetooth);
        }
    }

    private void startScan(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> startScan");
        checkPermissions();
        BluetoothService.getInstance().initService(this.cordova.getActivity().getApplication().getApplicationContext());
        scanCallback = callbackContext;
        BluetoothService.getInstance().startScanning();
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        scanCallback.sendPluginResult(result);
    }

    private  void stopScan(CallbackContext callbackContext){
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> stopScan");
        BluetoothService.getInstance().stopScanning();
        callbackContext.success();
    }

    private void connect(String message, JSONArray args, CallbackContext callbackContext) {
        connectionCallback = callbackContext;
        BluetoothService.getInstance().connectDevice(message);
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        connectionCallback.sendPluginResult(result);
        forceDisconnect = false;
    }

    Boolean forceDisconnect = false;
    private void disconnect(CallbackContext callbackContext){
        if(BluetoothService.getInstance() != null) BluetoothService.getInstance().disconnect();
        stopReconnection(false);
        disconnectCallback = callbackContext;
        forceDisconnect = true;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        disconnectCallback.sendPluginResult(result);
        onDisconnect("disconnected");
    }

    private void startLoop(CallbackContext callbackContext, String time) {
        dataCallback = callbackContext;
        BluetoothService.getInstance().sendTemperatureReq();
        BluetoothService.getInstance().resetTimer(time);
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        dataCallback.sendPluginResult(result);
    }

    private void resetSettings(CallbackContext callbackContext, String time) {
        BluetoothService.getInstance().resetTimer(time);
        callbackContext.success();
    }

    private void setAlarmTemperature(String temp){ BluetoothService.getInstance().setAlarmTemperature(temp); }

    private void requestBattery(){
        BluetoothService.getInstance().requestBattery();
    }

    private void reconnectionStart(CallbackContext callbackContext){
        counter = 0;
        reconnectionCallback = callbackContext;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        reconnectionCallback.sendPluginResult(result);
        startReconnection();
    }

    public void onScannedDevices(String device){
        Log.d("TonbandPlugin", "Tonband @>> onScannedDevices: " + device);
        PluginResult result = new PluginResult(PluginResult.Status.OK, device);
        result.setKeepCallback(true);
        scanCallback.sendPluginResult(result);
    }

    public void onConnect(){
        forceDisconnect = false;
        stopReconnection(false);
        PluginResult result = new PluginResult(PluginResult.Status.OK);
        result.setKeepCallback(true);
        connectionCallback.sendPluginResult(result);
    }

    public void onDisconnect(String message){
        if(forceDisconnect) {
            disconnectCallback.success("forceDisconnect");
        }else{
            if(connectionCallback != null) connectionCallback.error(message);
        }
    }

    public void onDataChanged(String message){
        Log.d("TonbandPlugin", "@>> onDataChanged: " + message);
        PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        result.setKeepCallback(true);
        if(dataCallback != null) dataCallback.sendPluginResult(result);
    }

    private BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            String data = intent.getStringExtra("data");
            if("REQUEST_PERMISSION".equals(state)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    cordova.getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }else if("onScannedDevices".equals(state)){
                onScannedDevices(data);
            }else if("onConnected".equals(state)){
                onConnect();
            }else if("onDisconnected".equals(state)){
                onDisconnect("disconnected");
            }else if("TEMPERATURE_CFM_HEADER".equals(state)){
                onDataChanged(data);
            }else if("ALARMTEMPERATURE_CFM_HEADER".equals(state)) {
                onDataChanged(data);
            }else if("ALARMTEMPERATURE_IND_HEADER".equals(state)){
                onDataChanged(data);
            }else if("BATTERY_CFM_HEADER".equals(state)){
                onDataChanged(data);
            }else if("ALARMBATTERY_IND_HEADER".equals(state)){
                onDataChanged(data);
            }
        }
    };

    Boolean isReconnection = false;
    Timer reconnectionTimer = null;
    int counter = 0;
    public void startReconnection(){
        isReconnection = true;
        Log.d("TonbandPlugin", "<<<<<<<<<<<<<<<<<<<<<<< startReconnection >>>>>>>>>>>>>>>>> counter: " + counter);
        if(reconnectionTimer != null) {
            reconnectionTimer.cancel();
            reconnectionTimer = null;
        }
        if(counter == 20 || counter == 40 || counter == 60 || counter == 80 || counter == 100){
            Log.d("TonbandPlugin", "result sending...");
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(true);
            reconnectionCallback.sendPluginResult(result);
        }else if(counter == 120){
            PluginResult result = new PluginResult(PluginResult.Status.ERROR);
            result.setKeepCallback(true);
            reconnectionCallback.sendPluginResult(result);
            stopReconnection(false);
            return;
        }
        counter += 1;
        TimerTask  timerTask = new TimerTask() {
            @Override
            public void run() {
                stopReconnection(true);
            }
        };
        reconnectionTimer = new Timer();
        reconnectionTimer.schedule(timerTask, 20000);
        BluetoothService.getInstance().startScanning();
    }

    public void stopReconnection(Boolean isStartAgain){
        if(reconnectionTimer != null) {
            reconnectionTimer.cancel();
            reconnectionTimer = null;
        }
        if(isStartAgain){
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    startReconnection();
                }
            };
            reconnectionTimer = new Timer();
            reconnectionTimer.schedule(task, 10000);
            BluetoothService.getInstance().stopScanning();
        }
        else {
            if(BluetoothService.getInstance() != null ) BluetoothService.getInstance().stopScanning();
            isReconnection = false;
        }
    }


    int PERMISSIONS_REQUEST_CODE = 100;

    public void checkPermissions(){
        int PERMISSION_ALL = 1;
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };
        Activity activity = this.cordova.getActivity();
        Context context = this.cordova.getActivity().getApplicationContext();
        try {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                int isPermissionEnabled = 0;
                for (String permission : permissions) {
                    if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                        boolean isDontAskAgain = activity.shouldShowRequestPermissionRationale(permission);
                        if(!isDontAskAgain) {
                            Log.d("TonbandPlugin", "'Dont’ ask again' option is activated for Location/Storage Services. Please enable services in Settings to continue.");
                            activity.requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
                            break;
                        } else {
                            Log.d("TonbandPlugin",
                                    "This app requires that Location/Storage Services be enabled for Bluetooth Low Energy devices. Please enable services in Settings to continue.");
                            activity.requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        switch (requestCode) {
            case PackageManager.PERMISSION_DENIED:
                checkPermissions();
                break;
            case PackageManager.PERMISSION_GRANTED:
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                break;
        }
    }
}
