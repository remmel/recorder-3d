package com.remmel.recorder3d.recorder.cam360;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class HandleCam360Handler {
    UsbManager usbManager;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private byte[] bytes;
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;
    private Activity activity;

    private static final String TAG = HandleCam360Handler.class.getSimpleName();

    public HandleCam360Handler(Activity activity) {
        this.activity = activity;
        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);

        PendingIntent permissionIntent = PendingIntent.getBroadcast(this.activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        activity.registerReceiver(usbReceiver, filter);
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            Log.d(TAG, "device");
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public void checkcameraUsb() {
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        bytes = "sdff".getBytes();
        UsbDevice device = deviceList.get("/dev/bus/usb/002/002");
        UsbInterface intf = device.getInterface(0);
        UsbEndpoint endpoint = intf.getEndpoint(0);
        UsbDeviceConnection connection = manager.openDevice(device);
        boolean isClaimed = connection.claimInterface(intf, true);
        int lenTransfered = connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT); //do in another thread
    }
}
