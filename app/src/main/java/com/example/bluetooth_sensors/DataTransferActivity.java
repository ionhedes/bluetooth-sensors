package com.example.bluetooth_sensors;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataTransferActivity extends AppCompatActivity {


    public static final String HC05_UUID = "00001101-0000-1000-8000-00805f9b34fb"; //SPP UUID
    private List<BluetoothConnectThread> deviceThreads = new ArrayList<>(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datatransfer);

        // get list of available devices
        CharSequence[] deviceAddresses = getIntent().getCharSequenceArrayExtra(MainActivity.INTENT_EXTRA);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {

            // get set of all node device addresses
            Set<BluetoothDevice> bondedDevices = MainActivity.bluetoothAdapter.getBondedDevices();
            Set<BluetoothDevice> availableDevices = bondedDevices.stream().filter(
                    bondedDevice -> Arrays.stream(deviceAddresses).anyMatch(
                            deviceAddress -> deviceAddress.equals(bondedDevice.getAddress())
                    )
            ).collect(Collectors.toSet());

            // connect to each node in a new thread
            for (BluetoothDevice device : availableDevices) {
                BluetoothConnectThread newThread = new BluetoothConnectThread(device);
                deviceThreads.add(newThread);
                newThread.start();
            }
        } else {
            Log.e("INIT", "Bluetooth permission denied");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (BluetoothConnectThread thread : deviceThreads) {
            thread.cancel();
        }
    }

    private class BluetoothConnectThread extends Thread {
        private final BluetoothSocket nodeSocket;
        private final BluetoothDevice nodeDevice;

        public BluetoothConnectThread(BluetoothDevice device) {
            nodeDevice = device;
            BluetoothSocket temp = null;

            // create socket for the required device
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    temp = nodeDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(HC05_UUID));
                } else {
                    Log.e(MainActivity.LOG_TAG, "Bluetooth permission denied");
                }
            } catch (IOException e) {

                Log.e(MainActivity.LOG_TAG, "Failed to create socket");
            }

            nodeSocket = temp;
        }

        public void run() {
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    nodeSocket.connect();
                    Log.d(MainActivity.LOG_TAG, "Node " + nodeDevice.getAddress() + " connected successfully");
                } else {
                    Log.e(MainActivity.LOG_TAG, "Bluetooth permission denied");
                }
            } catch (IOException connectException) {
                try {
                    nodeSocket.close();
                    Log.e(MainActivity.LOG_TAG, "Failed to connect to node " + nodeDevice.getAddress());
                } catch (IOException closeException) {
                    Log.e(MainActivity.LOG_TAG, "Failed to close socket.");
                }
                return;
            }

            // call method to get data from device
        }

        public void cancel() {
            try {
                nodeSocket.close();
                Log.d(MainActivity.LOG_TAG, "Closed socket to " + nodeDevice.getAddress());
            } catch (IOException e) {
                Log.e(MainActivity.LOG_TAG, "Could not close socket to " + nodeDevice.getAddress());
            }
        }
    }
}