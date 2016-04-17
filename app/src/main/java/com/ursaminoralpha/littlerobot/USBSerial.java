package com.ursaminoralpha.littlerobot;

import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class USBSerial{

    private MainActivity mMainAct;
    private AsyncTask mSerialDeviceSearchTask;
    private UsbManager mUsbManager;
    public UsbSerialPort mPort;

    USBSerial(MainActivity mainAct){
        mMainAct=mainAct;
    }

    public void open(){
        getSerialDevice();
    }

    public void close(){
        if(mPort!=null){
            try{mPort.close();
            } catch(IOException e){}
        }
        mPort=null;
    }


    // This is the main function to get and connect to the serial device
    // find the USB Serial Port device
    private void getSerialDevice(){
        mMainAct.setSerialTitleText("Looking For USB Serial Device...",Color.BLACK);
        mSerialDeviceSearchTask=new AsyncTask<Void,Integer,UsbSerialPort>(){
            // this does not keep checking for a device.
            // in theory should try to get notified if a device gets attached.
            // As it is now, if a device is not attached when starting up, it will not
            // ever be found anyway.
            @Override
            protected UsbSerialPort doInBackground(Void... params){
                UsbSerialPort result=null;
                mUsbManager=(UsbManager)mMainAct.getSystemService(Context.USB_SERVICE);
                final List<UsbSerialDriver> drivers=UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                if(drivers.size()>0){
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
                if(result != null){
                    openPort(result);
                }else{
                    mMainAct.setSerialTitleText("No Serial Port.",Color.rgb(180, 0, 0));
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // open the usb serial port
    // called if the mSerialDeviceSearchTask finds a port
    // only called from that task, in onPostExecute()
    private void openPort(UsbSerialPort result){
        if(result==null)
            return;
        mMainAct.setSerialTitleText("Getting connection...",0);
        UsbDeviceConnection connection=mUsbManager.openDevice(result.getDriver().getDevice());
        if(connection!=null){
            try{
                result.open(connection);
                result.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                mMainAct.setSerialTitleText("Serial Port Ready",Color.rgb(0,180,0));
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
}
