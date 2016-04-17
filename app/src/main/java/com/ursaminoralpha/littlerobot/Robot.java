package com.ursaminoralpha.littlerobot;


import android.graphics.Color;

import java.util.ArrayList;

import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;

public class Robot{

    private Commands mMovingState=Commands.STOP;
    private Modes mMode=Modes.STOP;

    MainActivity mMainAct;

    private class Target{
        Vec3 pos;
        double rot;
        Target(Vec3 p, double r){
            pos=p; rot=r;
        }
    };

    private ArrayList<Target> mTargetList=new ArrayList<>();
    private static int mCurrentTarget=0;
    private boolean mUseTargetRotation=true;
    private boolean mOnTarget=false;
    private boolean mOnTargetRot=false;
    private Vec3 mCurTranslation=new Vec3();
    private Vec3 mToTarget=new Vec3(10, 10, 0);
    private double mYRot=0;
    public Settings mSettings=new Settings();
    private OtherSerial mPortDevice;
    private boolean mLocalized=false;


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
    public Robot(MainActivity mainAct, OtherSerial port){
        this.mMainAct=mainAct;
        mPortDevice=port;
    }

    public void changeMode(final Modes m){
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
                sendManualCommand(Commands.STOP);
                mMainAct.setSerialTitleText("Stopped", Color.BLUE);
                break;
            case GOTOTARGET:
                if(mTargetList.size()==0){
                    mMainAct.dump("No Targets");
                    return;
                }
                sendCommand(Commands.BEEPHI);
                mOnTarget=false;
                mOnTargetRot=false;
                mMainAct.speak("Going to target");
                mMainAct.dump("Going to Target " + mCurrentTarget);
                mMainAct.setSerialTitleText("Going to Target " + mCurrentTarget + "...", Color.BLUE);
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
        mMainAct.dump("MODECHANGE: " + m);
    }

    public void clearTargets(){
        mCurrentTarget=0;
        mTargetList.clear();
        changeMode(Modes.STOP);
    }
    public void stopEverything(){
        mCurrentTarget=0;
        changeMode(Modes.STOP);
    }

    public void addTarget(){
        mMainAct.dump("Added Target " + mTargetList.size() + "\n");
        mTargetList.add(new Target(mCurTranslation, mYRot));
        mMainAct.speak("target recorded");
    }


    /////////////////////////
    // sendCommands()
    // This sends the serial commands.
    // if manual is true, command is sent even if the same as lastCommand,
    // and lastCommand is not updated.  This keeps the button-press commands(manual=true)
    // separate from the auto commands

    public void stop(){
        sendManualCommand(Commands.STOP);
    }

    private void sendCommand(Commands c){
        sendCommandX(c, false);
    }

    public void sendManualCommand(Commands c){
        sendCommandX(c, true);
    }

    private void sendCommandX(Commands c, boolean force){

        if(c==mMovingState && !force)
            return;
        if(c==Commands.FORWARD || c==Commands.STOP
                || c==Commands.SPINRIGHT || c==Commands.SPINLEFT
                || c==Commands.HALFRIGHT || c==Commands.HALFLEFT)
            mMovingState=c;

        byte buf[]=new byte[2];
        int n=0;
        if(mPortDevice.isOpen()){
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
            n=mPortDevice.send(new String(buf),1000);
        }

        //output an info message
        if(n>0){
            mMainAct.dump(c + " sent");
        } else{
            mMainAct.dump("Tried to send " + c);
        }
    }
    //This is the interesting stuff, called each time the pose data is updated
    //this is called from pose listener thread, NOT UI
    private void doYourStuff(){
        if(mTargetList.size()>0){
            mToTarget=mTargetList.get(mCurrentTarget).pos.subtract(mCurTranslation);
        } else
            mToTarget=new Target(new Vec3(0, 0, 0), 0).pos.subtract(mCurTranslation);

        final double toAngle=Math.atan2(mToTarget.y, mToTarget.x);
        final double toDist=Math.sqrt(mToTarget.x*mToTarget.x + mToTarget.y*mToTarget.y);
        final double turnAngle=makeAngleInProperRange(toAngle - mYRot);


        mMainAct.setDirToText(String.format("toAngle: %.3f, turnAngle: %.3f, Distance: %.3f", toAngle, turnAngle, toDist));

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

    // logic to drive toward target
    private void goToTarget(double toDist){
        try{
            //check for errors, just in case, shouldn't be "going to target" in this case
            if(mTargetList.size()==0 || mCurrentTarget>=mTargetList.size()){
                changeMode(Modes.STOP);
                return;
            }

            //On a target, or was, need to go to next target, or turn to proper rotation
            //if last target.  First check if the distance is too large and we are NOT on target
            if(mOnTarget){
                if(toDist<mSettings.threshDistBig){ //on target don't move
                    if(mCurrentTarget==mTargetList.size() - 1){ // on last target
                        if(mUseTargetRotation && !mOnTargetRot)
                            changeDirection(mTargetList.get(mCurrentTarget).rot, Commands.STOP);
                        if(mOnTargetRot || !mUseTargetRotation){
                            changeMode(Modes.STOP);
                            sendCommand(Commands.BEEPLOWHI);
                            mMainAct.dump("At Final Target");
                            mMainAct.setSerialTitleText("At Target " + mCurrentTarget + " (End)", Color.BLUE);
                            mMainAct.speak("engaging final target");
                        }
                        return;//don't do anything else

                    } else{ // more targets to go
                        mMainAct.dump("Switched to Target " + mCurrentTarget);
                        mMainAct.setSerialTitleText("Going to Target " + mCurrentTarget + "...", Color.BLUE);
                        sendCommand(Commands.BEEPLOWHI);
                        mCurrentTarget++;
                        mOnTarget=false;
                        mOnTargetRot=false;
                        mMainAct.speak("going to next target");
                        return;
                    }
                } else{// not on target
                    mOnTarget=false;
                    mOnTargetRot=false;
                }
            }

            //at this point maybe we got close enough to the target, set the flag and return
            if(toDist<mSettings.threshDistSmall){ //on target stop (next update will start going to next target)
                //sendCommand(Commands.STOP);
                mOnTarget=true;
                mOnTargetRot=false;
                return;
            }

            //at this point still need to get closer.
            //strangely, changeDirection() works on this.
            double toAngle=Math.atan2(mToTarget.y, mToTarget.x);
            changeDirection(toAngle, Commands.FORWARD);

        } catch(Exception e){
            mMainAct.dump("goToTarget() exception: " + e.getMessage());
        }
    }


    //changeDirection()
    // this one accepts a target direction and a command to run
    // after the target direction is aquired, forward or stop I guess
    private void changeDirection(double targetDir, Commands afterCommand){
        double turnAngle=makeAngleInProperRange(targetDir - mYRot);
        double mag=Math.abs(turnAngle);
        boolean needToTurnLeft=turnAngle>0; //need to turn left
        switch(mMovingState){
            case STOP:
            case FORWARD:
                if(mag>mSettings.threshAngleBig){//need to change
                    mOnTargetRot=false;
                    sendCommand(needToTurnLeft? Commands.SPINLEFT : Commands.SPINRIGHT);
                }else{
                    mOnTargetRot=true;
                    sendCommand(afterCommand);
                }
                break;
            case SPINRIGHT:
                if(mag<mSettings.threshAngleSmall || needToTurnLeft){
                    sendCommand(afterCommand);
                    if(!needToTurnLeft)
                        mOnTargetRot=true;
                }
                break;
            case SPINLEFT:
                if(mag<mSettings.threshAngleSmall || !needToTurnLeft){
                    sendCommand(afterCommand);
                    if(needToTurnLeft)
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
    private void searchForLocalization(boolean reset){
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

    public void setLocalized(boolean localized){
        mLocalized=localized;
        sendCommand(mLocalized? Commands.BEEPLOWHI : Commands.BEEPHILOW);
    }

    public boolean isLocalized(){
        return mLocalized;
    }

    public void setPose(Vec3 translation, double rotation){
        mCurTranslation=translation;
        mYRot=rotation;
        doYourStuff();
    }
}
