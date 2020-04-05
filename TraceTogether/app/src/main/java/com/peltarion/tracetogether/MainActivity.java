package com.peltarion.tracetogether;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.primitives.Longs;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

//@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.peltarion.tracetogether.CASE";
    public static final String CHANNEL_ID = "com.peltarion.tracetogether.NOTIFICATION_CHANNEL";

    final static BluetoothGattCharacteristic USER_ID_CHARACTERISTIC = new BluetoothGattCharacteristic(UUID.fromString("00000000-0000-0000-0000-000000002A3D"),
            BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);

    public static final AtomicInteger notificationId = new AtomicInteger();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        createNotificationChannel();

        //TODO: execute long running tasks in threads
        FirebaseMessaging.getInstance().subscribeToTopic("updates");

        setContentView(R.layout.activity_main);

        TextView myIdView = (TextView) findViewById(R.id.myId);
        myIdView.setText("" + RpcClient.RegisteredId.INSTANCE.id.getId());

        //TODO: do we need to use https://developer.android.com/reference/android/app/job/JobScheduler.html too?
        startService(new Intent(this, YourFirebaseMessagingService.class));
        ble(savedInstanceState);

    }

    private void ble(Bundle savedInstanceState){
        if (savedInstanceState == null) {

            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    startAfterBTHasBeenEnabled();
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported. TODO...
                Log.e("ble", "Not supported!");
                showErrorText(R.string.bt_not_supported);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {
                    startAfterBTHasBeenEnabled();
                } else {

                    Log.e("ble", "BLE not wanted!");
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startAfterBTHasBeenEnabled(){
        //mBluetoothManager.getAdapter().en
        // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
        // this device?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && false) {
            if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                Log.i("ble", "Everything good to go, starting ble service");
                // Everything is supported and enabled, load the fragments.
                //setupFragments();
                startService(getServiceIntentBLE());

            } else {
                //TODO: use startOldAdvertisement?
                Log.e("ble", "Advertisements Not supported!");
                // Bluetooth Advertisements are not supported.
                showErrorText(R.string.bt_ads_not_supported);
            }
            //Start scanning
            Scanner scanner = new Scanner();
            scanner.setBluetoothAdapter(mBluetoothAdapter);
            scanner.startScanning();
        }
        else{

            final AtomicReference<BluetoothGattServer> dummyServer = new AtomicReference<>();
            //BluetoothGattServer server = null;
            final BluetoothGattServerCallback bluetoothGattServerCallback= new BluetoothGattServerCallback() {
                @Override
                public void onConnectionStateChange(final BluetoothDevice device, int status, int newState) {
                    super.onConnectionStateChange(device, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED){
                        Log.e("bt", "Connected to " + device.getName() + " - " + device.toString());
                        //dummyServer.get()..
                        //Server should also read form the client
                        //BluetoothGatt bluetoothGatt = device.connectGatt(MainActivity.this, true, createCallback());
                        //if(!bluetoothGatt.readCharacteristic(USER_ID_CHARACTERISTIC)){
                        //    Log.e("bt", "Failed to read characteristic");
                        //}
                        //bluetoothGatt.readCharacteristic();
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.e("bt", "Disconnected to " + device.getName() + " - " + device.toString());
                    }
                }

                @Override
                public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                    if(characteristic.getUuid().equals(USER_ID_CHARACTERISTIC.getUuid())){
                        dummyServer.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                    }
                    dummyServer.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{});
                }

                @Override
                public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                }

                @Override
                public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                    super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                }

                @Override
                public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                }
            };

            BluetoothGattServer server=mBluetoothManager.openGattServer(this, bluetoothGattServerCallback);
            dummyServer.set(server);

            BluetoothGattService service = new BluetoothGattService(Constants.Service_UUID.getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);


            //characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString("a2bec675-fe11-4d79-aa8e-d317892d8d6d"), BluetoothGattCharacteristic.PERMISSION_WRITE));
            USER_ID_CHARACTERISTIC.setValue(Longs.toByteArray( RpcClient.RegisteredId.INSTANCE.id.getId()));
            if(!service.addCharacteristic(USER_ID_CHARACTERISTIC)){
                Log.e("bt", "Failed to add USER_ID_CHARACTERISTIC");
            }

            if(!server.addService(service)){
                Log.e("bt", "Failed to add bt service");
            }
            //startOldAdvertisement?
            startOldScanner();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startAdvertising();
            }
        }
    }

    private BluetoothGattCallback createCallback(){
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e("bt", "Connected to " +  gatt.getDevice().toString() + ", discovering services.");
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.e("bt", "Services discovered: " +  gatt.getServices());
                BluetoothGattService service = gatt.getService(Constants.Service_UUID.getUuid());
                if(service != null){
                    Log.e("bt", "Found Service_UUID. Reading USER_ID_CHARACTERISTIC.");
                    BluetoothGattCharacteristic fetched = service.getCharacteristic(USER_ID_CHARACTERISTIC.getUuid());
                    byte[] bytes = fetched.getValue();
                    Log.e("bt", "Bytes: " + Arrays.toString(bytes));
                    if (!gatt.readCharacteristic(USER_ID_CHARACTERISTIC)) {
                        Log.e("bt", "Failed to read characteristic");
                    }
                }else {
                    Log.e("bt", "No service found for gatt connection " + gatt.toString());
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (characteristic.getUuid().equals(USER_ID_CHARACTERISTIC.getUuid())) {
                    Log.i("bt", "Read device id: " + Longs.fromByteArray(characteristic.getValue()));
                } else {
                    Log.e("bt", "Read some other characteristic: " + characteristic);
                }
            }
        };
    }

    private void startOldScanner() {
        //TODO: also start advertisement somehow
        Log.e("ble", "isMultipleAdvertisementSupported not supported, too old version! using startLeScan");
        //new UUID[]{Constants.Service_UUID.getUuid()},
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP){
            //New phone runs the advertiser, old phone scans
            final List<BluetoothDevice> foundDevices = new CopyOnWriteArrayList<>();
            final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.e("bt", "Found device: " + device.toString() + " - " + device.getName() + ", connecting to read id. Advertisment data: " + Arrays.toString(scanRecord));
                    foundDevices.add(device);
                }
            };
            if(mBluetoothAdapter.startLeScan(callback)) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.stopLeScan(callback);
                        Log.e("bt", "Stopped scanning. Found " + foundDevices);
                        //getMainExecutor().execute(new Runnable() {
                        //    @Override
                        //    public void run() {
                        for (BluetoothDevice device : foundDevices) {
                            device.connectGatt(MainActivity.this, true, createCallback());
                        }
                         //   }
                        //});
                    }
                }, TimeUnit.SECONDS.toMillis(5));
            }
            else{
                Log.e("bt", "Failed to start scan");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w("bt", "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(Constants.Service_UUID)
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        super.onStartSuccess(settingsInEffect);
                        Log.e("bt", "Success with advertisement: " + settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        super.onStartFailure(errorCode);
                        Log.e("bt", "Failed with advertisement" + errorCode);
                    }
                });
    }

    private void showErrorText(int messageId) {
        //TODO: show errors!
        //TextView view = (TextView) findViewById(R.id.error_textview);
        //view.setText(getString(messageId));
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, SendCaseActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void sendNotification(View view){
        //TODO: consider setFullScreenIntent to really grab the attention!
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_potential_case_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId.incrementAndGet(), notification);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Intent getServiceIntentBLE(){
        return new Intent(this, AdvertiserService.class);
    }
}
