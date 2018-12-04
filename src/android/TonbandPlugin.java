package cordova.plugin.tonband;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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

/**
 * This class echoes a string called from JavaScript.
 */
public class TonbandPlugin extends CordovaPlugin {
    CallbackContext scanCallback = null;
    CallbackContext connectionCallback = null;
    CallbackContext dataCallback = null;

    Intent intentBluetooth = null;

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            BluetoothService.getInstance().onDestroy();
            this.cordova.getActivity().stopService(intentBluetooth);
        }catch(Exception e) { e.printStackTrace(); }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("startService")) {
            try {
                this.startService(callbackContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if(action.equals("stopService")){
            try{
                this.cordova.getActivity().stopService(intentBluetooth);
                BluetoothService.getInstance().onDestroy();
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
        } else if(action.equals("connect")){
            try{
                String message = args.getString(0);
                this.connect(message, args, callbackContext);
            }catch(Exception e) {
                e.printStackTrace();
                callbackContext.error("connect error");
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
        } else if (action.equals("requestBattery")){
            this.requestBattery();
            return true;
        }
        return false;
    }

    private void startService(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> startService");
        intentBluetooth = new Intent(this.cordova.getActivity().getApplicationContext(), BluetoothService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) this.cordova.getActivity().startForegroundService(intentBluetooth);
        else this.cordova.getActivity().startService(intentBluetooth);
        LocalBroadcastManager.getInstance(this.cordova.getActivity().getApplicationContext()).registerReceiver(serviceBroadcastReceiver, new IntentFilter("tonband_channel"));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.cordova.getActivity().requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        BluetoothService.getInstance().initService(this.cordova.getActivity().getApplication().getApplicationContext());
        callbackContext.success();
    }

    private void startScan(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> startScan");
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

    private void setAlarmTemperature(String temp){
        BluetoothService.getInstance().setAlarmTemperature(temp);
    }

    private void requestBattery(){
        BluetoothService.getInstance().requestBattery();
    }

    public void onScannedDevices(String device){
        Log.d("TonbandPlugin", "Tonband @>> onScannedDevices: " + device);
        PluginResult result = new PluginResult(PluginResult.Status.OK, device);
        result.setKeepCallback(true);
        scanCallback.sendPluginResult(result);
    }
    public void onConnect(){
        connectionCallback.success();
    }
    public void onDisconnect(String message){
        connectionCallback.error(message);
    }

    public void onDataChanged(String message){
        Log.d("TonbandPlugin", "@>> onDataChanged: " + message);
        PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        result.setKeepCallback(true);
        dataCallback.sendPluginResult(result);
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
}
