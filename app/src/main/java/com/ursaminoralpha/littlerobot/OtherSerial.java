package com.ursaminoralpha.littlerobot;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Magpie on 4/17/2016.
 */
public class OtherSerial{
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    private UsbManager mUsbManager;
    private UsbDevice device;
    private UsbSerialDevice mPort;
    private UsbDeviceConnection connection;
    private MainActivity mMainAct;


    public OtherSerial(MainActivity main){
        mMainAct=main;
        mUsbManager=(UsbManager)mMainAct.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mMainAct.registerReceiver(broadcastReceiver,filter);
    }

    public void getDevice() {
        HashMap<String,UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mMainAct.dump("Entry: "+entry.getKey());
                mMainAct.dump("VID: "+entry.getValue().getVendorId());
                mMainAct.dump("Prod Name: "+entry.getValue().getProductName());
                mMainAct.dump("PID: "+entry.getValue().getProductId());

                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 10755 || deviceVID==1027)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(mMainAct, 0,
                            new Intent(ACTION_USB_PERMISSION), 0);
                    mUsbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            mMainAct.dump("got intent");
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    mMainAct.dump("got permission");
                    connection = mUsbManager.openDevice(device);
                    mMainAct.dump(connection==null? "connection null":"connection not null");
                    mPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (mPort != null) {
                        if (mPort.open()) { //Set Serial Connection Parameters.
                            mPort.setBaudRate(9600);
                            mPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            mPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            mPort.setParity(UsbSerialInterface.PARITY_NONE);
                            mPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            mPort.read(mCallback);
                            mMainAct.dump("Serial Connection Opened!");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                open();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                close();

            }
        }
    };

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] data) {
            for(byte b:data)
                if(b>=32)
                    mMainAct.dump("("+(char)b+")");
                else
                    mMainAct.dump("("+b+")");
        }
    };
    public void close(){
        if(mPort!=null)
            mPort.close();
        mPort=null;
    }
    public void open(){
        getDevice();
    }
    public boolean isOpen(){
        return mPort!=null;
    }
    public int send(String data,int timeout){
        mPort.write(data.getBytes());
        return data.length();
    }
}
