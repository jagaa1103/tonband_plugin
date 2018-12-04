package cordova.plugin.tonband;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.haesung.tonband.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class BluetoothService extends Service {

    String SERVICE_UUID = "0783B03E-8535-B5A0-7140-A304D2495CB7";
    String TX_CHARACTERISTIC = "0783B03E-8535-B5A0-7140-A304D2495CB8";
    String RX_CHARACTERISTIC = "0783B03E-8535-B5A0-7140-A304D2495CBA";

    Boolean isStartedService = false;

    byte[] TEMPERATURE_REQ = {(byte)0xF7, 0x01, 0x01, 0x00, (byte)0xF9};
    byte[] ALARMTEMPERATURE_REQ = {(byte)0xF7, 0x05, 0x02, 0x00, 0x00, (byte)0xF9};
    byte[] BATTERY_REQ = {(byte)0xF7, 0x02, 0x01, 0x00, (byte)0xFA};

    public static BluetoothService instance;

    static String TAG = "BluetoothService";
    static final int JOB_ID = 1001;

    Context mContext = null;

    BluetoothManager bluetoothManager = null;
    BluetoothAdapter adapter = null;
    BluetoothLeScanner mScanner = null;
    BluetoothScanCallback  myScanCallback = null;
    ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    BluetoothGatt gatt = null;

    BluetoothGattService myService = null;
    BluetoothGattCharacteristic txCharacteristic = null;
    BluetoothGattCharacteristic rxCharacteristic = null;


    public static BluetoothService getInstance() {
        return instance;
    }

    Notification notification = null;
    @Override
    public void onCreate() {
         super.onCreate();
         instance = this;
         if (Build.VERSION.SDK_INT >= 26) {
             String CHANNEL_ID = "bluetooth_service";
             String CHANNEL_NAME = "BluetoothService";

             NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
             ((NotificationManager) getSystemService(this.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
             notification = new NotificationCompat.Builder(this, CHANNEL_ID).setCategory(Notification.CATEGORY_SERVICE).setSmallIcon(R.mipmap.icon).setPriority(PRIORITY_MIN).build();
             startForeground(102, notification);
         }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "============= onDestroy =============");
        if(gatt != null) gatt.disconnect();
//        if(adapter != null) adapter.disable();
//        adapter = null;
        gatt = null;
        myScanCallback = null;
        isStartedService = false;
        instance = null;
        stopNotification();
    }

    public void stopNotification(){
        if (Build.VERSION.SDK_INT >= 26) stopForeground(notification.flags);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopNotification();
        this.stopSelf();
    }

    public void initService(Context context){
        Log.d(TAG, "initService");
        mContext = context;
        if(isStartedService) return;
        adapter = null;
        bluetoothManager = (BluetoothManager) mContext.getSystemService(mContext.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        checkPermission();
    }

    public boolean checkPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                return true;
            }else {
                return false;
            }
        }else {
            return true;
        }
    }

    public void startScanning(){
        Log.d(TAG, "startScanning..");
        deviceList.clear();
        mScanner = adapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> scanFilters = Arrays.asList( new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build());
        myScanCallback = new BluetoothScanCallback();
        mScanner.startScan(scanFilters, scanSettings, myScanCallback);
    }

    public class BluetoothScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            Log.d(TAG, "device scanned");
            final BluetoothDevice scannedDevice = adapter.getRemoteDevice(result.getDevice().getAddress());
            if(deviceList.size() > 0){
                Boolean isNewDevice = true;
                for(BluetoothDevice device : deviceList){
                    if(device.getAddress().equals(scannedDevice.getAddress())) isNewDevice = false;
                }
                if(isNewDevice) addDeviceList(scannedDevice);
            }else{
                addDeviceList(scannedDevice);
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {

        }
        @Override
        public void onScanFailed(int errorCode) {
            //Handle error
        }
    }

    public void stopScanning(){
        try{
            if(mScanner != null) mScanner.stopScan(myScanCallback);
            deviceList.clear();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void addDeviceList(BluetoothDevice device){
        deviceList.add(device);
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("name", device.getName());
            jsonObject.put("uuid", device.getAddress());
            sendBroadcast("onScannedDevices", jsonObject);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    protected void sendBroadcast(String state, JSONObject data){
        Intent i = new Intent("tonband_channel");
        i.putExtra("state", state);
        if(data != null) {
            i.putExtra("data", data.toString());
        }else{
            i.putExtra("data", "");
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
    class GattCallback extends BluetoothGattCallback {
        public GattCallback() {
            super();
        }
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange : old = " + status + ", new = " + newState);
            switch (newState){
                case BluetoothGatt.STATE_CONNECTED:
                    gatt.discoverServices();
                    sendBroadcast("onConnected", null);
                    stopScanning();
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    sendBroadcast("onDisconnected", null);
                    break;
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServiceDiscovered : " + status);
            List<BluetoothGattService> services = gatt.getServices();
            for(BluetoothGattService service : services){
                Log.d(TAG, "service: " + service.getUuid().toString());
                if(service.getUuid().toString().equalsIgnoreCase(SERVICE_UUID)){
                    myService = service;
                    List<BluetoothGattCharacteristic> characteristics = myService.getCharacteristics();
                    for(BluetoothGattCharacteristic characteristic : characteristics){
                        if(characteristic.getUuid().toString().equalsIgnoreCase(RX_CHARACTERISTIC)){
                            rxCharacteristic = characteristic;
                        }else if(characteristic.getUuid().toString().equalsIgnoreCase(TX_CHARACTERISTIC)){
                            txCharacteristic = characteristic;
                            gatt.setCharacteristicNotification(txCharacteristic, true);
                        }
                    }
                }
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] bytes = characteristic.getValue();
            String dataString = "";
            for(int i = 0; i < bytes.length; i++){
                dataString += String.format("%02X", bytes[i]);
            }
            Log.d(TAG, "received bytes: " + dataString);
            parseData(bytes);
        }
    }

    public void connectDevice(String uuid){
        for(BluetoothDevice device : deviceList){
            if(device.getAddress().equals(uuid)) {
                gatt = device.connectGatt(mContext, false, new GattCallback());
                break;
            }
        }

    }

    protected boolean sendToCharacteristics(byte[] data){
        if(rxCharacteristic != null){
            rxCharacteristic.setValue(data);
            boolean state = gatt.writeCharacteristic(rxCharacteristic);
            Log.d(TAG, "sendToCharacteristics : state : " + state);
            return state;
        }
        return false;
    }

    public void parseData(byte[] data) {
                if(data[0] != (byte) 0xF7) return;
                try {
                    String header = parseHeader(data[1]);
                    int l = (int) data[2];
                    byte[] dataArray = new byte[l];
                    int counter = 0;
                    for(int i=3; i<(l+3); i++){
                        dataArray[counter] = data[i];
                        counter ++;
                    }
                    int message = parseBody(header, dataArray);
                    if(header == "" || message == -1) return;
                    JSONObject jsonObject = new JSONObject();
                    try{
                        jsonObject.put("header", header);
                        jsonObject.put("data", message);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    sendBroadcast(header, jsonObject);
                }catch(Exception e) {
                    e.printStackTrace();
                }
    }

    public String parseHeader(byte data){
        String header = "";
        switch (data) {
            case (byte)0x10:
                header = "TEMPERATURE_CFM_HEADER";
                break;
            case (byte)0x20:
                header = "BATTERY_CFM_HEADER";
                break;
            case (byte)0x30:
                header = "ALARMTEMPERATURE_IND_HEADER";
                break;
            case (byte)0x40:
                header = "ALARMBATTERY_IND_HEADER";
                break;
            case (byte)0x50:
                header = "ALARMTEMPERATURE_CFM_HEADER";
                break;
        }
        return header;
    }

    public int parseBody(String header, byte[] data){
        int message2 = -1;
        Log.d("Bluetooth", "@>> byte: " + data);
        if(header.equals("TEMPERATURE_CFM_HEADER")){
            int message = 0;
            message =  message | data[1];
            message = message << 8;
            message = message | (byte)(0x00000000 & data[0]);
            return message;
        } else if (header.equals("ALARMTEMPERATURE_CFM_HEADER")) {
            return -1;
        } else if (header.equals("ALARMTEMPERATURE_IND_HEADER")) {
            int message = (int)data[0];
            return message;
        } else if (header.equals("BATTERY_CFM_HEADER")) {
            int message = (int)data[0];
            return message;
        } else if (header.equals("ALARMBATTERY_IND_HEADER")){
            int message = (int)data[0];
            return message;
        }
        return message2;
    }

    public boolean sendTemperatureReq(){
        return sendToCharacteristics(TEMPERATURE_REQ);
    }


    Timer timer = null;
    public void startLoop(int time){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendTemperatureReq();
            }
        };
        timer = new Timer();
        timer.schedule(task, 0, time);
    }

    public void resetTimer(String time){
        if(timer == null) {
            int t = Integer.parseInt(time) * 60000;
            startLoop(t);
            return;
        }else {
            timer.cancel();
            timer = null;
            int t = Integer.parseInt(time) * 60000;
            startLoop(t);
        }
    }

    public void setAlarmTemperature(String hexString){
        byte[] data = hexStringToByteArray(hexString);
        byte[] hex_array = new byte[6];
        hex_array[0] = (byte)0xF7;
        hex_array[1] = (byte)0x05;
        hex_array[2] = (byte)0x02;
        hex_array[3] = data[1];
        hex_array[4] = data[0];
        byte checksum = (byte)(hex_array[0] + hex_array[1] + hex_array[2] + hex_array[3] + hex_array[4]);
        hex_array[5] = checksum;
        sendToCharacteristics(hex_array);
    }

    public void requestBattery(){
        try{
            Thread.sleep(1000);
            sendToCharacteristics(BATTERY_REQ);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
