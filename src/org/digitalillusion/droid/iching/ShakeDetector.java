package org.digitalillusion.droid.iching;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_GRAVITY = 1.1f;
    private static final int SHAKE_RESET_TIME_MS = 400;
    private static final int SHAKE_DELTA_TIME_MS = 250;

    private OnShakeListener mListener;
    private long mShakeTimestamp;
    private boolean isShaking;

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement.
            double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            final long now = System.currentTimeMillis();
            if (gForce > SHAKE_THRESHOLD_GRAVITY && mShakeTimestamp < now) {
                if (!isShaking) {
                    isShaking = true;
                    mListener.onStartShake();
                } else {
                    mShakeTimestamp = now + SHAKE_DELTA_TIME_MS;
                    mListener.onShake(gX);
                }
            } else if (mShakeTimestamp + SHAKE_RESET_TIME_MS < now) {
                if (isShaking) {
                    mListener.onEndShake();
                    mShakeTimestamp = now + SHAKE_RESET_TIME_MS;
                }
                isShaking = false;
            }
        }
    }

    public interface OnShakeListener {
        void onStartShake();

        void onEndShake();

        void onShake(float xForce);
    }
}