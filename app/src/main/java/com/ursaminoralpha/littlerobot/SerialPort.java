package com.ursaminoralpha.littlerobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialPort{

    private MainActivity mMainAct;
    private UsbManager mUsbManager;
    public UsbSerialPort mPort;


    SerialPort(MainActivity mainAct){
        mMainAct=mainAct;
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mMainAct.registerReceiver(broadcastReceiver,filter);
    }

    public void open(){
        close();
        getSerialDevice();
    }

    public void close(){
        if(mPort!=null){
            try{mPort.close();
            } catch(IOException e){}
        }
        mPort=null;
        mMainAct.setStatusSerial(false,false);
    }

    public boolean isOpen(){
        return mPort!=null;
    }


    public int send(String data,int timeout){
        if(mPort==null)
            return 0;
        byte buf[]=data.getBytes();
        try{
            return mPort.write(buf,timeout);
        }catch(IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    // This is the main function to get and connect to the serial device
    // find the USB Serial Port device
    private void getSerialDevice(){
        mMainAct.setStatusSerial(false,false);
        new AsyncTask<Void, Integer, UsbSerialPort>() {
            @Override
            protected UsbSerialPort doInBackground(Void... params){
                UsbSerialPort result=null;
                mUsbManager=(UsbManager)mMainAct.getSystemService(Context.USB_SERVICE);
                final List<UsbSerialDriver> drivers=UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                if(drivers.size()>0){
                    mMainAct.setStatusSerial(true,false);
                    UsbSerialDriver driver=drivers.get(0);
                    try{
                        UsbDeviceConnection connection=mUsbManager.openDevice(driver.getDevice());
                        if(connection != null){
                            result=driver.getPorts().get(0);
                        }
                    }catch(SecurityException e){
                        mMainAct.dump("GetSerialDevice Exception: " + e.getMessage());
                    }
                }
                return result;
            }

            // this is called when the background task finishes
            // (runs on UI thread)
            @Override
            protected void onPostExecute(UsbSerialPort result){
                if(result != null)
                    openPort(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // open the usb serial port
    // called if the mSerialDeviceSearchTask finds a port
    // only called from that task, in onPostExecute()
    private void openPort(UsbSerialPort result){
        if(result==null)
            return;
        UsbDeviceConnection connection=mUsbManager.openDevice(result.getDriver().getDevice());
        if(connection!=null){
            try{
                result.open(connection);
                result.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                mMainAct.setStatusSerial(true,true);
                mPort=result;
            } catch(IOException e){
                try{
                    result.close();
                } catch(IOException f){
                    e.printStackTrace();
                }
            }
        }
    }




    public final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                open();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                //close();
            }
        }
    };
}
