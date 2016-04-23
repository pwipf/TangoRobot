package com.ursaminoralpha.littlerobot;


import android.graphics.PointF;

import java.util.ArrayList;

import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;

public class Robot{

    //////////////////////////////////////////////////////////////////////////////////////////////// Member variables
    private Commands mMovingState=Commands.STOP;
    private Modes mMode=Modes.STOP;
    MainActivity mMainAct;
    private ArrayList<Target> mTargetList=new ArrayList<>();
    private static int mCurrentTarget=0;
    private boolean mUseTargetRotation=true;
    private boolean mOnTarget=false;
    private boolean mOnTargetRot=false;
    private Vec3 mCurTranslation=new Vec3();
    private Vec3 mToTarget=new Vec3(10, 10, 0);
    private double mYRot=0;
    public Settings mSettings=new Settings();
    private SerialPort mPortDevice;
    private boolean mLocalized=false;


    private ArrayList<PointF> path=new ArrayList<>();
    private boolean mSavingPath=false;
    private double mPathStartRotation;
    private double mPathEndRotation;


    //////////////////////////////////////////////////////////////////////////////////////////////// path saving stuff
    public void startSavingPath(){
        if(!mSavingPath){
            mPathStartRotation=mYRot;
            mSavingPath=true;
            clearPath();
            addPath();
        }
    }
    public void stopSavingPath(){
        if(mSavingPath){
            mSavingPath=false;
            mPathEndRotation=mYRot;


            // most likely we already stopped motion before sending the stopSavingPath() command,
            // so if we don't remove the last point before sending the real last one, we end up
            // with two points on top of each other.
            if(mMovingState != Commands.FORWARD){
                //remove last point before adding new last point
                subtractPath(path.size() - 1);
            }
            //add last point
            addPath();
        }
    }

    private void subtractPath(int index){
        path.remove(index);
        resendPath();
    }
    private void clearPath(){
        path.clear();
        mMainAct.sendToRemoteClearTargets();
    }
    private void addPath() {
        // I think in this one we should check and only add a new point if it is at least
        // a certain distance from the last one:
        PointF newPt=mCurTranslation.toPointFXY();
            if(path.size()>0){
                PointF lastPt=path.get(path.size() - 1);
            float dist=new PointF(lastPt.x - newPt.x, lastPt.y - newPt.y).length();
            if(dist<.1f)//10 cm
                return;
        }

        path.add(newPt);
        mMainAct.sendToRemoteAddTarget(newPt);
    }
    private void resendPath(){
        mMainAct.sendToRemoteClearTargets();
        for(PointF pt:path)
            mMainAct.sendToRemoteAddTarget(pt);
    }

    public void tracePathForward(){
        if (path.size() == 0)
            return;
        mTargetList.clear();
        for(PointF p:path){
            Target t=new Target(new Vec3(p.x,p.y,0),0);
            if(p==path.get(path.size()-1))
                t.rot=mPathEndRotation;
            mTargetList.add(t);
        }
        mCurrentTarget = 0;
        changeMode(Modes.GOTOTARGET);
    }
    public void tracePathReverse(){
        if (path.size() == 0)
            return;
        mTargetList.clear();
        for (int i = path.size() - 1; i >= 0; i--) {
            PointF p=path.get(i);
            Target t=new Target(new Vec3(p.x,p.y,0),0);
            if(i==0)
                t.rot=mPathStartRotation;
            mTargetList.add(t);
        }
        mCurrentTarget = 0;
        changeMode(Modes.GOTOTARGET);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// Enumerations, inner classes
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


    private class Target{
        Vec3 pos;
        double rot;

        Target(Vec3 p, double r){
            pos=p;
            rot=r;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Constructor
    //constructor
    public Robot(MainActivity mainAct, SerialPort port){
        this.mMainAct=mainAct;
        mPortDevice=port;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// Public methods

    // main "callback" to give robot it's location and set it deciding what to do.
    public void setPose(Vec3 translation, double rotation){
        mCurTranslation=translation;
        mYRot=rotation;
        mMainAct.setRobotMap(translation, rotation);
        doYourStuff();
    }

    public void setLocalized(boolean localized){
        mLocalized=localized;
        sendCommand(mLocalized? Commands.BEEPLOWHI : Commands.BEEPHILOW);
    }

    public boolean isLocalized(){
        return mLocalized;
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
                break;
            case GOTOTARGET:
                if(mTargetList.size() == 0){
                    mMainAct.dump("No Targets");
                    return;
                }
                sendCommand(Commands.BEEPHI);
                mOnTarget=false;
                mOnTargetRot=false;
                mMainAct.speak("Going to target");
                mMainAct.dump("Going to Target " + mCurrentTarget);
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
        mMainAct.setStatusRobotMode(m+"");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// clearTargets() (outdated, not in ui)
    public void clearTargets(){
        mCurrentTarget=0;
        mTargetList.clear();
        changeMode(Modes.STOP);
        mMainAct.sendToRemoteClearTargets();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// addTarget() (outdated, not in ui)
    public void addTarget(){
        mMainAct.dump("Added Target " + mTargetList.size() + "\n");
        mTargetList.add(new Target(mCurTranslation, mYRot));
        mMainAct.speak("target recorded");
        mMainAct.sendToRemoteAddTarget(mCurTranslation.toPointFXY());
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// sendCommands section
    /////////////////////////
    // sendCommands()
    // This sends the serial commands.
    // if manual is true, command is sent even if the same as lastCommand,
    // and lastCommand is not updated.  This keeps the button-press commands(manual=true)
    // separate from the auto commands

    private void sendCommand(Commands c){
        sendCommandX(c, false);
    }

    public void sendManualCommand(Commands c){
        sendCommandX(c, true);
    }

    private void sendCommandX(Commands c, boolean force){

        if (mSavingPath && mMovingState == Commands.FORWARD) {
            switch (c) {
                case STOP:
                case SPINLEFT:
                case SPINRIGHT:
                case HALFLEFT:
                case HALFRIGHT:
                    addPath();
                    break;
            }
        }


        if(c == mMovingState && !force)
            return;
        if(c == Commands.FORWARD || c == Commands.STOP||c==Commands.REVERSE
                || c == Commands.SPINRIGHT || c == Commands.SPINLEFT
                || c == Commands.HALFRIGHT || c == Commands.HALFLEFT){
            mMovingState=c;
        }

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
            n=mPortDevice.send(new String(buf), 1000);
        }

        //output an info message
        if(n>0){
            mMainAct.dump(c + " sent");
            mMainAct.setStatusRobotState(mMovingState+"");
        }else{
            mMainAct.dump("Tried to send " + c);
            mMainAct.setStatusRobotState("~"+mMovingState);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// automation section
    //This is the interesting stuff, called each time the pose data is updated
    //this is called from pose listener thread, NOT UI
    private void doYourStuff(){

        //READY TO GO!!!
        switch(mMode){
            case GOTOTARGET:
                goToTarget();
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
    private void goToTarget() {
        try{
            //check for errors, just in case, shouldn't be "going to target" in this case
            if(mTargetList.size() == 0 || mCurrentTarget>=mTargetList.size()){
                changeMode(Modes.STOP);
                return;
            }

            if (mTargetList.size() > 0) {
                mToTarget = mTargetList.get(mCurrentTarget).pos.subtract(mCurTranslation);
            } else
                mToTarget = new Target(new Vec3(0, 0, 0), 0).pos.subtract(mCurTranslation);

            double dist = Math.sqrt(mToTarget.x * mToTarget.x + mToTarget.y * mToTarget.y);

            //On a target, or was, need to go to next target, or turn to proper rotation
            //if last target.  First check if the distance is too large and we are NOT on target
            if(mOnTarget){
                if (dist < mSettings.threshDistBig) { //on target don't move
                    if(mCurrentTarget == mTargetList.size() - 1){ // on last target
                        if(mUseTargetRotation && !mOnTargetRot)
                            changeDirection(mTargetList.get(mCurrentTarget).rot, 0, Commands.STOP);
                        if(mOnTargetRot || !mUseTargetRotation){
                            changeMode(Modes.STOP);
                            sendCommand(Commands.BEEPLOWHI);
                            mMainAct.dump("At Final Target");
                            mMainAct.speak("engaging final target");
                        }
                        return;//don't do anything else

                    }else{ // more targets to go
                        mMainAct.dump("Switched to Target " + mCurrentTarget);
                        sendCommand(Commands.BEEPLOWHI);
                        mCurrentTarget++;
                        mOnTarget=false;
                        mOnTargetRot=false;
                        mMainAct.speak("going to next target");
                        return;
                    }
                }else{// not on target
                    mOnTarget=false;
                    mOnTargetRot=false;
                }
            }

            //at this point maybe we got close enough to the target, set the flag and return
            if (dist < mSettings.threshDistSmall) { //on target stop (next update will start going to next target)
                //sendCommand(Commands.STOP);
                mOnTarget=true;
                mOnTargetRot=false;
                return;
            }

            //at this point still need to get closer.
            //strangely, changeDirection() works on this.
            double toAngle=Math.atan2(mToTarget.y, mToTarget.x);
            changeDirection(toAngle, dist, Commands.FORWARD);

        }catch(Exception e){
            mMainAct.dump("goToTarget() exception: " + e.getMessage());
        }
    }


    //changeDirection()
    // this one accepts a target direction and a command to run
    // after the target direction is aquired, forward or stop I guess
    private static final double TURNINGAPERDTHRESH = Math.PI / 4;
    private static final double FORWAPERDTHRESH = Math.PI;

    private void changeDirection(double targetDir, double dist, Commands afterCommand) {

        double turnAngle=makeAngleInProperRange(targetDir - mYRot);
        double mag=Math.abs(turnAngle);
        boolean needToTurnLeft=turnAngle>0; //need to turn left

        double anglePerDist = mag / dist;
        mMainAct.setStatusPoseData(new Vec3(mag, dist, anglePerDist), (float) turnAngle);

        switch(mMovingState){
            case STOP:
                if (mag > mSettings.threshAngleBig) {//need to change
                    mOnTargetRot = false;
                    sendCommand(needToTurnLeft ? Commands.SPINLEFT : Commands.SPINRIGHT);

                } else {
                    mOnTargetRot = true;
                    sendCommand(afterCommand);
                }
                break;

            case FORWARD:
                if (mag > mSettings.threshAngleSmall) {//need to change
                    mOnTargetRot=false;

                    if (mag < mSettings.threshAngleBig)
                        sendCommand(needToTurnLeft ? Commands.HALFLEFT : Commands.HALFRIGHT);
                    else
                        sendCommand(needToTurnLeft ? Commands.SPINLEFT : Commands.SPINRIGHT);
                }else{
                    mOnTargetRot=true;
                    sendCommand(afterCommand);
                }
                break;


            case SPINRIGHT:
                if(mag<mSettings.threshAngleSmall || needToTurnLeft){
                    if (!needToTurnLeft)
                        mOnTargetRot = true;
                    sendCommand(afterCommand);
                    break;
                }
                if (mag < mSettings.threshAngleBig) {
                    sendCommand(Commands.HALFRIGHT);
                    break;
                }
                break;

            // Halfturn same as spin except use threshhold divided by 2
            case HALFRIGHT:
                if (mag < (mSettings.threshAngleSmall / 2) || needToTurnLeft) {
                    if (!needToTurnLeft)
                        mOnTargetRot = true;
                    sendCommand(afterCommand);
                    break;
                }

                if (mag > mSettings.threshAngleBig) {
                    sendCommand(Commands.SPINRIGHT);
                }
                break;


            case SPINLEFT:
                if(mag<mSettings.threshAngleSmall || !needToTurnLeft){
                    if (needToTurnLeft)
                        mOnTargetRot = true;
                    sendCommand(afterCommand);
                    break;
                }
                if (mag < mSettings.threshAngleBig) {
                    sendCommand(Commands.HALFLEFT);
                    break;
                }
                break;

            case HALFLEFT:
                if (mag < (mSettings.threshAngleSmall / 2) || !needToTurnLeft) {
                    if (needToTurnLeft)
                        mOnTargetRot = true;
                    sendCommand(afterCommand);
                }

                if (mag > mSettings.threshAngleBig) {
                    sendCommand(Commands.SPINLEFT);
                }
                break;

        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// unused "Search" section, in theory what to do if localization lost
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

        long curTime=System.currentTimeMillis();

        if(curTime>gTriggerTime){
            switch(gSearchMode){
                case 0:
                    sendCommand(Commands.SPINRIGHT);
                    gSearchMode=1;
                    gTriggerTime=curTime + 3000;
                    break;
                case 1:
                    sendCommand(Commands.SPINLEFT);
                    gSearchMode=2;
                    gTriggerTime=curTime + 3000;
                    break;
                case 2:
                    switch(gCircleMode){
                        case 0:
                            sendCommand(Commands.FORWARD);
                            gTriggerTime=curTime + 1000 + (int)(gRadius*1000);
                            gCircleMode=1;
                            break;
                        case 1:
                            sendCommand(Commands.SPINLEFT);
                            gTriggerTime=curTime + (int)(1000/(gRadius));
                            gCircleMode=0;
                            gRadius*=1.5;
                            break;
                    }
                    break;
            }
        }
    }
}
