
package com.makeasy.reactlibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNBluetoothListenerModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "MAKEASY";
    private ReactApplicationContext reactContext;

    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private BluetoothAdapter adapter;

    private static final int ENABLE_LOCATION_SERVICES = 1009;

    public RNBluetoothListenerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
        adapter = getBluetoothAdapter();
        this.reactContext = reactContext;
        registerBluetoothStateReceiver();
        Log.d(LOG_TAG, "BluetoothStateModule created");
    }

    @ReactMethod
    public void getCurrentState(Promise promise) {
        try {
            String state = "unknown";
            if (adapter != null) {
                switch (adapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        state = "on";
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        state = "off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        state = "turning off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        state = "turning on";
                        break;
                }
            }

            WritableMap map = Arguments.createMap();
            map.putString("connectionState", state);

            Log.d(LOG_TAG, "connectionState:" + state);

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("NO_BLUETOOTH", e);
        }
    }

    /*@ReactMethod
    public void setBluetoothOn(Callback callback) {
        if (adapter != null) {
            adapter.enable();
        }
        callback.invoke(null, adapter != null);
    }

    @ReactMethod
    public void setBluetoothOff(Callback callback) {
        if (adapter != null) {
            adapter.disable();
        }
        callback.invoke(null, adapter != null);
    }*/


    private void registerBluetoothStateReceiver() {
        Log.d(LOG_TAG, "registerBluetoothStateReceiver");
        if (adapter == null) {
            Log.d(LOG_TAG, "NO_BLUETOOTH");
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive");
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                String stringState = "";

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        stringState = "off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        stringState = "turning_off";
                        break;
                    case BluetoothAdapter.STATE_ON:
                        stringState = "on";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        stringState = "turning_on";
                        break;
                }

                WritableMap map = Arguments.createMap();
                map.putString("connectionState", stringState);

                Log.d(LOG_TAG, "connectionState: " + stringState);
                emitDeviceEvent("bluetoothDidUpdateState", map);

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
               //Device is now connected
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                WritableMap map = Arguments.createMap();
                map.putString("connectedDevice", device.getName());

                Log.d(LOG_TAG, "connectedDevice: " + device.getName());
                emitDeviceEvent("bluetoothDidUpdateState", map);
            }
            /*else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
             //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            //Device has disconnected
            }*/
            else {
                Log.d(LOG_TAG, "connectedDevice: " + action);
            }
        }
    };

    private void emitDeviceEvent(String eventName, @Nullable WritableMap eventData) {
        // A method for emitting from the native side to JS
        // https://facebook.github.io/react-native/docs/native-modules-android.html#sending-events-to-javascript

        if (reactContext.hasActiveCatalystInstance()) {
            Log.d(LOG_TAG, "Sending event: " + eventName);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, eventData);
            Log.d(LOG_TAG, "emitDeviceEvent: " + eventName);
        }
    }


    private BluetoothAdapter getBluetoothAdapter() {

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }


    @Override
    public String getName() {
        return "RNBluetoothListener";
    }
}
