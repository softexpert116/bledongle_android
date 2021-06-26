package com.example.bledongle;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.lang.reflect.Method;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

import static com.example.bledongle.Util.bleAddress;
import static com.example.bledongle.Util.serviceUUID;

public class HomeActivity extends AppCompatActivity {
    Bluetooth bluetooth;
    BluetoothDevice device;
    Button btn_pair, btn_send;
    EditText edit_address, edit_text;
    private ProgressDialog mProgressConnectDlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble Not Supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please open bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mProgressConnectDlg 		= new ProgressDialog(this);

        mProgressConnectDlg.setMessage("Paring...");
        mProgressConnectDlg.setCancelable(false);

        btn_pair = findViewById(R.id.btn_pair);
        btn_send = findViewById(R.id.btn_send);
        edit_address = findViewById(R.id.edit_address);
        edit_text = findViewById(R.id.edit_text);

        btn_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = edit_address.getText().toString().trim();
                if (address.length() == 0) {
                    Util.showAlert(HomeActivity.this, "Warning", "Please input address");
                    return;
                }
                if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                    Util.showAlert(HomeActivity.this, "Warning", "Invalid address");
                    return;
                }
                device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bleAddress);
                if (!bluetooth.isConnected()) {
                    bluetooth.connectToDevice(device);
                } else {
                    bluetooth.disconnect();
                }
                mProgressConnectDlg.show();
            }
        });

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        //bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDiscoveryCallback(discoveryCallback);
        bluetooth.setDeviceCallback(deviceCallback);

    }
    private void refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh");
            if(localMethod != null) {
                localMethod.invoke(gatt);
            }
        } catch(Exception localException) {
            Log.d("Exception", localException.toString());
        }
    }
    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            //scanning = true;
            //Toast.makeText(HomeActivity.this, "Scan started!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDiscoveryFinished() {
            //scanning = false;
            //Toast.makeText(HomeActivity.this, "Scan finished!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            Toast.makeText(HomeActivity.this, "device found!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            //Toast.makeText(HomeActivity.this, "Successfully paired!", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) {
//            Toast.makeText(HomeActivity.this, "Successfully unpaired!", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onError(int errorCode) {
            mProgressConnectDlg.dismiss();
            Toast.makeText(HomeActivity.this, "Error Code: " + String.valueOf(errorCode), Toast.LENGTH_SHORT).show();
        }
    };
    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            mProgressConnectDlg.dismiss();
            btn_pair.setText("Unpair Device");
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            mProgressConnectDlg.dismiss();
            btn_pair.setText("Pair Device");
        }

        @Override
        public void onMessage(byte[] message) {
            String str = new String(message);

        }

        @Override
        public void onError(int errorCode) {

        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            Toast.makeText(HomeActivity.this, "Could not connect, next try in 3 sec...", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetooth.connectToDevice(device);
                }
            }, 3000);
        }
    };
    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override public void onBluetoothTurningOn() {}
        @Override public void onBluetoothTurningOff() {}
        @Override public void onBluetoothOff() {}

        @Override
        public void onBluetoothOn() {
            // doStuffWhenBluetoothOn() ...
        }

        @Override
        public void onUserDeniedActivation() {
            // handle activation denial...
        }
    };
    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        if(bluetooth.isEnabled()){
            // doStuffWhenBluetoothOn() ...
            bluetooth.startScanning();
        } else {
            bluetooth.showEnableDialog(HomeActivity.this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetooth.onActivityResult(requestCode, resultCode);
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();


    }


    @Override
    protected void onResume() {
        super.onResume();

    }


}