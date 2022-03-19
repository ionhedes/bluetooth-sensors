package com.example.bluetooth_sensors;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class DataTransferActivity extends AppCompatActivity {

    public static final String SERVICE_ID = "00001101-0000-1000-8000-00805f9b34fb"; //SPP UUID
    public static final String SERVICE_ADDRESS = "98:D3:71:F6:48:88"; // HC-05 BT ADDRESS

    private TextView console;
    private BluetoothAdapter bluetoothAdapter;
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                console.append("New device found: " + deviceName + " " + deviceHardwareAddress + "\n");
            }
        }
    };

    private void enableBluetooth() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableIntent);
    }

    private boolean initBluetooth(TextView console) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!bluetoothAdapter.isEnabled()) {
                console.append("Bluetooth not enabled. Enabling...\n");
                enableBluetooth();
            }
        } catch (Exception e) {
            console.append("Could not enable bluetooth.\n");
            return false;
        }
        console.append("Bluetooth enabled.\n");
        return true;
    }

    private void readFromHC05(BluetoothSocket socket) {
        byte[] msg = new byte[8];
        float temp;
        if (socket != null) {
            try { // Converting the string to bytes for transferring
                while (socket.getInputStream().available() < 4) ;
                int readSize = socket.getInputStream().read(msg, 0, 4);
                console.append("am citit " + readSize + "\n");

                temp = ByteBuffer.wrap(msg).getFloat();
                //Log.d("TEMP", Double.toString(temp));
                console.append("temp: " + temp + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datatransfer);
        this.console = findViewById(R.id.console);
//        initBluetooth(console);
//
//        // Register for broadcasts when a device is discovered.
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(receiver, filter);
//
//        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//
//        boolean isHC05 = false;
//        BluetoothDevice HC05 = null;
//        if (pairedDevices.size() > 0) {
//            console.append("Paired devices found:\n");
//            // There are paired devices. Get the name and address of each paired device.
//            for (BluetoothDevice device : pairedDevices) {
//                String deviceName = device.getName();
//                if (device.getAddress().equals(SERVICE_ADDRESS)) {
//                    isHC05 = true;
//                    HC05 = device;
//                }
//                String deviceHardwareAddress = device.getAddress(); // MAC address
//                console.append(deviceName + " " + deviceHardwareAddress + "\n");
//            }
//        } else {
//            console.append("No paired device found.\n");
//        }
//
//
//        if (isHC05) {
//            console.append("Found Andrei. Trying to connect...\n");
//            try {
//                BluetoothSocket HC05Socket = HC05.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
//                HC05Socket.connect();
//                console.append("Connected.\n");
//
//                readFromHC05(HC05Socket);
//
//
//            } catch (Exception e) {
//                console.append(e.getMessage());
//            }
//        } else {
//            console.append("Need to find Andrei...\n");
//        }
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        // Don't forget to unregister the ACTION_FOUND receiver.
//        unregisterReceiver(receiver);
//    }
}