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
    CordovaInterface _cordova = null;
    static TonbandPlugin instance = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        _cordova = cordova;
        Intent intent = new Intent(_cordova.getActivity().getApplicationContext(), BluetoothService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) _cordova.getActivity().startForegroundService(intent);
        else _cordova.getActivity().startService(intent);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if(action.equals("startService")){
            this.startService(callbackContext);
            return true;
        } else if(action.equals("scan")){
            this.scan(callbackContext);
            return true;
        } else if(action.equals("connect")){
            String message = args.getString(0);
            this.connect(message, args, callbackContext);
            return true;
        } else if(action.equals("startLoop")){
            String message = args.getString(0);
            this.startLoop(callbackContext, message);
            return true;
        } else if(action.equals("resetSettings")){
            String message = args.getString(0);
            resetSettings(callbackContext, message);
            return true;
        }

        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void startService(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> startService");
        LocalBroadcastManager.getInstance(_cordova.getActivity().getApplicationContext()).registerReceiver(serviceBroadcastReceiver, new IntentFilter("tonband_channel"));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) _cordova.getActivity().requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        BluetoothService.getInstance().initService(_cordova.getActivity().getApplication().getApplicationContext());
        callbackContext.success();
    }

    private void scan(CallbackContext callbackContext) {
        Log.d("TonbandPlugin", "@>> TonbandPlugin >> scan");
        scanCallback = callbackContext;
        BluetoothService.getInstance().startScanning();
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        scanCallback.sendPluginResult(result);
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

    public void onScannedDevices(String device){
        Log.d("TonbandPlugin", "Tonband @>> onScannedDevices: " + device);
        scanCallback.success(device);
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
                    _cordova.getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }else if("onScannedDevices".equals(state)){
                onScannedDevices(data);
            }else if("onConnected".equals(state)){
                onConnect();
            }else if("onDisconnected".equals(state)){
                onDisconnect("disconnected");
            }else if("TEMPERATURE_CFM_HEADER".equals(state)){
                onDataChanged(data);
            }
        }
    };
}
