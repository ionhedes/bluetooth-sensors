package com.example.bluetooth_sensors;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 5;
    private static final String DEVICE_LIST_FILE = "sensor_devices.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void enableBluetooth(View view) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();

        if (bt.isEnabled()) {
            Toast.makeText(MainActivity.this, "Bluetooth already on.", Toast.LENGTH_SHORT).show();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.BLUETOOTH }, REQUEST_ENABLE_BT);
            }
        }
    }

    public void connectToDevices(View view) {
        Intent intent = new Intent(this, DataTransferActivity.class);
        startActivity(intent);
    }

    private boolean checkAddrFormat(@NonNull String addr) {
        String regex = "^([0-9A-Fa-f]{2}[:-])"
                + "{5}([0-9A-Fa-f]{2})|"
                + "([0-9a-fA-F]{4}\\."
                + "[0-9a-fA-F]{4}\\."
                + "[0-9a-fA-F]{4})$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(addr);

        return m.matches();
    }

    public void addNewDevice(View view) {
        EditText deviceAddrField = findViewById(R.id.editTextDeviceAddr);
        File deviceFile = new File(getFilesDir(), DEVICE_LIST_FILE);
        String deviceAddr = deviceAddrField.getText().toString();
        BufferedWriter deviceFileStream;

        if (!checkAddrFormat(deviceAddr)) {
            Toast.makeText(MainActivity.this, "Invalid MAC address format", Toast.LENGTH_SHORT).show();
        } else {
            try {
                deviceFileStream = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(deviceFile)
                        )
                );
                deviceFileStream.write(deviceAddr);
                deviceFileStream.newLine();

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
