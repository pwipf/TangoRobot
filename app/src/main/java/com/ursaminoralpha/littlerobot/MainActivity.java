package com.ursaminoralpha.littlerobot;

// CS 4something Robotics with Hunter

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.ursaminoralpha.littlerobot.Robot.Commands;
import static com.ursaminoralpha.littlerobot.StatusFragment.*;

public class MainActivity extends AppCompatActivity implements SetADFNameDialog.CallbackListener{
    String mCurrentUUID="";

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
    TangoReal mTango;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // read settings

        // have screen not sleep while this activity running
        findViewById(android.R.id.content).setKeepScreenOn(true);

//        WindowManager.LayoutParams params = getWindow().getAttributes();
//        params.screenBrightness=0;
//        getWindow().setAttributes(params);
        //UI Setup

        //mMapView=new MapView(this);

        setContentView(R.layout.activity_main);

        mMapView=(MapView)findViewById(R.id.imageMap);

        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Little Robot");

        mStatusFrag=new StatusFragment();

        getFragmentManager().beginTransaction().add(R.id.statusHolder, mStatusFrag).commit();

        //status TextViews
        mDumpTextView=(TextView)findViewById(R.id.consoleText);
        mDumpScroll=(ScrollView)findViewById(R.id.scroller);
        //mStatusFrag = (StatusFragment) getFragmentManager().findFragmentById(R.id.status);


        //initialize Remote control server
        mRemoteServer=new RemoteServer(this, 6242);

        mSerialPort=new SerialPort(this);

        //Robot
        mRobot=new Robot(this, mSerialPort);

        //have to load prefs after creating robot
        readPrefs();

        //Tango
        //give tango initial learning mode, adf, and a robot to send updates to
        mTango = new TangoReal(this, false, mCurrentUUID, mRobot);


        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                dump("TTS initialized");
            }
        });
    }


    @Override
    protected void onStart(){
        Log.e("TAG", "onStart");
        super.onStart();
        mRemoteServer.start();
        mTango.start();
    }

    @Override
    protected void onStop(){
        Log.e("TAG", "onStop");
        super.onStop();
        mDumpTextView.append("Pausing...\n");
        sendToRemote("Server Pausing, will need to reconnect remote");
        mRemoteServer.stop();
        mRobot.stop(); //better stop the robot!
        mTango.stop();
    }

    @Override
    protected void onDestroy(){
        Log.e("TAG", "onDestroy");
        super.onDestroy();
        if(mSerialPort != null)
            unregisterReceiver(mSerialPort.broadcastReceiver);
    }

    @Override
    protected void onResume(){
        Log.e("TAG", "onResume");
        super.onResume();
        mSerialPort.open();
    }

    @Override
    protected void onPause(){
        Log.e("TAG", "onPause");
        super.onPause();
        mSerialPort.close();
    }

    public void speak(final String s){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ttobj.speak(s, TextToSpeech.QUEUE_FLUSH, null);
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


    public void onClick(View view){

        mRobot.sendManualCommand(Commands.BEEPLOW);
        switch(view.getId()){
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
//            case R.id.buttonSettings:
//                Intent i = new Intent(this, SettingsActivity.class);
//                i.putExtra("settings", mRobot.mSettings);
//                startActivityForResult(i, 1);
//                break;
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

    public void actionLearnADF(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(!mTango.isLearningMode())
                    mTango.startLearnADFmode(null);
                else
                    mTango.stopLearnADFmode();
            }
        });
    }

    public void setLearningStatus(final boolean learningMode){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(learningMode){
                    ((Button)findViewById(R.id.buttonLearnADF)).setText("Cancel Learning");
                    sendToRemote("Learning Mode On");
                }else{
                    ((Button)findViewById(R.id.buttonLearnADF)).setText("Learn ADF");
                    sendToRemote("Learning Mode Off");
                }
                setStatusItem(StatusItem.LEARNING, learningMode,true);
            }
        });
    }

    private void showSetADFNameDialog(){
        new SetADFNameDialog().show(getFragmentManager(), "ADFNameDialog");
    }

    @Override
    public void onAdfNameOk(String name, String uuid){
        mTango.saveADF(name);
    }

    @Override
    public void onAdfNameCancelled(){
    }

    public void actionSaveADF(){
        if(mTango.isLearningMode()){
            mRobot.changeMode(Robot.Modes.STOP);
            showSetADFNameDialog();
        }else{
            dump("Not Learning");
        }
    }

    public void actionSaveADFName(String name){
        if(mTango.isLearningMode()){
            mRobot.changeMode(Robot.Modes.STOP);
            mTango.saveADF(name);
        }else{
            dump("Not Learning");
        }
    }


    public void sendToRemote(String s){
        mRemoteServer.sendData(SendDataType.STRINGCOMMAND,s,null);
    }

    public void sendToRemote(Vec3 pos, float rot){
        float[] data={(float)pos.x, (float)pos.y, (float)pos.z, rot};
        mRemoteServer.sendData(SendDataType.POSITIONROTATION,null,data);
    }

    public void sendAddedTarget(float x,float y){
        mMapView.addTarget(x,y, Color.MAGENTA);
        float[] f={x,y};
        mRemoteServer.sendData(SendDataType.TARGETADDED,null,f);
    }
    public void sendClearTargets(){
        mMapView.clearTargets();
        mRemoteServer.sendData(SendDataType.TARGETSCLEARED,null,null);
    }

    public void sendAllStatusItems(){
        Log.e("TAG","sendallstatusitems");
        for(StatusItem item : StatusItem.values()){
            sendToRemote(item.string+'%'+item.currentString);
        }
    }

    private void setStatusItem(final StatusItem item, final String s, boolean remoteIt){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                StatusFragment.setStatusItem(item, s);
            }
        });

        if(remoteIt)
            sendToRemote(item.string + '%' + s);
    }

    private void setStatusItem(StatusItem item, Boolean b,boolean remoteIt){
        setStatusItem(item, b? "YES" : "NO", remoteIt);
    }

    public void setStatusRemoteServer(final String ip, final int port,
                                      final boolean running, final int connections){
        setStatusItem(StatusItem.REMOTEIP, ip,false);
        setStatusItem(StatusItem.REMOTEPORT, port + "",false);
        setStatusItem(StatusItem.REMOTERUN, running,false);
        setStatusItem(StatusItem.REMOTECON, connections + "",false);
    }

    public void setStatusSerial(final boolean found, final boolean connected){
        setStatusItem(StatusItem.SERIALFOUND, found,true);
        setStatusItem(StatusItem.SERIALCON, connected,true);
    }


    public void setStatusTango(final boolean localized, final String status){
        setStatusItem(StatusItem.LOCALIZED, localized,true);
        setStatusItem(StatusItem.POSESTAT, status,true);
    }

    public void setStatusPoseData(final Vec3 position, final float rot){
        sendToRemote(position, rot);
        setStatusItem(StatusItem.POSITION, position.toString(),true);
        setStatusItem(StatusItem.ROTATION, String.format("%.1f", rot*(180/Math.PI)),true);
    }

    public void setStatusADFName(final String name){
        setStatusItem(StatusItem.ADFNAME, name,true);
    }

    public void setStatusRobotState(final String state){
        setStatusItem(StatusItem.ROBOLASTCOM, state,true);
    }

    public void setStatusRobotMode(final String mode){
        setStatusItem(StatusItem.ROBOMODE, mode,true);
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


    void writePrefs(){
        SharedPreferences pref=this.getSharedPreferences("Prefs", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor=pref.edit();
        editor.putString("LastUUID", mCurrentUUID);
        editor.putFloat("ThreshDistBig", mRobot.mSettings.threshDistBig);
        editor.putFloat("ThreshDistSmall", mRobot.mSettings.threshDistSmall);
        editor.putFloat("ThreshAngleBig", mRobot.mSettings.threshAngleBig);
        editor.putFloat("ThreshAngleSmall", mRobot.mSettings.threshAngleSmall);
        editor.putFloat("UpdateRate", mRobot.mSettings.updateInterval);
        editor.commit();
    }


    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id=item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_settings){
            Intent i=new Intent(this, SettingsActivity.class);
            i.putExtra("settings", mRobot.mSettings);
            startActivityForResult(i, 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // this is called when the settings activity returns (startActivityForResult())
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                mRobot.mSettings=data.getParcelableExtra("settings");
            }
        }
    }

    // read some settings from built in pref file
    void readPrefs(){
        SharedPreferences pref=this.getSharedPreferences("Prefs", Activity.MODE_PRIVATE);
        mRobot.mSettings.threshDistBig=pref.getFloat("ThreshDistBig", .3f);
        mRobot.mSettings.threshDistSmall=pref.getFloat("ThreshDistSmall", .2f);
        mRobot.mSettings.threshAngleBig=pref.getFloat("ThreshAngleBig", .4f);
        mRobot.mSettings.threshAngleSmall=pref.getFloat("ThreshAngleSmall", .3f);
        mRobot.mSettings.updateInterval=pref.getFloat("UpdateRate", 100.0f);
        mCurrentUUID = pref.getString("LastUUID", "");
    }
}
