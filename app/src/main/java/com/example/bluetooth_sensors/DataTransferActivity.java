package com.example.bluetooth_sensors;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetooth_sensors.adapters.DeviceAdapter;
import com.example.bluetooth_sensors.model.Device;
import com.example.bluetooth_sensors.model.LogEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataTransferActivity extends AppCompatActivity {

    /**
     * SPP UUID for any HC-05 bluetooth adapter
     */
    public static final String HC05_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    /**
     * used to keep references to all the device threads running
     */
    private final List<BluetoothConnectThread> deviceThreads = new ArrayList<>(10);

    /**
     * list of devices we will TRY to connect to (name is misleading)
     */
    private final ArrayList<Device> connectedDevices = new ArrayList<>(10);
    private DeviceAdapter deviceListAdapter;
    private int currentPosition = 0;

    /**
     * Creates the layout of the activity
     * Basically generates the list of available devices and generates a recycler view for them
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datatransfer);

        // get list of devices registered in the app
        CharSequence[] registeredDeviceAddresses = getIntent().getCharSequenceArrayExtra(MainActivity.INTENT_EXTRA);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {

            // get set of available devices (registered in app + paired)
            // by finding all the bluetooth devices that have addresses registered in the app
            Set<BluetoothDevice> availableDevices =
                    MainActivity.bluetoothAdapter.getBondedDevices().stream().
                            filter(bondedDevice -> Arrays.stream(registeredDeviceAddresses).
                                            anyMatch(deviceAddress -> deviceAddress.equals(
                                                    bondedDevice.getAddress())
                    )
            ).collect(Collectors.toSet());

            // create a new thread for each available device
            for (BluetoothDevice device : availableDevices) {
                connectedDevices.add(new Device(device.getAddress(), device.getName()));
                BluetoothConnectThread newThread = new BluetoothConnectThread(device, currentPosition++);
                deviceThreads.add(newThread);
                newThread.start();
            }

            // populate the list - each available device gets an entry in the list
            deviceListAdapter = new DeviceAdapter(getApplicationContext(), R.layout.listitem_device, connectedDevices);
            RecyclerView connectedDevicesList = findViewById(R.id.device_list);
            connectedDevicesList.setLayoutManager(new LinearLayoutManager(this));
            connectedDevicesList.setAdapter(deviceListAdapter);
        } else {
            Log.e("INIT", "Bluetooth permission denied");
        }
    }

    /**
     * Called before this activity exits
     * Will signal each thread to stop and close its corresponding sockets
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (BluetoothConnectThread thread : deviceThreads) {
            thread.signalStop();
            thread.cancel();
            Log.d(getString(R.string.log_tag), "Thread " + thread.getName() + " is alive " + thread.isAlive());
        }
    }

    /**
     * Used to connect to each bluetooth device in a separate thread
     */
    private class BluetoothConnectThread extends Thread {

        /**
         * the index in the device list corresponding to the current thread
         */
        private final int positionInList;

        /**
         * bluetooth related fields
         */
        private final BluetoothSocket deviceSocket;
        private final BluetoothDevice device;

        /**
         * flag for stopping the loop inside the thread
         */
        private boolean shouldStop = false;

        private final File deviceLogFile;

        public BluetoothConnectThread(BluetoothDevice device, int positionInList) {
            this.positionInList = positionInList;
            this.device = device;
            this.deviceLogFile = new File(getFilesDir(), device.getAddress() + "_log.json");

            // the bluetooth related fields are final, so we need a temporary reference to work with
            // before setting the deviceSocket field
            BluetoothSocket temp = null;

            // create socket for the required device
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    temp = this.device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(HC05_UUID));
                } else {
                    Log.e(getString(R.string.log_tag), "Bluetooth permission denied");
                }
            } catch (IOException e) {
                Log.e(getString(R.string.log_tag), "Failed to create socket");
            }

            deviceSocket = temp;
        }

        private void writeCurrentLogEntryAsJson(JsonWriter deviceLogFileStream, LogEntry entry) {
            try {
                deviceLogFileStream.beginObject();
                for (Field field : LogEntry.class.getDeclaredFields()) {
                    deviceLogFileStream.name(field.getName());
                    deviceLogFileStream.value(field.get(entry).toString());
                }
                deviceLogFileStream.endObject();
            } catch (IOException ioe) {
                Log.e(getString(R.string.log_tag), "Failed to write to logfile");
            } catch (IllegalAccessException iae) {
                Log.e(getString(R.string.log_tag), iae.getMessage());
            }
        }

        /**
         * Main data transfer function
         * @param deviceLogFileStream
         */
        private void getDataFromDevice(JsonWriter deviceLogFileStream) {
            float temperature;
            float pressure;
            byte[] payload = new byte[8];

            try {
                InputStream socketStream = deviceSocket.getInputStream();

                // beginning JSON array (top-level in log file)
                deviceLogFileStream.beginArray();

                // main reading loop
                while (true) {
                    if (shouldStop) {
                        // thread should exit, so we break the loop
                        break;
                    } else if (socketStream.available() >= 8) {
                        // read 8 bytes (4 temperature, 4 pressure)
                        socketStream.read(payload, 0, 8);

                        temperature = ByteBuffer.wrap(payload, 0, 4).getFloat();
                        pressure = ByteBuffer.wrap(payload, 4, 4).getFloat();

                        // update the underlying array of the list
                        connectedDevices.get(positionInList).setTemperature(temperature);
                        connectedDevices.get(positionInList).setPressure(pressure);

                        // signal the recycler view that its data was updated
                        // have to update the view from the UI thread, not from the current thread
                        runOnUiThread(() -> deviceListAdapter.notifyItemChanged(positionInList));

                        // write the read data to the corresponding device log
                        LogEntry currentLogEntry = new LogEntry(device.getAddress(), temperature, pressure);
                        writeCurrentLogEntryAsJson(deviceLogFileStream, currentLogEntry);
                    }
                }
            } catch (IOException e) {
                Log.e(getString(R.string.log_tag), "Failed to read from socket to " + device.getAddress());
            }

            try {
                // end json array (close all brackets in logfile)
                deviceLogFileStream.endArray();

                // close log file stream
                deviceLogFileStream.close();
            } catch (IOException e) {
                Log.e(getString(R.string.log_tag), "Failed to close log file for " + device.getAddress());
            }
        }

        /**
         * Used to set the flag for breaking the loop
         * When the current activity exits, all running threads should stop, so the onDestroy()
         * method of the view should call this method on all device threads
         */
        public void signalStop() {
            this.shouldStop = true;
        }

        /**
         * Runs the thread
         * Will try to connect to the device
         * If the connection is successful, it will call the function which reads the data
         */
        public void run() {
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                    deviceSocket.connect();

                    if (deviceSocket.isConnected()) {
                        Log.d(getString(R.string.log_tag), "Node " + device.getAddress() + " connected successfully");
                        connectedDevices.get(positionInList).setStatus("connected");

                        // open the log file only if the device successfully connected
                        JsonWriter deviceLogFileStream = new JsonWriter(new FileWriter(deviceLogFile, false));
                        Log.d(getString(R.string.log_tag), "The path for the logfile is " +
                                deviceLogFile.getAbsolutePath());

                        // call method to get data from device
                        getDataFromDevice(deviceLogFileStream);

                        // close the log file
                        deviceLogFileStream.close();
                    }

                } else {
                    Log.e(getString(R.string.log_tag), "Bluetooth permission denied");
                }
            } catch (IOException connectException) {
                try {
                    deviceSocket.close();
                    Log.e(getString(R.string.log_tag), "Failed to connect to node " + device.getAddress());
                } catch (IOException closeException) {
                    Log.e(getString(R.string.log_tag), "Failed to close socket.");
                }
            }
        }

        /**
         * Closes the bluetooth socket (if necessary) before the thread object is recycled
         */
        public void cancel() {
            try {
                if (deviceSocket.isConnected()) {
                    deviceSocket.close();
                    Log.d(getString(R.string.log_tag), "Closed socket to " + device.getAddress());
                }
            } catch (IOException e) {
                Log.e(getString(R.string.log_tag), "Could not close socket to " + device.getAddress());
            }
        }
    }
}