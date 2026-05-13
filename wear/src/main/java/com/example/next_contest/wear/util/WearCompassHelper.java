package com.example.next_contest.wear.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class WearCompassHelper implements SensorEventListener {
    private final SensorManager sensorManager;
    private final DegreeCallback callback;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private Sensor activeSensor;

    public WearCompassHelper(Context context, DegreeCallback callback) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.callback = callback;
    }

    public void start() {
        if (sensorManager == null) {
            return;
        }

        activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (activeSensor == null) {
            activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

        if (activeSensor != null) {
            sensorManager.registerListener(this, activeSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }

        float degree;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            degree = (float) Math.toDegrees(orientationAngles[0]);
            degree = (degree + 360.0f) % 360.0f;
        } else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            degree = event.values[0];
        } else {
            return;
        }

        callback.onDegreeChanged(degree);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface DegreeCallback {
        void onDegreeChanged(float degree);
    }
}
