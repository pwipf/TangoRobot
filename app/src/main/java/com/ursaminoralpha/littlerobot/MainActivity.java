package com.ursaminoralpha.littlerobot;

// CS 4something Robotics with Hunter

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
//import com.google.atap.tango.ux;


import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SetADFNameDialog.CallbackListener{
    // ADF FILE NAME TO LOAD
    //String mADFName="littleRobotView";
    String mADFName="lab2 30mar";
    String mCurrentADFName="none loaded";
    String mADFUUID;

    // remote control server
    RemoteServer mRemoteServer;

    //UI Stuff
    TextView mSerialTitle;
    TextView mTangoTitle;
    TextView mDumpTextView;
    TextView mTranslationTextView, mRotationTextView, mStatusTextView, mDirToTextView;
    ScrollView mDumpScroll;

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
    private AsyncTask mSaveTask;
    private AsyncTask mPermissionsTask;
    //TangoUx mTangoUx;
    int mStatus;
    boolean mTangoReady;

    private double mPreviousTimeStamp;
    private double mTimeToNextUpdate=0;//mSettings.updateInterval;
    boolean mAdfFound=false;

    // various flags
    boolean mLocalized=false;
    boolean mLearning=false;
    boolean mPermissionsReady=false;

    TextToSpeech ttobj;

    Robot mRobot;

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
        mSerialTitle=(TextView)findViewById(R.id.serialTitle);
        mDumpTextView=(TextView)findViewById(R.id.consoleText);
        mDumpScroll=(ScrollView)findViewById(R.id.scroller);

        //initialize Remote control server
        mRemoteServer=new RemoteServer(this);

        mRobot=new Robot(this);

        if(!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
        }
        //Tango Setup
        mTango=new Tango(this, new Runnable(){
            @Override
            public void run(){
                dump("Tango Ready");
                mTangoReady=true;
            }
        });


        mConfig=mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);

        mPermissionsTask = new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params){
                dump("Waiting for ADF permission...");
                while(!Tango.hasPermission(getApplicationContext(), Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)
                        && !isCancelled()){
                    SystemClock.sleep(50);
                }
                if(!Tango.hasPermission(getApplicationContext(), Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
                    dump("Could not get ADF Permission");
                } else{
                    dump("Got ADF Permission, Trying to load ADF: " + mADFName);
                    if(!mTangoReady && !isCancelled())
                        dump("Waiting For Tango Ready");
                    while(!mTangoReady && !isCancelled()){
                        SystemClock.sleep(50);
                    }
                    ArrayList<String> fullUUIDList=mTango.listAreaDescriptions();
                    if(fullUUIDList.size()>0){
                        for(String uuid : fullUUIDList){
                            TangoAreaDescriptionMetaData metadata=mTango.loadAreaDescriptionMetaData(uuid);
                            final String name=new String(metadata.get(TangoAreaDescriptionMetaData.KEY_NAME));
                            if(name.equals(mADFName)){
                                mADFUUID=uuid;
                                mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, uuid);
                                mAdfFound=true;
                                dump("Found ADF");
                            }
                        }
                        if(!mAdfFound)dump("Could not find ADF "+mADFName);
                    }else dump("No ADFs Stored");
                }
                mPermissionsReady=true; // if no permissions, no adf, but still ready to go!
                startTango();
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
        findViewById(R.id.buttonLearnADF).setOnClickListener(this);
        findViewById(R.id.buttonSaveADF).setOnClickListener(this);
//        findViewById(R.id.buttonConnect).setOnClickListener(this);

        mTranslationTextView=(TextView)findViewById(R.id.translation_textview);
        mRotationTextView=(TextView)findViewById(R.id.rotation_textview);
        mStatusTextView=(TextView)findViewById(R.id.status_textview);
        mDirToTextView=(TextView)findViewById(R.id.dirTo_textview);

        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                dump("TTS initialized");
            }
        });
    }//onCreate

    public void speak(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ttobj.speak(s, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
   }

    void setTangoTitleColor(final int color){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mTangoTitle.setTextColor(color);
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

                //send text also to Remote
                sendToRemote(s);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        mRemoteServer.serverStart();
        getSerialDevice();
        startTango();
    }

    void startTango(){
        if(!mIsTangoServiceConnected && mPermissionsReady)
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                // get the name of the ADF set in mConfig
                String uuid=mConfig.getString(TangoConfig.KEY_STRING_AREADESCRIPTION);
                if(uuid==null || uuid.length()==0){
                    mCurrentADFName="NOT USING";
                }else{
                    byte[] n=mTango.loadAreaDescriptionMetaData(uuid).get(TangoAreaDescriptionMetaData.KEY_NAME);
                    if(n!=null)
                    mCurrentADFName= n==null? "NOT FOUND" : new String(n);
                }
                // try to set listeners
                try{
                    setTangoListeners();
                } catch(TangoErrorException e){
                    dump("Tango Error! Restart the app!");
                }

                // try to start service
                try{
                    mTango.connect(mConfig);
                    mIsTangoServiceConnected=true;
                } catch(TangoOutOfDateException e){
                    dump("Tango Service out of date!");
                } catch(TangoErrorException e){
                    dump("Tango Error! Restart the app!");
                }
                setLearningStatus();
            }
        });
    }

    @Override
    protected void onPause(){
        mDumpTextView.append("Pausing...\n");
        mRemoteServer.sendFeedback("Server Pausing, will need to reconnect remote");

        mRemoteServer.serverStop();
        closeUSBSerial();
        disconnectTangoService();
        super.onPause();
    }

    void closeUSBSerial(){
        if(Robot.mPort!=null){
            try{Robot.mPort.close();
            } catch(IOException e){}
        }

        Robot.mPort=null;
        mSerialDeviceSearchTask.cancel(true);
    }

    void sendToRemote(String s){
        mRemoteServer.sendFeedback(s);
    }

    void disconnectTangoService(){
        // tango disconnect, from google java-quick-start-example
        if(mIsTangoServiceConnected){
            try{

                mTango.disconnect();
                mIsTangoServiceConnected=false;
                sendToRemote("Tango Service Disconnect");
            } catch(TangoErrorException e){
                dump("Tango Error!");
            }
        }
    }
//    @Override
//    protected void onStop(){
//        super.onStop();
//        disconnectTangoService();
//    }

    // google java-quick-start-example code to setup pose listener
    // modified
    private void setTangoListeners(){
        dump("setTangoListeners");
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs=new ArrayList<>();

        //if(mAdfFound){
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        //} else{
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));
        //}
        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new OnTangoUpdateListener(){

            @Override
            public void onPoseAvailable(TangoPoseData pose){
                boolean newLoc=mLocalized;
                boolean changed=false; //localization or status
                if(pose.baseFrame==TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                        && pose.targetFrame==TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE){
                    if(pose.statusCode==TangoPoseData.POSE_VALID){
                        newLoc=true;
                        // Set the color to green
                    } else{
                        newLoc=false;
                        // Set the color blue
                    }
                }
                if(mLocalized!=newLoc){
                    mLocalized=newLoc;
                    changed=true;
                    dump("Localized: " + mLocalized);
                }

                if(pose.statusCode!=mStatus){
                    mStatus=pose.statusCode;
                    changed=true;
                    dump("Status: " + statusText(mStatus));
                }

                if(changed){
                    if(mStatus==TangoPoseData.POSE_INITIALIZING){
                        setTangoTitleColor(Color.RED);
                        setTangoTitleText("Tango Initializing (hold still)...");
                    }else{
                        setTangoTitleColor(mLocalized? Color.rgb(0,180,0) : Color.rgb(180,0,0));
                        setTangoTitleText((mLocalized? "Localized" : "Not Localized") + " ADF: " + mCurrentADFName);
                    }
                }

                if(pose.baseFrame==TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                        && pose.targetFrame==TangoPoseData.COORDINATE_FRAME_DEVICE){
                    if(!mLocalized)
                        return;
                }

                if(pose.baseFrame==TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                        && pose.targetFrame==TangoPoseData.COORDINATE_FRAME_DEVICE){
                    if(mLocalized)
                        return;
                }


                // Throttle updates to the UI based on UPDATE_INTERVAL_MS.
                final double deltaTime=(pose.timestamp - mPreviousTimeStamp)
                        *SECS_TO_MILLISECS;
                mPreviousTimeStamp=pose.timestamp;
                mTimeToNextUpdate-=deltaTime;
                if(mTimeToNextUpdate<0.0){
                    mTimeToNextUpdate=Robot.mSettings.updateInterval;

                    setStatusText(statusText(mStatus));

                    Robot.mCurTranslation=new Vec3(pose.translation);

                    //convert quaternion to simple azimuth (z rotation);
                    Robot.mYRot=quaternionToAngle(pose.rotation, 1);

                    //need to add 90 to rotation to line up with x-axis (basis for atan2(y,x) returned angles)
                    Robot.mYRot=makeAngleInProperRange(Robot.mYRot + Math.PI/2);

                    setRotationText(String.format("YRot:%.2f", Robot.mYRot));
                    setTranslationText("Translation: " + Robot.mCurTranslation);

                    //update robot localization status
                    if(mLocalized){
                        if(!Robot.mLocalized){
                            //Robot.changeMode(Robot.Modes.SEARCHLOC);
                            Robot.sendCommand(Commands.BEEPHILOW);
                            Robot.mLocalized=true;
                        }
                    } else{
                        if(Robot.mLocalized){
                            //Robot.changeMode(Robot.Modes.SEARCHLOC);
                            Robot.sendCommand(Commands.BEEPLOWHI);
                            Robot.mLocalized=false;
                        }
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
                    dump(e.eventKey + ": " + e.eventValue);
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
            case R.id.buttonLearnADF:
                actionLearnADF();
                break;
            case R.id.buttonSaveADF:
                actionSaveADF();
                break;
            case R.id.buttonClose:
                exitApp();
                break;
        }
    }

    public void actionLearnADF(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(!mLearning)
                    learnADF();
                else
                    stopLearnADF(mADFUUID);
            }
        });
    }

    private void setLearningStatus(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(mLearning){
                    ((Button)findViewById(R.id.buttonLearnADF)).setText("Cancel Learning");
                    sendToRemote("Learning Mode On");
                } else{
                    ((Button)findViewById(R.id.buttonLearnADF)).setText("Learn ADF");
                    sendToRemote("Learning Mode Off");
                }
            }
        });
    }

    private void showSetADFNameDialog() {
        Bundle bundle = new Bundle();
        bundle.putString("name", "New ADF");
        bundle.putString("id", ""); // UUID is generated after the ADF is saved.

        FragmentManager manager = getFragmentManager();
        SetADFNameDialog setADFNameDialog = new SetADFNameDialog();
        setADFNameDialog.setArguments(bundle);
        setADFNameDialog.show(manager, "ADFNameDialog");
    }

    @Override
    public void onAdfNameOk(String name, String uuid) {
        reallySaveADF(name);
    }

    @Override
    public void onAdfNameCancelled(){

    }

    public void actionSaveADF(){
        if(mLearning){
            showSetADFNameDialog();
        }else{
            dump("Not Learning");
        }
    }

    public void reallySaveADF(final String name){
        dump("starting save task");
        mSaveTask=new AsyncTask<Void, Integer, String>(){
            @Override
            protected String doInBackground(Void... params){
                Log.e("TAG", "saving aDF");
                dump("Saving Area Description...");
                try{
                    String uuid=mTango.saveAreaDescription();
                    dump("Finished Saving, restarting, UUID: " + uuid);
                    return uuid;
                } catch(TangoErrorException e){
                    e.printStackTrace();
                    dump("save exc: " + e.getMessage());
                }
                return "";
            }

            @Override
            protected void onProgressUpdate(Integer... progress){
                dump("Save prog: " + progress);
                sendToRemote("Save Progress: "+progress);
            }

            @Override
            protected void onPostExecute(String uuid){
                disconnectTangoService();
                TangoAreaDescriptionMetaData meta=mTango.loadAreaDescriptionMetaData(uuid);
                meta.set(TangoAreaDescriptionMetaData.KEY_NAME, name.getBytes());
                mTango.saveAreaDescriptionMetadata(uuid, meta);
                dump("Saved ADF as: "+name);
                //sendToRemote("Saved ADF as: "+name);
                stopLearnADF(uuid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void learnADF(){
        try{
            disconnectTangoService();
            mConfig=new TangoConfig();
            mConfig=mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
            mLearning=true;
            dump("learning mode starting");
            Log.e("TAG","learning mode starting");
            startTango();
        }catch(TangoErrorException e){dump("learnADF() tangoExc: "+e.getMessage());}
    }

    private void stopLearnADF(String loadADFuuid){
        try{
            //disconnect
            disconnectTangoService();
            mConfig=mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, false);
            //load adf
            mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, loadADFuuid);
            dump("learning mode stop");
            mLearning=false;
            startTango();
        }catch(TangoErrorException e){dump("stopLearnADF() tangoExc: "+e.getMessage());}
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
        Robot.mSettings.threshAngleBig=pref.getFloat("ThreshAngleBig", .4f);
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
                int dots;
                // this runs in background until a device is found
                @Override
                protected UsbSerialPort doInBackground(Void... params){
                    UsbSerialPort result=null;
                    mUsbManager=(UsbManager)getSystemService(Context.USB_SERVICE);
                    long startTime=System.currentTimeMillis();
                    //start getting device with "timeout"
                    while(result==null && !isCancelled() && System.currentTimeMillis()-startTime<3000){
                        final List<UsbSerialDriver> drivers=UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                        if(drivers.size()>0){
                            UsbSerialDriver driver=drivers.get(0);
                            try{
                                UsbDeviceConnection connection=mUsbManager.openDevice(driver.getDevice());
                                if(connection!=null){
                                    result=driver.getPorts().get(0);
                                }
                            } catch(SecurityException e){
                                dump("GetSerialDevice Exception: "+e.getMessage());
                            }
                        }
                        if(result==null){
                            SystemClock.sleep(500);
                            dots++;
                            if(dots==4) dots=0;
                            publishProgress(dots);
                        }
                    }
                    return result;
                }


                // this is called when the background task finishes
                // (runs on UI thread)
                @Override
                protected void onPostExecute(UsbSerialPort result){
                    mLookingForDevice=false;
                    if(result!=null){
                        openPort(result);
                    }else{
                        mSerialTitle.setTextColor(Color.rgb(180,0,0));
                        mSerialTitle.setText("No Serial Port.");
                    }
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
                    dump("Search USB Cancelled");
                }
            }.execute();
        }
    }

    // open the usb serial port
    // called if the mSerialDeviceSearchTask finds a port
    // only called from that task, in onPostExecute()
    private void openPort(UsbSerialPort result){
        if(result==null)
            return;
        mSerialTitle.setText("Getting connection...");
        UsbDeviceConnection connection=mUsbManager.openDevice(result.getDriver().getDevice());
        if(connection!=null){
            try{
                mSerialTitle.setText("Opening now....");
                result.open(connection);
                mSerialTitle.setText("Opened....");
                result.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                mSerialTitle.setTextColor(Color.rgb(0,180,0));
                mSerialTitle.setText("Serial Port Ready");
                Robot.mPort=result;
            } catch(IOException e){
                try{
                    result.close();
                } catch(IOException f){
                    e.printStackTrace();
                }
            }
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
