package com.example.bledongle;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.example.bledongle.Util.bleAddress;
import static com.example.bledongle.Util.characterUUID;
import static com.example.bledongle.Util.default_mtu;
import static com.example.bledongle.Util.default_packet;
import static com.example.bledongle.Util.dongleUUID;
import static com.example.bledongle.Util.notifyUUID;
import static com.example.bledongle.Util.serviceUUID;
import static com.example.bledongle.Util.switchUUID;

public class StartActivity extends AppCompatActivity {

    RadioGroup radioGroup;
    RadioButton radio_win, radio_linux;
    Button btn_pair, btn_send;
    EditText edit_address, edit_text, edit_mtu, edit_packet;
    LinearLayout ly_setting;
    private static final String TAG = StartActivity.class.getSimpleName();
    private ProgressDialog mProgressScanDlg, mProgressConnectDlg, mProgressSendDlg;
    int cnt = 0;
    Timer myTimer;
    String address, text;
    BleDevice bleDevice;
    byte[] data = {};
    byte[] packet = {};
    int packet_size = default_packet;
    int mtu_size = default_mtu;
    byte comma = 44;
    int os_code = 1;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = "a";//β";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int iVal = Integer.valueOf(text.charAt(i));
            String sVal;
            if (os_code == 1) {
                sVal = "0" + String.valueOf(iVal);
            } else {
                sVal = "00" + Integer.toHexString(iVal);
            }
            if (data.length > 0) {
                sVal = "," + sVal;
            }
            byte[] tmp = sVal.getBytes();
            data = Util.combineBytes(data, tmp); // decimal bytes
        }
        byte[] dd = EncryptDecryptString.encrypt(data);

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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mProgressScanDlg 		= new ProgressDialog(this);
        mProgressScanDlg.setMessage("Scanning...");
        mProgressScanDlg.setCancelable(false);

        mProgressConnectDlg 		= new ProgressDialog(this);
        mProgressConnectDlg.setMessage("Connecting...");
        mProgressConnectDlg.setCancelable(false);

        mProgressSendDlg 		= new ProgressDialog(this);
        mProgressSendDlg.setMessage("Sending...");
        mProgressSendDlg.setCancelable(false);

        Button btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_address.setText("");
                startScan();
            }
        });
        btn_pair = findViewById(R.id.btn_pair);
        btn_send = findViewById(R.id.btn_send);
        edit_address = findViewById(R.id.edit_address);
        edit_text = findViewById(R.id.edit_text);
        radioGroup = findViewById(R.id.radioGroup);
        radio_win = findViewById(R.id.radio_win);
        radio_linux = findViewById(R.id.radio_linux);
        radio_win.setChecked(true);
        radio_linux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                os_code = 2;
                switch_target();
            }
        });
        radio_win.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                os_code = 1;
                switch_target();
            }
        });

        btn_send.setEnabled(false);
        btn_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address = edit_address.getText().toString();
                if (address.length() == 0) {
                    Util.showAlert(StartActivity.this, "Warning", "Invalid address");
                    return;
                }
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                bleDevice = new BleDevice(device);
                if (BleManager.getInstance().isConnected(address)) {
                    BleManager.getInstance().disconnect(bleDevice);
                } else {
                    connect(bleDevice);
                }
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address = edit_address.getText().toString();
                text = edit_text.getText().toString().trim();
                if (text.length() == 0) {
                    Util.showAlert(StartActivity.this, "Warning", "Please fill in text field.");
                    return;
                }
                data = new byte[]{};

                for (int i = 0; i < text.length(); i++) {
                    int iVal = Integer.valueOf(text.charAt(i));
                    String sVal;
                    if (os_code == 1) {
                        sVal = "0" + String.valueOf(iVal);
                    } else {
                        sVal = "00" + Integer.toHexString(iVal);
                    }
                    if (data.length > 0) {
                        sVal = "," + sVal;
                    }
                    byte[] tmp = sVal.getBytes();
                    data = Util.combineBytes(data, tmp);
                }

                myTimer = new Timer();
                cnt = 0;
                myTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (cnt == 0) {
                                    try {
                                        mProgressSendDlg.show();
                                        cnt ++;
                                        writeText();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    cnt = 2;
                                    myTimer.cancel();
                                    myTimer.purge();
                                }

                            }
                        });
                    }

                }, 3000, 10000);


                BleManager.getInstance().notify(bleDevice, serviceUUID, notifyUUID, new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {

                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Util.showAlert(StartActivity.this, "Notify Set Error", String.valueOf(exception.getDescription()));
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] n_data) {
                        if (n_data[0] == 4) {
                            if (data.length > 0) {
                                try {
                                    writeText();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mProgressSendDlg.dismiss();
                            }
                        } else {
                            Util.showAlert(StartActivity.this, "Notify Error", String.valueOf(n_data[0]));
                        }
                    }
                });
/*
                BleManager.getInstance().read(bleDevice, serviceUUID, characterUUID, new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] bytes) {
                        mProgressSendDlg.dismiss();
                        Util.showAlert(StartActivity.this, "Success", new String(bytes));
                    }

                    @Override
                    public void onReadFailure(BleException e) {
                        mProgressSendDlg.dismiss();
                        Toast.makeText(StartActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                });
*/
            }
        });

    }
    private void switch_target() {
        BleManager.getInstance().removeNotifyCallback(bleDevice, notifyUUID);
        byte[] pre_bytes = new byte[]{(byte)os_code};
        BleManager.getInstance().write(bleDevice, serviceUUID, switchUUID, pre_bytes, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int i, int i1, byte[] bytes) {
                String str;
                if (os_code==1) {
                    str = "Set Windows successfully";
                } else {
                    str = "Set Linux successfully";
                }
                Toast.makeText(StartActivity.this, str, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onWriteFailure(BleException e) {
                mProgressSendDlg.dismiss();
                Toast.makeText(StartActivity.this, e.toString(), Toast.LENGTH_LONG).show();

            }
        });
    }

    private void writeText() throws IOException {
        if (data.length > packet_size) {
            packet = Arrays.copyOf(data, packet_size);
            for (int i = packet_size-1; i >= 0; i--) {
                if (packet[i] == comma) {
                    packet = Arrays.copyOf(packet, i+1);
                    data = Arrays.copyOfRange(data, i+1, data.length);
                    break;
                }
            }
        } else {
            packet = Arrays.copyOf(data, data.length);
            data = new byte[]{};
        }
        BleManager.getInstance().write(bleDevice, serviceUUID, characterUUID, packet, false, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int i, int i1, byte[] bytes) {
                String str = new String(bytes);
                Toast.makeText(StartActivity.this, str, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onWriteFailure(BleException e) {
                mProgressSendDlg.dismiss();
                Toast.makeText(StartActivity.this, e.toString(), Toast.LENGTH_LONG).show();

            }
        });
    }

    private void setScanRule() {
        String[] names = new String[1];
        names[0] = Util.INTENT_PREFIX;

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setDeviceMac(bleAddress)
//                .setDeviceName(true, names)
//                .setAutoConnect(true)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mProgressScanDlg.show();
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                //
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                mProgressScanDlg.dismiss();
                if (scanResultList.size() == 0) {
//                    edit_address.setText("Not available devices!");
//                    edit_address.setEnabled(false);
//                    startScan();
                } else {
                    for (BleDevice bleDevice:scanResultList) {
                        String name = bleDevice.getName();
                        String addr = bleDevice.getMac();
                        if (name != null) {
                            if (name.equals(Util.INTENT_PREFIX)) {
                                edit_address.setText(addr);
                                edit_address.setEnabled(true);
                                //pairDevice(bleDevice.getDevice());
                                //connect(bleDevice);
                                break;
                            }
                        }
                    }
                }


            }
        });
    }
    private void setMtu(int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
//                Util.showAlert(StartActivity.this, "Mtu Set Error", String.valueOf(exception.getDescription()));
                Toast.makeText(StartActivity.this, "MTU Failure: error=" + String.valueOf(exception.getDescription()), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMtuChanged(int mtu) {
//                Util.showAlert(StartActivity.this, "Successful", "Mtu: " + String.valueOf(mtu));
                Toast.makeText(StartActivity.this, "Successful: Mtu=" + String.valueOf(mtu), Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                mProgressConnectDlg.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice1, BleException exception) {
                mProgressConnectDlg.dismiss();
                Toast.makeText(StartActivity.this, "Connection fail", Toast.LENGTH_LONG).show();
                btn_send.setEnabled(false);
                radioGroup.setVisibility(View.GONE);
                connect(bleDevice);
                mProgressConnectDlg.show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice1, BluetoothGatt gatt, int status) {
                mProgressConnectDlg.dismiss();
                //
                btn_pair.setText("Unpair Device");
                edit_address.setEnabled(false);
                btn_send.setEnabled(true);
                radioGroup.setVisibility(View.VISIBLE);
                //bleDevice = bleDevice1;
//                SystemClock.sleep(500);
                setMtu(default_mtu);
//                BleManager.getInstance().read(bleDevice1, serviceUUID, switchUUID, new BleReadCallback() {
//                    @Override
//                    public void onReadSuccess(byte[] bytes) {
//                        mProgressSendDlg.dismiss();
//                        os_code = bytes[0];
//                        if (os_code == 1) {
//                            radio_win.setChecked(true);
//                        } else {
//                            radio_linux.setChecked(true);
//                        }
//                        setMtu(default_mtu);
//
////                        SystemClock.sleep(500);
////                        switch_target();
////                        Util.showAlert(StartActivity.this, "Success", new String(bytes));
//                    }
//
//                    @Override
//                    public void onReadFailure(BleException e) {
//                        mProgressSendDlg.dismiss();
//                        Toast.makeText(StartActivity.this, e.toString(), Toast.LENGTH_LONG).show();
//                    }
//                });


            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mProgressConnectDlg.dismiss();

                if (isActiveDisConnected) {
                    Toast.makeText(StartActivity.this, "Disconnected successfully", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(StartActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                }
                btn_pair.setText("Pair Device");
                edit_address.setEnabled(true);
                btn_send.setEnabled(false);
                radioGroup.setVisibility(View.GONE);
            }
        });
    }

    private void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                startScan();
            } else {
                finish();
            }
        }
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();

    }


    @Override
    protected void onResume() {
        super.onResume();
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                //.setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(10000);

        setScanRule();
//        startScan();
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please open bluetooth", Toast.LENGTH_LONG).show();
            return;
        }
    }

}