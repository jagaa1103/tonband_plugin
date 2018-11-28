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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class BluetoothService extends Service {

    String SERVICE_UUID = "0783B03E-8535-B5A0-7140-A304D2495CB7";
    String TX_CHARACTERISTIC = "0783B03E-8535-B5A0-7140-A304D2495CB8";
    String RX_CHARACTERISTIC = "0783B03E-8535-B5A0-7140-A304D2495CBA";

    Boolean isStartedService = false;

    int REQ_ALARM = 1;
    int REQ_TEMP = 2;
    int REQ_BATT = 3;
    int RES_ALARM = 4;
    int RES_TEMP = 5;
    int RES_BATT = 6;

    byte[] TEMPERATURE_REQ = {(byte)0xF7, 0x01, 0x01, 0x00, (byte)0xF9};
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

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "bluetooth_service";
            String CHANNEL_NAME = "BluetoothService";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(this.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setCategory(Notification.CATEGORY_SERVICE).setSmallIcon(R.drawable.ic_launcher_background).setPriority(PRIORITY_MIN).build();
            startForeground(102, notification);
        }
        initService();
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initService(){
        Log.d(TAG, "initService");
        if(isStartedService) return;
        instance = this;
        sendBroadcast("START_SERVICE");
        mContext = getApplicationContext();
        bluetoothManager = (BluetoothManager) mContext.getSystemService(mContext.BLUETOOTH_SERVICE);
        adapter = null;
        adapter = bluetoothManager.getAdapter();
    }

    public boolean checkPermission(){
        if(mContext == null) initService();
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
        if(adapter == null) {
            initService();
        }
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
                if(!deviceList.contains(scannedDevice)) {
                    deviceList.add(scannedDevice);
                    sendBroadcast("NEW_DEVICE_SCANNED");
                    stopScanning();
                }
            }else{
                deviceList.add(scannedDevice);
                sendBroadcast("NEW_DEVICE_SCANNED");
                stopScanning();
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
            if(deviceList.size() > 0){
                BluetoothDevice device = deviceList.get(0);
                gatt = device.connectGatt(mContext, false, new GattCallback());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    protected void sendBroadcast(String state){
        Intent i = new Intent("tonband_channel");
        i.putExtra("state", state);
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
                    String msg = "DEVICE_CONNECTED >> " + gatt.getDevice().getAddress();
                    sendBroadcast(msg);
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    sendBroadcast("DEVICE_CONNECTING");
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    sendBroadcast("DEVICE_DISCONNECTED");
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
            bufferDataFromDevice(bytes);
        }
    }

    public void connectDevice(){
        startScanning();
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

    public void bufferDataFromDevice(byte[] data) {
        try {
            if(data[0] != (byte) 0xF7){
                return;
            }
            String header = parseHeader(data[1]);
            int command_length = (int) data[2];
            Log.d(TAG, "parseHeader : " + header + ", command_length: " + command_length);
            sendBroadcast(header);
        } catch (Exception e) {
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

    public boolean sendTemperatureReq(){
        return sendToCharacteristics(TEMPERATURE_REQ);
    }
}
