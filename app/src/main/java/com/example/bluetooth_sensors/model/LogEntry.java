package com.example.bluetooth_sensors.model;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

public class LogEntry {
    public String measurementTime;
    public String deviceAddress;
    public float temperature;
    public float pressure;

    public LogEntry(String deviceAddress, float temperature, float pressure) {
        LocalDateTime now = LocalDateTime.now();

        this.measurementTime = now.toString();
        this.deviceAddress = deviceAddress;
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public String getMeasurementTime() {
        return measurementTime;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getPressure() {
        return pressure;
    }
}
