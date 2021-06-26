package com.example.bledongle;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Observable;

import static com.example.bledongle.Util.bleAddress;
import static com.example.bledongle.Util.characterUUID;
import static com.example.bledongle.Util.pinCode;
import static com.example.bledongle.Util.serviceUUID;
import static com.example.bledongle.Util.uuidFromString;

public class MainActivity extends AppCompatActivity {

    Button btn_pair, btn_send;
    EditText edit_address, edit_text;

    private BluetoothGatt mGatt;
    BluetoothDevice device;

    // This is used to allow GUI fragments to subscribe to state change notifications.
    public static class StateObservable extends Observable
    {
        private void notifyChanged() {
            setChanged();
            notifyObservers();
        }
    };
    // When the logic state changes, State.notifyObservers(this) is called.
    public final StateObservable State = new StateObservable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble Not Supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btn_pair = findViewById(R.id.btn_pair);
        btn_send = findViewById(R.id.btn_send);
        edit_address = findViewById(R.id.edit_address);
        edit_text = findViewById(R.id.edit_text);

        btn_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    unpairDevice(device);
                } else {
                    displayState("Pairing...");
                    pairDevice(device);
                }
                //pairDevice(device);
                //broadcastUpdate(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            }
        });

// Actually set it in response to ACTION_PAIRING_REQUEST.
/*
        final IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        getApplicationContext().registerReceiver(broadcastReceiver, pairingRequestFilter);
*/
        // Update the UI.
        State.notifyChanged();

        // Note that we don't actually need to request permission - all apps get BLUETOOTH and BLUETOOTH_ADMIN permissions.
        // LOCATION_COARSE is only used for scanning which I don't need (MAC is hard-coded).

        // Connect to the device.
        connectGatt();
    }
    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Disconnect from the device if we're still connected.
        disconnectGatt();

        // Unregister the broadcast receiver.
        getApplicationContext().unregisterReceiver(broadcastReceiver);
    }

    // The state used by the UI to show connection progress.
    public ConnectionState getConnectionState()
    {
        return mState;
    }

    // Internal state machine.
    public enum ConnectionState
    {
        IDLE,
        CONNECT_GATT,
        DISCOVER_SERVICES,
        READ_CHARACTERISTIC,
        FAILED,
        SUCCEEDED,
    }
    private ConnectionState mState = ConnectionState.IDLE;

    // When this fragment is created it is given the MAC address and PIN to connect to.
//    public byte[] macAddress()
//    {
//        return getArguments().getByteArray("mac");
//    }
//    public int pinCode()
//    {
//        return getArguments().getInt("pin", -1);
//    }

    // Start the connection process.
    private void connectGatt()
    {
        // Disconnect if we are already connected.
        disconnectGatt();

        // Update state.
        mState = ConnectionState.CONNECT_GATT;
        State.notifyChanged();

        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bleAddress);

        // Connect!
        mGatt = device.connectGatt(this, false, mBleCallback);
    }

    private void disconnectGatt()
    {
        if (mGatt != null)
        {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        /*
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, intent);
        */
        final IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(Util.ACTION_GATT_CONNECTED);
        //intentFilter.addAction(Util.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);


    }

    // See https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
    private static final int GATT_ERROR = 0x85;
    private static final int GATT_AUTH_FAIL = 0x89;

    private android.bluetooth.BluetoothGattCallback mBleCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            displayState(String.valueOf(newState));
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    broadcastUpdate(Util.ACTION_GATT_CONNECTED);
                    // Connected to the device. Try to discover services.
                    if (gatt.discoverServices())
                    {
                        // Update state.
                        mState = ConnectionState.DISCOVER_SERVICES;
                        State.notifyChanged();
                    }
                    else
                    {
                        // Couldn't discover services for some reason. Fail.
                        disconnectGatt();
                        mState = ConnectionState.FAILED;
                        State.notifyChanged();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    broadcastUpdate(Util.ACTION_GATT_DISCONNECTED);
                    // If we try to discover services while bonded it seems to disconnect.
                    // We need to debond and rebond...

                    switch (mState)
                    {
                        case IDLE:
                            // Do nothing in this case.
                            break;
                        case CONNECT_GATT:
                            // This can happen if the bond information is incorrect. Delete it and reconnect.
                            deleteBondInformation(gatt.getDevice());
                            connectGatt();
                            break;
                        case DISCOVER_SERVICES:
                            // This can also happen if the bond information is incorrect. Delete it and reconnect.
                            deleteBondInformation(gatt.getDevice());
                            connectGatt();
                            break;
                        case READ_CHARACTERISTIC:
                            // Disconnected while reading the characteristic. Probably just a link failure.
                            gatt.close();
                            mState = ConnectionState.FAILED;
                            State.notifyChanged();
                            break;
                        case FAILED:
                        case SUCCEEDED:
                            // Normal disconnection.
                            break;
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);

            // Services have been discovered. Now I try to read a characteristic that requires MitM protection.
            // This triggers pairing and bonding.

            BluetoothGattService nameService = gatt.getService(uuidFromString(serviceUUID));
            if (nameService == null)
            {
                // Service not found.
                disconnectGatt();
                mState = ConnectionState.FAILED;
                State.notifyChanged();
                return;
            }
            BluetoothGattCharacteristic characteristic = nameService.getCharacteristic(uuidFromString(characterUUID));
            if (characteristic == null)
            {
                // Characteristic not found.
                disconnectGatt();
                mState = ConnectionState.FAILED;
                State.notifyChanged();
                return;
            }

            // Read the characteristic.
            gatt.readCharacteristic(characteristic);
            mState = ConnectionState.READ_CHARACTERISTIC;
            State.notifyChanged();

            // Write Characteristic
//            characteristic.setValue(value);
//            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                // Characteristic read. Check it is the right one.
                if (!uuidFromString(characterUUID).equals(characteristic.getUuid()))
                {
                    // Read the wrong characteristic. This shouldn't happen.
                    disconnectGatt();
                    mState = ConnectionState.FAILED;
                    State.notifyChanged();
                    return;
                }

                // Get the name (the characteristic I am reading just contains the device name).
                byte[] value = characteristic.getValue();
                if (value == null)
                {
                    // Hmm...
                }

                disconnectGatt();
                mState = ConnectionState.SUCCEEDED;
                State.notifyChanged();

                // Success! Save it to the database or whatever...
            }
            else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION)
            {
                // This is where the tricky part comes
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE)
                {
                    // Bonding required.
                    // The broadcast receiver should be called.
                    //broadcastUpdate(BluetoothDevice.ACTION_PAIRING_REQUEST);
                }
                else
                {
                    // ?
                }
            }
            else if (status == GATT_AUTH_FAIL)
            {
                // This can happen because the user ignored the pairing request notification for too long.
                // Or presumably if they put the wrong PIN in.
                disconnectGatt();
                mState = ConnectionState.FAILED;
                State.notifyChanged();
            }
            else if (status == GATT_ERROR)
            {
                // I thought this happened if the bond information was wrong, but now I'm not sure.
                disconnectGatt();
                mState = ConnectionState.FAILED;
                State.notifyChanged();
            }
            else
            {
                // That's weird.
                disconnectGatt();
                mState = ConnectionState.FAILED;
                State.notifyChanged();
            }
        }
    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void displayState(final String state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, state, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (Util.ACTION_GATT_CONNECTED.equals(action)) {
                displayState("Connected");
            } else if (Util.ACTION_GATT_DISCONNECTED.equals(action)) {
                displayState("Disconnected");
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    displayState("Paired");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //btn_pair.setBackgroundDrawable(getResources().getDrawable(R.drawable.frame_round_btn_unpair));
                            btn_pair.setText("UNPAIR DEVICE");
                            //btn_pair.setTextColor(getResources().getColor(R.color.colorAccent));
                        }
                    });
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    displayState("Unpaired");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //btn_pair.setBackgroundDrawable(getResources().getDrawable(R.drawable.frame_round_btn_pair));
                            btn_pair.setText("PAIR DEVICE");
                            //btn_pair.setTextColor(getResources().getColor(R.color.colorWhite));
                        }
                    });

                }

            }
            /*
            final String action = intent.getAction();
            if (Util.ACTION_GATT_CONNECTED.equals(action)) {
                displayState("Connected");
            } else if (Util.ACTION_GATT_DISCONNECTED.equals(action)) {
                displayState("Disconnected");
            }

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction()))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

                if (type == BluetoothDevice.PAIRING_VARIANT_PIN)
                {
                    device.setPin(Util.IntToPasskey_be(pinCode));
                    abortBroadcast();
                }
                else
                {
                    Log.w("Unexpected pairingType:", String.valueOf(type));
                }
            }

             */
        }
    };

    public static void deleteBondInformation(BluetoothDevice device)
    {
        try
        {
            // FFS Google, just unhide the method.
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        }
        catch (Exception e)
        {
            Log.e("exception: ", e.getMessage());
        }
    }
}