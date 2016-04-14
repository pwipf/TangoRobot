package com.ursaminoralpha.littlerobot;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ursaminoralpha.littlerobot.Robot.Commands;
import static com.ursaminoralpha.littlerobot.MathUtil.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    // ADF FILE NAME TO LOAD
    String mADFName="littleRobotView";

    RemoteServer mRemoteServer;

    // TARGET LOCATION (relative to ADF origin) (It is the origin, ADF was started recording
    // from the target location)


    //Vec3 mTargetLoc=new Vec3(0, 0, 0);
    //double mTargetRot=0;

     //read from android preferences, which have defaults, see readPrefs()

    // commands that can be sent over serial to intellibrain


    //UI Stuff
    TextView mSerialTitle;
    TextView mTangoTitle;
    TextView mDumpTextView;
    TextView mTranslationTextView, mRotationTextView, mStatusTextView, mDirToTextView;

    //USB Serial Stuff
    UsbManager mUsbManager;
    Integer mSerialDots=0;
    boolean mLookingForDevice;
    private AsyncTask mSerialDeviceSearchTask;

    //Tango Stuff
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsTangoServiceConnected;
    private static final int SECS_TO_MILLISECS=1000;

    private double mPreviousTimeStamp;
    private double mTimeToNextUpdate=0;//mSettings.updateInterval;
    boolean adfFound=false;

    // Math stuff

    // various flags
    boolean mLocalized=false;

    TextToSpeech ttobj;

    Robot mRobot;

    ScrollView mDumpScroll;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // read settings
        readPrefs();

        // have screen not sleep while this activity running
        findViewById(android.R.id.content).setKeepScreenOn(true);

        //UI Setup
        setContentView(R.layout.activity_main);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Little Robot");

        mTangoTitle=(TextView)findViewById(R.id.tangoTitle);
        mTangoTitle.setText("Loading ADF...");
        mSerialTitle=(TextView)findViewById(R.id.serialTitle);
        mSerialTitle.setText("Starting Serial...");
        mDumpTextView=(TextView)findViewById(R.id.consoleText);
        mDumpScroll=(ScrollView)findViewById(R.id.scroller);

        //initialize Remote control server
        mRemoteServer=new RemoteServer(this);

        mRobot=new Robot(this);

        //Tango Setup
        mTango=new Tango(this, new Runnable(){
            @Override
            public void run(){
                dump("Tango Ready");
            }
        });

        mConfig=mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);

        if(!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            startActivityForResult(
                    Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                    Tango.TANGO_INTENT_ACTIVITYCODE);
        }

        new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params){
                dump("Waiting for permission...");
                while(!Tango.hasPermission(getApplicationContext(), Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){

                    SystemClock.sleep(100);
                }

                ArrayList<String> fullUUIDList=mTango.listAreaDescriptions();
                // Load the proper ADF if ADFs are found
                //mDumpTextView.append("looking for ADF file: " + mADFName + "...\n");
                if(fullUUIDList.size()>0){
                    for(String uuid : fullUUIDList){
                        TangoAreaDescriptionMetaData metadata=mTango.loadAreaDescriptionMetaData(uuid);
                        final String name=new String(metadata.get(TangoAreaDescriptionMetaData.KEY_NAME));
                        if(name.equals(mADFName)){
                            dump("loading ADF: " + name + "...");
                            mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, uuid);
                            adfFound=true;
                        } else{
                           dump("found ADF: " + name + "...");
                        }
                    }
                }
                else{
                    dump("!No ADF file listed.");
                }
                if(!adfFound)
                    dump("!!Did not load ADF file: " + mADFName);
                else
                    dump("Loaded ADF");
            return 0;
            }
        }.execute();


        //Buttons Setup
        findViewById(R.id.buttonForward).setOnClickListener(this);
        findViewById(R.id.buttonLeft).setOnClickListener(this);
        findViewById(R.id.buttonRight).setOnClickListener(this);
        findViewById(R.id.buttonReverse).setOnClickListener(this);
        findViewById(R.id.buttonStop).setOnClickListener(this);
        findViewById(R.id.buttonCountDown).setOnClickListener(this);
        findViewById(R.id.buttonStopEverything).setOnClickListener(this);
        findViewById(R.id.buttonAddTarget).setOnClickListener(this);
        findViewById(R.id.buttonClose).setOnClickListener(this);
        findViewById(R.id.buttonSettings).setOnClickListener(this);
        findViewById(R.id.buttonResetTargets).setOnClickListener(this);
//        findViewById(R.id.buttonConnect).setOnClickListener(this);

        mTranslationTextView=(TextView)findViewById(R.id.translation_textview);
        mRotationTextView=(TextView)findViewById(R.id.rotation_textview);
        mStatusTextView=(TextView)findViewById(R.id.status_textview);
        mDirToTextView=(TextView)findViewById(R.id.dirTo_textview);

        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        mDumpTextView.append("TTS initialized\n");
                    }
                });
            }
        });
    }

    public void speak(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ttobj.speak(s, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
   }

    public void setTangoTitleText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mTangoTitle.setText(s);
            }
        });
    }
    public void setSerialTitleText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mSerialTitle.setText(s);
            }
        });
    }
    public void setTranslationText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mTranslationTextView.setText(s);
            }
        });
    }
    public void setRotationText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mRotationTextView.setText(s);
            }
        });
    }
    public void setStatusText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mStatusTextView.setText(s);
            }
        });
    }
    public void setDirToText(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mDirToTextView.setText(s);
            }
        });
    }

    public void dump(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mDumpTextView.append(s + "\n");
                mDumpScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        mRemoteServer.serverStart();

        // find and open serial port. this is done asynchonously. The task keeps going
        // in the background until it finds one.
        getSerialDevice();

        // tango setup, direct from google java-quick-start-example
        if(!mIsTangoServiceConnected){
            try{
                setTangoListeners();
            } catch(TangoErrorException e){
                dump("Tango Error! Restart the app!");
            }

            try{
                mTango.connect(mConfig);
                mIsTangoServiceConnected=true;
            } catch(TangoOutOfDateException e){
                dump("Tango Service out of date!");
            } catch(TangoErrorException e){
                dump("Tango Error! Restart the app!");
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        mDumpTextView.append("Pausing...\n");

        mRemoteServer.serverStop();

        //close serial port if open
        if(Robot.mPort!=null){
            try{
                Robot.mPort.close();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        Robot.mPort=null;
        //also stop looking for it in the background!
        if(mSerialDeviceSearchTask!=null && mLookingForDevice)
            mSerialDeviceSearchTask.cancel(true);

        // tango disconnect, from google java-quick-start-example
        try{
            mTango.disconnect();
            mIsTangoServiceConnected=false;
        } catch(TangoErrorException e){
            dump("Tango Error!");
        }
    }

    // google java-quick-start-example code to setup pose listener
    // modified
    private void setTangoListeners(){
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs=new ArrayList<>();

        // here use adf if file was found, otherwise just go without it
        if(adfFound){
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_DEVICE));
        } else{
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                    TangoPoseData.COORDINATE_FRAME_DEVICE));
        }
        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new OnTangoUpdateListener(){

            @Override
            public void onPoseAvailable(TangoPoseData pose){

                final double deltaTime=(pose.timestamp - mPreviousTimeStamp)
                        *SECS_TO_MILLISECS;
                mPreviousTimeStamp=pose.timestamp;
                mTimeToNextUpdate-=deltaTime;


                // Throttle updates to the UI based on UPDATE_INTERVAL_MS.
                if(mTimeToNextUpdate<0.0){
                    mTimeToNextUpdate=Robot.mSettings.updateInterval;
                    final int sc=pose.statusCode;

                    Robot.mCurTranslation=new Vec3(pose.translation);

                    //convert quaternion to simple azimuth (z rotation);
                    Robot.mYRot=quaternionToAngle(pose.rotation, 1);

                    //need to add 90 to rotation to line up with x-axis (basis for atan2(y,x) returned angles)
                    Robot.mYRot=makeAngleInProperRange(Robot.mYRot + Math.PI/2);

                    setRotationText(String.format("YRot:%.2f", Robot.mYRot));
                    setTranslationText("Translation: " + Robot.mCurTranslation);
                    setStatusText(statusText(sc));

                    //make some changes based on status code
                    if(sc==TangoPoseData.POSE_INVALID){
                        if(mLocalized){
                            Robot.changeMode(Robot.Modes.SEARCHLOC);
                            Robot.sendCommand(Commands.BEEPHILOW);
                            mLocalized=false;
                        }
                        if(adfFound)
                            setTangoTitleText("Trying to Localize ADF " + mADFName + "...");
                        else
                            setTangoTitleText("ADF file not found");
                    }
                    if(sc==TangoPoseData.POSE_INITIALIZING){
                        mLocalized=false;
                        setTangoTitleText("Tango Initializing (hold still)...");
                    }
                    if(sc==TangoPoseData.POSE_VALID){
                        //if(mMode==Modes.SEARCHLOC){
                        //    changeMode(Modes.GOTOTARGET);
                        //}
                        if(adfFound){
                            if(!mLocalized)
                                Robot.sendCommand(Commands.BEEPLOWHI);
                            mLocalized=true;
                            setTangoTitleText("Localized, Tracking (" + mADFName + ")...");
                        } else
                            setTangoTitleText("Tracking (no ADF)...");
                    }


                    //Run the robot logic
                    Robot.doYourStuff();
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0){
                // Ignoring XyzIj data
            }

            @Override
            public void onTangoEvent(final TangoEvent e){
                if(e.eventType!=TangoEvent.EVENT_FISHEYE_CAMERA){
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            mDumpTextView.append(e.eventKey + ": " + e.eventValue + "\n");
                        }
                    });
                }
            }

            @Override
            public void onFrameAvailable(int arg0){
                // Ignoring onFrameAvailable Events
            }

        });
    }

    //helper function
    public String statusText(int status){
        switch(status){
            case TangoPoseData.POSE_INITIALIZING:
                return "POSE_INITIALIZING";
            case TangoPoseData.POSE_INVALID:
                return "POSE_INVALID";
            case TangoPoseData.POSE_VALID:
                return "POSE_VALID";
            case TangoPoseData.POSE_UNKNOWN:
                return "POSE_UNKNOWN";
        }
        return "OTHER";
    }


    // finally the onClick function
    @Override
    public void onClick(View view){

        Robot.sendCommand(Commands.BEEPLOW);
        switch(view.getId()){
            case R.id.buttonForward:
                Robot.sendForcedCommand(Commands.FORWARD);
                break;
            case R.id.buttonLeft:
                Robot.sendForcedCommand(Commands.SPINLEFT);
                break;
            case R.id.buttonRight:
                Robot.sendForcedCommand(Commands.SPINRIGHT);
                break;
            case R.id.buttonReverse:
                Robot.sendForcedCommand(Commands.REVERSE);
                break;
            case R.id.buttonStop:
                Robot.sendForcedCommand(Commands.STOP);
                break;
            case R.id.buttonCountDown:
                actionGo();
                break;
            case R.id.buttonStopEverything:
                actionStopEverything();
                break;
            case R.id.buttonResetTargets:
                actionClearTargets();
                break;
            case R.id.buttonSettings:
                Intent i=new Intent(this, SettingsActivity.class);
                i.putExtra("settings",Robot.mSettings);
                startActivityForResult(i, 1);
                break;
            //Not using a button anymore to start remote server, starts automatically in onResume()
//            case R.id.buttonConnect:
//                if(mServerRunning){
//                    mRemoteServer.serverStop();
//                }else{
//                    mRemoteServer.serverStart();
//                }
//                break;
            case R.id.buttonAddTarget:
                actionAddTarget();
                break;
            case R.id.buttonClose:
                exitApp();
                break;
        }
    }

    // call from RemoteServer to update it's status
    public void setServerStatus(final String status){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                setTitle(status);
            }
        });
    }

    //send command on UI Thread
    // this should be changed to a different thread I guess,
    // not sure if the serial port is thread safe.
    // The automatic commands are called from the tango listener thread
    public void actionCommand(final Robot.Commands c){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Robot.sendForcedCommand(c);
            }
        });
    }

    // start going to target
    public void actionGo(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Robot.go();
            }
        });
    }

    // clear targets
    public void actionClearTargets(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Robot.clearTargets();
            }
        });
    }

    // stop goint to targets
    public void actionStopEverything(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Robot.stopEverything();
            }
        });

    }

    // add target
    public void actionAddTarget(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Robot.addTarget();
            }
        });

    }

    // read some settings from built in pref file
    void readPrefs(){
        SharedPreferences pref= this.getSharedPreferences("Prefs", Activity.MODE_PRIVATE);
        Robot.mSettings.threshDistBig=pref.getFloat("ThreshDistBig", .3f);
        Robot.mSettings.threshDistSmall=pref.getFloat("ThreshDistSmall", .2f);
        Robot.mSettings.threshAngleSmall=pref.getFloat("ThreshAngleBig", .4f);
        Robot.mSettings.threshAngleSmall=pref.getFloat("ThreshAngleSmall",.3f);
        Robot.mSettings.updateInterval=pref.getFloat("UpdateRate",100.0f);
    }

    // this is called when the settings activity returns (startActivityForResult())
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                Robot.mSettings=data.getParcelableExtra("settings");
            }
        }
    }

    // This is the main function to get and connect to the serial device
    // find the USB Serial Port device
    private void getSerialDevice(){
        mSerialTitle.setText("Looking For USB Serial Device...");
        if(!mLookingForDevice){
            mLookingForDevice=true;
            mSerialDeviceSearchTask=new AsyncTask<Void, Integer, UsbSerialPort>(){

                // this runs in background until a device is found
                @Override
                protected UsbSerialPort doInBackground(Void... params){
                    UsbSerialPort result=null;
                    mUsbManager=(UsbManager)getSystemService(Context.USB_SERVICE);
                    while(result==null && !isCancelled()){
                        final List<UsbSerialDriver> drivers=UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                        if(drivers.size()>0){
                            UsbSerialDriver driver=drivers.get(0);
                            try{
                                UsbDeviceConnection connection=mUsbManager.openDevice(driver.getDevice());
                                if(connection!=null){
                                    result=driver.getPorts().get(0);
                                }
                            } catch(SecurityException e){
                                System.exit(1);
                            }

                        }
                        if(result==null){
                            SystemClock.sleep(500);
                            mSerialDots++;
                            if(mSerialDots==4) mSerialDots=0;
                            publishProgress(mSerialDots);
                        }
                    }
                    return result;
                }


                // this is called when the background task finishes
                // (runs on UI thread)
                @Override
                protected void onPostExecute(UsbSerialPort result){
                    mLookingForDevice=false;
                    mSerialTitle.setText("Serial Found");
                    Robot.mPort=result;
                    openPort();
                }

                // this is just to make the elipses in the status keep moving
                // just for fun
                @Override
                protected void onProgressUpdate(Integer... progress){
                    mSerialTitle.setText("Looking For USB Serial Device");
                    for(int i=0; i<progress[0]; i++){
                        mSerialTitle.append(".");
                    }
                }

                // this is called if the task is cancelled
                @Override
                protected void onCancelled(){
                    mLookingForDevice=false;
                }
            }.execute();
        }
    }

    // open the usb serial port
    // called if the mSerialDeviceSearchTask finds a port
    // only called from that task, in onPostExecute()
    private void openPort(){
        mSerialTitle.setText("Getting connection...");
        UsbDeviceConnection connection=mUsbManager.openDevice(Robot.mPort.getDriver().getDevice());
        if(connection!=null){
            try{
                mSerialTitle.setText("Opening now....");
                Robot.mPort.open(connection);
                mSerialTitle.setText("Opened....");
                Robot.mPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                mSerialTitle.setText("Serial Port Ready");
            } catch(IOException e){
                try{
                    Robot.mPort.close();
                } catch(IOException f){
                    e.printStackTrace();
                }
                Robot.mPort=null;
            }
        } else{
            Toast.makeText(this, "Re-Getting Serial Device", Toast.LENGTH_SHORT).show();
            getSerialDevice();
        }
    }

    // close button, should probably close other things, make sure robot is stopped, etc.
    void exitApp(){
        Robot.sendForcedCommand(Robot.Commands.STOP);
        finish();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////



}
