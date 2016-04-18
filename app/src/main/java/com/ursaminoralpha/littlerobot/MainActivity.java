package com.ursaminoralpha.littlerobot;

// CS 4something Robotics with Hunter

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.ursaminoralpha.littlerobot.Robot.Commands;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SetADFNameDialog.CallbackListener, StatusFragment.OnFragmentInteractionListener {
    // DEFAULT ADF FILE NAME TO INITIALLY LOAD
    String mInitialADF = "lab2 30mar";

    // remote control server
    RemoteServer mRemoteServer;

    //UI Stuff
    private TextView mDumpTextView;
    private ScrollView mDumpScroll;
    private StatusFragment mStatusFrag;

    //USB Serial Stuff
    UsbManager mUsbManager;
    private AsyncTask mSerialDeviceSearchTask;

    TextToSpeech ttobj;

    SerialPort mSerialPort;
    Robot mRobot;
    MapView mMapView;
    TheTango mTango;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // read settings

        // have screen not sleep while this activity running
        findViewById(android.R.id.content).setKeepScreenOn(true);

        //UI Setup

        //mMapView=new MapView(this);

        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.imageMap);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Little Robot");

        //status TextViews
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mDumpScroll = (ScrollView) findViewById(R.id.scroller);
        mStatusFrag = (StatusFragment) getSupportFragmentManager().findFragmentById(R.id.status);

        mStatusFrag.ADFName(mInitialADF);
        //  mStatusFrag.localized(mTango.mLocalized);

        //initialize Remote control server
        mRemoteServer = new RemoteServer(this, 6242);

        mSerialPort = new SerialPort(this);

        //Robot
        mRobot = new Robot(this, mSerialPort);

        //have to load prefs after creating robot
        readPrefs();

        //Tango
        //give tango initial learning mode, adf, and a robot to send updates to
        mTango = new TheTango(this, false, mInitialADF, mRobot);


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
        //findViewById(R.id.buttonConnect).setOnClickListener(this);


        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                dump("TTS initialized");
            }
        });
    }


    @Override
    protected void onStart() {
        Log.e("TAG", "onStart");
        super.onStart();
        mRemoteServer.start();
        mSerialPort.open();
//        mTango.start();
    }

    @Override
    protected void onStop() {
        Log.e("TAG", "onStop");
        super.onStop();
        mDumpTextView.append("Pausing...\n");
        mRemoteServer.sendMessage("Server Pausing, will need to reconnect remote");
        mRemoteServer.stop();
        mRobot.stop(); //better stop the robot!
        mSerialPort.close();
//        mTango.stopTango();
    }

    @Override
    protected void onDestroy() {
        Log.e("TAG", "onDestroy");
        super.onDestroy();
        if (mSerialPort != null)
            unregisterReceiver(mSerialPort.broadcastReceiver);
    }

    @Override
    protected void onResume() {
        Log.e("TAG", "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("TAG", "onPause");
        super.onPause();
    }

    public void speak(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ttobj.speak(s, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    public void dump(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append(s + "\n");
                mDumpScroll.fullScroll(View.FOCUS_DOWN);

                //send text also to Remote
                sendToRemote(s);
            }
        });
    }

    public void sendToRemote(String s) {
        mRemoteServer.sendMessage(s);
    }

    @Override
    public void onClick(View view) {

        mRobot.sendManualCommand(Commands.BEEPLOW);
        switch (view.getId()) {
            case R.id.buttonForward:
                mRobot.sendManualCommand(Commands.FORWARD);
                break;
            case R.id.buttonLeft:
                mRobot.sendManualCommand(Commands.SPINLEFT);
                break;
            case R.id.buttonRight:
                mRobot.sendManualCommand(Commands.SPINRIGHT);
                break;
            case R.id.buttonReverse:
                mRobot.sendManualCommand(Commands.REVERSE);
                break;
            case R.id.buttonStop:
                mRobot.sendManualCommand(Commands.STOP);
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
                Intent i = new Intent(this, SettingsActivity.class);
                i.putExtra("settings", mRobot.mSettings);
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
                finish();
                break;
        }
    }

    public void actionLearnADF() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mTango.isLearningMode())
                    mTango.startLearnADFmode(null);
                else
                    mTango.stopLearnADFmode(mTango.getUUIDFromADFFileName(mInitialADF));
            }
        });
    }

    public void setLearningStatus(final boolean learningMode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (learningMode) {
                    ((Button) findViewById(R.id.buttonLearnADF)).setText("Cancel Learning");
                    sendToRemote("Learning Mode On");
                } else {
                    ((Button) findViewById(R.id.buttonLearnADF)).setText("Learn ADF");
                    sendToRemote("Learning Mode Off");
                }
                mStatusFrag.learning(learningMode);
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
        mTango.saveADF(name);
    }

    @Override
    public void onAdfNameCancelled() {
    }

    public void actionSaveADF() {
        if (mTango.isLearningMode()) {
            mRobot.changeMode(Robot.Modes.STOP);
            showSetADFNameDialog();
        } else {
            dump("Not Learning");
        }
    }

    // call from RemoteServer to update it's status
    public void setServerStatus(final String ip, final int port,
                                final boolean running, final int connections) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setTitle(""+connections);
                mStatusFrag.remoteIP(ip);
                mStatusFrag.remotePort(port);
                mStatusFrag.remoteConnections(connections);
                mStatusFrag.remoteRunning(running);
            }
        });
    }

    public void setSerialStatus(final boolean found, final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusFrag.serialFound(found);
                mStatusFrag.serialCon(connected);
            }
        });
    }


    public void setTangoStatus(final boolean localized, final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusFrag.localized(localized);
                mStatusFrag.poseStatus(status);
            }
        });
    }
    public void setPoseStatus(final Vec3 trans, final float rot){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusFrag.setPose(trans,rot);
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
                mRobot.sendManualCommand(c);
            }
        });
    }

    // start going to target
    public void actionGo(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mRobot.changeMode(Robot.Modes.GOTOTARGET);
            }
        });
    }

    // clear targets
    public void actionClearTargets(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mRobot.clearTargets();
            }
        });
    }

    // stop goint to targets
    public void actionStopEverything(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mRobot.stopEverything();
            }
        });

    }

    // add target
    public void actionAddTarget(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mRobot.addTarget();
            }
        });

    }

    // read some settings from built in pref file
    void readPrefs(){
        SharedPreferences pref= this.getSharedPreferences("Prefs", Activity.MODE_PRIVATE);
        mRobot.mSettings.threshDistBig=pref.getFloat("ThreshDistBig", .3f);
        mRobot.mSettings.threshDistSmall=pref.getFloat("ThreshDistSmall", .2f);
        mRobot.mSettings.threshAngleBig=pref.getFloat("ThreshAngleBig", .4f);
        mRobot.mSettings.threshAngleSmall=pref.getFloat("ThreshAngleSmall",.3f);
        mRobot.mSettings.updateInterval=pref.getFloat("UpdateRate",100.0f);
        mInitialADF=pref.getString("ADFInitial","lab2 30mar");
    }

    void writePrefs(){
        SharedPreferences pref=this.getSharedPreferences("Prefs",Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("ADFInitial",mInitialADF);
        editor.putFloat("ThreshDistBig",mRobot.mSettings.threshDistBig);
        editor.putFloat("ThreshDistSmall",mRobot.mSettings.threshDistSmall);
        editor.putFloat("ThreshAngleBig",mRobot.mSettings.threshAngleBig);
        editor.putFloat("ThreshAngleSmall",mRobot.mSettings.threshAngleSmall);
        editor.putFloat("UpdateRate",mRobot.mSettings.updateInterval);
        editor.commit();
    }


    // this is called when the settings activity returns (startActivityForResult())
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                mRobot.mSettings=data.getParcelableExtra("settings");
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
    }

    @Override
    public void onFragmentInteraction(Uri uri){
    }
}
