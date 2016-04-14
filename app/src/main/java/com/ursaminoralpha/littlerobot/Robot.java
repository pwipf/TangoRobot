package com.ursaminoralpha.littlerobot;

import android.speech.tts.TextToSpeech;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;

/**
 * Created by Magpie on 4/14/2016.
 */
public class Robot{

    static Commands mMovingState=Commands.STOP;
    static Modes mMode=Modes.STOP;

    static MainActivity mainAct;

    static class Target{
        Vec3 pos;
        double rot;
        Target(Vec3 p, double r){
            pos=p; rot=r;
        }
    };

    static ArrayList<Target> mTargetList=new ArrayList<>();
    static int mCurrentTarget=0;
    static boolean mUseTargetRotation=true;
    static boolean mOnTarget=false;
    static boolean mOnTargetRot=false;
    static Vec3 mCurTranslation=new Vec3();
    static Vec3 mToTarget=new Vec3(10, 10, 0);
    static double mYRot=0;
    public static Settings mSettings=new Settings();
    public static UsbSerialPort mPort;

    public enum Commands{
        FORWARD("FORWARD"), SPINRIGHT("SPINRIGHT"), SPINLEFT("SPINLEFT"),
        HALFRIGHT("HALFRIGHT"), HALFLEFT("HALFLEFT"),
        REVERSE("REVERSE"), STOP("STOP"), BEEPHI("BEEPHI"), BEEPLOW("BEEPLOW"),
        BEEPHILOW("BEEPHILOW"), BEEPLOWHI("BEEPLOWHI");
        String name;

        Commands(String s){
            name=s;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    public enum Modes{
        STOP("STOP"), GOTOTARGET("GOTOTARGET"), SEARCHLOC("SEARCH");
        String name;

        Modes(String s){
            name=s;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    //constructor
    Robot(MainActivity mainAct){
        this.mainAct=mainAct;
    }


    public static void go(){
        if(mTargetList.size()==0){
            mainAct.dump("No Targets");
            return;
        }
        sendCommand(Commands.BEEPHI);
        changeMode(Modes.GOTOTARGET);
        mainAct.speak("starting");
    }

    public static void clearTargets(){
        mCurrentTarget=0;
        mTargetList.clear();
        changeMode(Modes.STOP);
    }
    public static void stopEverything(){
        mCurrentTarget=0;
        changeMode(Modes.STOP);
    }

    public static void addTarget(){
        mainAct.dump("Added Target " + mTargetList.size() + "\n");
        mTargetList.add(new Target(mCurTranslation, mYRot));
        //mTargetLoc=mCurTranslation;
        //mTargetRot=mYRot;
        mainAct.speak("target recorded");
    }


    /////////////////////////
    // sendCommands()
    // This sends the serial commands.
    // if manual is true, command is sent even if the same as lastCommand,
    // and lastCommand is not updated.  This keeps the button-press commands(manual=true)
    // separate from the auto commands
    public static void sendCommand(Commands c){
        sendCommandX(c, false);
    }

    public static void sendForcedCommand(Commands c){
        sendCommandX(c, true);
    }

    private static void sendCommandX(Commands c, boolean force){

        if(c==mMovingState && !force)
            return;
        if(c==Commands.FORWARD || c==Commands.STOP
                || c==Commands.SPINRIGHT || c==Commands.SPINLEFT
                || c==Commands.HALFRIGHT || c==Commands.HALFLEFT)
            mMovingState=c;

        byte buf[]=new byte[2];
        int n=0;
        if(mPort!=null){
            switch(c){
                case FORWARD:
                    buf[0]='w';
                    buf[1]='e';
                    break;
                case SPINLEFT:
                    buf[0]='e';
                    buf[1]='x';
                    break;
                case HALFLEFT:
                    buf[0]='e';
                    buf[1]='s';
                    break;
                case SPINRIGHT:
                    buf[0]='w';
                    buf[1]='c';
                    break;
                case HALFRIGHT:
                    buf[0]='w';
                    buf[1]='d';
                    break;
                case STOP:
                    buf[0]='s';
                    buf[1]='d';
                    break;
                case REVERSE:
                    buf[0]='x';
                    buf[1]='c';
                    break;
                case BEEPHILOW:
                    buf[0]='r';
                    buf[1]='t';
                    break;
                case BEEPLOWHI:
                    buf[0]='t';
                    buf[1]='r';
                    break;
                case BEEPHI:
                    buf[0]='r';
                    buf[1]=' ';
                    break;
                case BEEPLOW:
                    buf[0]='t';
                    buf[1]=' ';
                    break;
            }

            // actually send the command over the port
            try{
                n=mPort.write(buf, 5000);
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        //output an info message
        if(n>0){
            mainAct.dump(c + " sent");
        } else{
            mainAct.dump("Tried to send " + c);
        }
    }
    //This is the interesting stuff, called each time the pose data is updated
    //this is called from pose listener thread, NOT UI
    public static void doYourStuff(){
        if(mTargetList.size()>0){
            mToTarget=mTargetList.get(mCurrentTarget).pos.subtract(mCurTranslation);
        } else
            mToTarget=new Target(new Vec3(0, 0, 0), 0).pos.subtract(mCurTranslation);

        final double toAngle=Math.atan2(mToTarget.y, mToTarget.x);
        final double toDist=Math.sqrt(mToTarget.x*mToTarget.x + mToTarget.y*mToTarget.y);
        final double turnAngle=makeAngleInProperRange(toAngle - mYRot);


        mainAct.setDirToText(String.format("toAngle: %.3f, turnAngle: %.3f, Distance: %.3f", toAngle, turnAngle, toDist));

        //READY TO GO!!!
        switch(mMode){
            case GOTOTARGET:
                goToTarget(toDist);
                break;

            case STOP:
                break;

            case SEARCHLOC:
                //This is done by a timertask not the poselistener thread
                //searchForLocalization(false);
                break;
        }
    }

    Timer gSearchTimer;
    public static void changeMode(final Modes m){
        switch(mMode){//current mode
            case STOP:
                break;
            case GOTOTARGET:
                break;
            case SEARCHLOC:
                //gSearchTimer.cancel();
                //gSearchTimer.purge();
                break;
        }
        switch(m){ // to mode
            case STOP:
                sendForcedCommand(Commands.STOP);
                break;
            case GOTOTARGET:
                mOnTarget=false;
                mOnTargetRot=false;
                mainAct.dump("Going to Target " + mCurrentTarget);
                mainAct.setSerialTitleText("Going to Target " + mCurrentTarget + "...");
                break;
            case SEARCHLOC:
//                searchForLocalization(true);
//                gSearchTimer=new Timer();
//                gSearchTimer.schedule(new TimerTask(){
//                    @Override
//                    public void run(){
//                        runOnUiThread(new Runnable(){
//                            @Override
//                            public void run(){
//                                mDumpTextView.append("timer...\n");
//                            }
//                        });
//                        searchForLocalization(false);
//                    }
//                },100,500);
                break;
        }
        mMode=m;
        mainAct.dump("MODECHANGE: " + m);
    }


    // logic to drive toward target
    private static void goToTarget(double toDist){
        try{
            if(mTargetList.size()==0){
                changeMode(Modes.STOP);
                return;
            }

            if(mOnTarget){
                if(toDist<mSettings.threshDistBig){ //on target don't move
                    if(mCurrentTarget>=mTargetList.size() - 1){ // on last target
                        if(mUseTargetRotation && !mOnTargetRot)
                            changeDirection(mTargetList.get(mCurrentTarget - 1).rot, Commands.STOP);
                        if(mOnTargetRot || !mUseTargetRotation){
                            sendCommand(Commands.BEEPLOWHI);
                            changeMode(Modes.STOP);
                            mainAct.dump("At Final Target");
                            mainAct.setSerialTitleText("At Target " + mCurrentTarget + " (End)");
                            mainAct.speak("engaging final target");

                        }
                        return;//don't do anything else
                    } else{ // more targets to go
                        mainAct.dump("Switched to Target " + mCurrentTarget);
                        mainAct.setSerialTitleText("Going to Target " + mCurrentTarget + "...");
                        sendCommand(Commands.BEEPLOWHI);
                        mCurrentTarget++;
                        mOnTarget=false;
                        mOnTargetRot=false;
                        mainAct.speak("going to next target");

                        return;
                    }
                } else{// not on target
                    mOnTarget=false;
                    mOnTargetRot=false;
                }
            }

            if(toDist<mSettings.threshDistSmall){ //on target stop
                sendCommand(Commands.STOP);
                mOnTarget=true;
                mOnTargetRot=false;
                return;
//            if(mUseTargetRotation)
//                changeDirection(mTargetList.get(mCurrentTarget).rot, Commands.STOP);
//            return;
            }

            // need to get closer
            double toAngle=Math.atan2(mToTarget.y, mToTarget.x);
            changeDirection(toAngle, Commands.FORWARD);
        }catch(final Exception e){
            mainAct.dump(e.getMessage());
        }
    }


    //changeDirection()
    // this one accepts a target direction and a command to run
    // after the target direction is aquired, forward or stop I guess
    private static void changeDirection(double targetDir, Commands afterCommand){
        double turnAngle=makeAngleInProperRange(targetDir - mYRot);
        double mag=Math.abs(turnAngle);
        boolean left=turnAngle>0;
        switch(mMovingState){
            case STOP:
            case FORWARD:
                if(mag>mSettings.threshAngleBig){//need to change
                    mOnTargetRot=false;
                    sendCommand(left? Commands.SPINLEFT : Commands.SPINRIGHT);
                }else{
                    mOnTargetRot=true;
                    sendCommand(afterCommand);
                }
                break;
            case SPINRIGHT:
                if(mag<mSettings.threshAngleSmall || left){
                    sendCommand(afterCommand);
                    if(!left)
                        mOnTargetRot=true;
                }
                break;
            case SPINLEFT:
                if(mag<mSettings.threshAngleSmall || !left){
                    sendCommand(afterCommand);
                    if(left)
                        mOnTargetRot=true;
                }
                break;
        }
    }



    ///Search for Localization
    // in theory go in bigger and bigger  circles to get localized
    // not really working yet... works a bit
    static long gTriggerTime;
    static int gSearchMode;
    static double gRadius;
    static int gCircleMode;
    private static void searchForLocalization(boolean reset){
        if(reset){
            gSearchMode=0;
            gRadius=1;
            gTriggerTime=0;
            gCircleMode=0;
            return;
        }

        long curTime = System.currentTimeMillis();

        if(curTime>gTriggerTime){
            switch(gSearchMode){
                case 0:
                    sendCommand(Commands.SPINRIGHT);
                    gSearchMode=1;
                    gTriggerTime=curTime+3000;
                    break;
                case 1:
                    sendCommand(Commands.SPINLEFT);
                    gSearchMode=2;
                    gTriggerTime=curTime+3000;
                    break;
                case 2:
                    switch(gCircleMode){
                        case 0:
                            sendCommand(Commands.FORWARD);
                            gTriggerTime=curTime+1000+(int)(gRadius*1000);
                            gCircleMode=1;
                            break;
                        case 1:
                            sendCommand(Commands.SPINLEFT);
                            gTriggerTime=curTime+(int)(1000/(gRadius));
                            gCircleMode=0;
                            gRadius*=1.5;
                            break;
                    }
                    break;
            }
        }
    }
}
