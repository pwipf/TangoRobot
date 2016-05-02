package com.ursaminoralpha.littlerobot;


import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.SystemClock;

import java.io.IOException;
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
    public ArrayList<String> pathNames = new ArrayList<>();
    private ArrayList<Float> pathRotations = new ArrayList<>();
    private boolean mSavingPath=false;
    private double mPathStartRotation;
    private double mPathEndRotation;

    //private ArrayList<PointF> mObstacleList=new ArrayList<>();

    private static final int NOBST = 30;
    private PointF[] mObstacleList = new PointF[NOBST];
    private int obstListIndex = 0;

    private long mEngageTime;
    private static final long mEngageGoTime = 2000;

    private Object sendLock = new Object();
    private PointF mPtInFront = new PointF(0, 0);
    private static final float OBSTHRESH = .1f;


    //////////////////////////////////////////////////////////////////////////////////////////////// path saving stuff
    public void startSavingPath(){
        if(!mSavingPath){
            mSavingPath=true;
            clearPath();
            addPath();
        }
    }
    public void stopSavingPath(){
        if(mSavingPath){
            mSavingPath=false;

        }
    }

    private void subtractPath(int index){
        path.remove(index);
        pathNames.remove(index);
        pathRotations.remove(index);
        resendPath();
    }
    private void clearPath(){
        path.clear();
        pathNames.clear();
        pathRotations.clear();
        mMainAct.clearTargets();
    }
    private void addPath(){addPath(null);}
    private void addPath(String name) {
        // I think in this one we should check and only add a new point if it is at least
        // a certain distance from the last one:
        PointF newPt=mCurTranslation.toPointFXY();
        float rot = (float) mYRot;
            if(path.size()>0){
                PointF lastPt=path.get(path.size() - 1);
            float dist=new PointF(lastPt.x - newPt.x, lastPt.y - newPt.y).length();
                if (dist < .4f && name == null) {//10 cm
                return;
            }
            if(dist<.1f){
                subtractPath(path.size()-1);
            }
        }

        if(name==null)
            name=path.size()+"";
        path.add(newPt);
        pathNames.add(name);
        pathRotations.add(rot);
        mMainAct.addTarget(newPt,name);
    }

    public void saveLocation(String name){
        addPath(name);
        mMainAct.speak(name + " recorded");
    }

    private void resendPath(){
        mMainAct.clearTargets();
        for(int i=0;i<path.size();i++)
            mMainAct.addTarget(path.get(i),pathNames.get(i));
    }

    public void tracePathForward(int start) {
        if (path.size() == 0)
            return;

        mTargetList.clear();

        for (int i = start; i < path.size(); i++) {
            Target t = new Target(new Vec3(path.get(i).x, path.get(i).y, 0), pathRotations.get(i), pathNames.get(i));
            mTargetList.add(t);
        }
        mCurrentTarget = 0;
        changeMode(Modes.GOTOTARGET);
    }

    public void tracePathReverse(int start) {
        if (path.size() == 0)
            return;

        mTargetList.clear();

        for (int i = start; i >= 0; i--) {
            PointF p=path.get(i);
            Target t = new Target(new Vec3(p.x, p.y, 0), pathRotations.get(i), pathNames.get(i));
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
        STOP("STOP"), GOTOTARGET("GOTOTARGET"), SEARCHLOC("SEARCH"), ENGAGE("ENGAGE"),
        DISENGAGE("DISENGAGE"), WAITFOROBST("WAITFOROBST");
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
        String name;

        Target(Vec3 p, double r, String n){
            pos=p;
            rot=r;
            name=n;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Constructor
    //constructor
    public Robot(MainActivity mainAct, SerialPort port){
        this.mMainAct=mainAct;
        mPortDevice=port;

        for (int i = 0; i < NOBST; i++) {
            mObstacleList[i] = new PointF(0, 0);
        }
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
            case ENGAGE:
                sendCommand(Commands.FORWARD);
                mEngageTime = System.currentTimeMillis();
                break;
            case DISENGAGE:
                sendCommand(Commands.REVERSE);
                mEngageTime = System.currentTimeMillis();
                break;
            case WAITFOROBST:
                sendCommand(Commands.STOP);
                mMainAct.speak("Something in the way");
                break;
            case STOP:
                sendManualCommand(Commands.STOP);
                break;
            case GOTOTARGET:
                if(mTargetList.size() == 0){
                    mMainAct.dump("No Targets");
                    return;
                }
                //sendCommand(Commands.BEEPHI);
                mOnTarget=false;
                mOnTargetRot=false;
                mMainAct.speak("Going to " + mTargetList.get(mTargetList.size() - 1).name);
                //mMainAct.dump("Going to Target " + mCurrentTarget);
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
        mMainAct.clearTargets();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// addTarget() (outdated, not in ui)
    public void addTarget(String name){
        mMainAct.dump("Added Target " + mTargetList.size() + "\n");
        mTargetList.add(new Target(mCurTranslation, mYRot,name));
        mMainAct.speak("target recorded");
        mMainAct.addTarget(mCurTranslation.toPointFXY(),name);
    }

    PointF extendUZ(PointF uz, float curRot, PointF current) {
        float[] pt = {uz.x, uz.y};
        Matrix rot = new Matrix();
        rot.preRotate((float) Math.toDegrees(curRot) - 90);
        rot.mapPoints(pt);
        PointF ext = new PointF(pt[0], pt[1]);
        ext.x += current.x;
        ext.y += current.y;
        return ext;
    }

    public void addObstacle(float u, float z){
        //u z is direct from tango. u = .5 is straight ahead, z= distance ahead

        PointF obst = extendUZ(new PointF(u, z), (float) mYRot, mCurTranslation.toPointFXY());

//        for(PointF p: mObstacleList){
//            float dist = (float)Math.sqrt(Math.pow(p.x-obst.x,2) + Math.pow(p.y-obst.y,2));
//            if(dist < .02f)
//                return;
//        }

        //point not found in list
        //mObstacleList.add(obst);
        mObstacleList[obstListIndex] = new PointF(u, z);
        obstListIndex++;
        if (obstListIndex == NOBST) obstListIndex = 0;
        mMainAct.addObstacle(obst.x, obst.y);

    }
    public void clearObstacles(){
        //mObstacleList.clear();
        for (int i = 0; i < NOBST; i++)
            mObstacleList[i].set(0, 0);
        mMainAct.clearedObstacles();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// GO TO LOCATION
    public void goToLocation(String name){
        for(int i=0;i<pathNames.size();i++){
            if(pathNames.get(i).equals(name)){
                goToLocation(i);
            }
        }
    }

    public void goToLocation(int pathIndex) {

        stopSavingPath();

        PointF curpt = mCurTranslation.toPointFXY();

        PointF closest = path.get(0);
        int j = 0;
        for (int i = 1; i < path.size(); i++) {
            if (new PointF(curpt.x - path.get(i).x, curpt.y - path.get(i).y).length() <
                    new PointF(curpt.x - closest.x, curpt.y - closest.y).length()) {
                closest = path.get(i);
                j = i;
            }
        }


        if (pathIndex > j) {

            tracePathForward(j);

        } else if (pathIndex < j) {

            tracePathReverse(j);
        }
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

        /// TODO REMOVE
//        if(c == Commands.FORWARD || c == Commands.STOP||c==Commands.REVERSE
//                || c == Commands.SPINRIGHT || c == Commands.SPINLEFT
//                || c == Commands.HALFRIGHT || c == Commands.HALFLEFT){
//            mMovingState=c;
//        }
        ////


        if(c == mMovingState && !force)
            return;

        byte buf[] = new byte[4];
        int n=0;
        String com = "";
        if(mPortDevice.isOpen()){
            switch(c){
                case FORWARD:
                    buf[0]='w';
                    buf[1] = 8;
                    buf[2] = 'e';
                    buf[3] = 8;
                    break;
                case SPINLEFT:
                    buf[0]='e';
                    buf[1] = 2;
                    buf[2] = 'x';
                    buf[3] = 2;
                    break;
                case HALFLEFT:
                    buf[0]='e';
                    buf[1] = 2;
                    buf[2] = 's';
                    buf[3] = 0;
                    break;
                case SPINRIGHT:
                    buf[0]='w';
                    buf[1] = 2;
                    buf[2] = 'c';
                    buf[3] = 2;
                    break;
                case HALFRIGHT:
                    buf[0]='w';
                    buf[1] = 2;
                    buf[2] = 'd';
                    buf[3] = 0;
                    break;
                case STOP:
                    buf[0]='s';
                    buf[1] = 0;
                    buf[2] = 'd';
                    buf[3] = 0;
                    break;
                case REVERSE:
                    buf[0]='x';
                    buf[1] = 2;
                    buf[2] = 'c';
                    buf[3] = 2;
                    break;
                case BEEPHILOW:
                    buf[0]='r';
                    buf[1] = 16;
                    buf[2] = 't';
                    buf[3] = 16;
                    break;
                case BEEPLOWHI:
                    buf[0]='t';
                    buf[1] = 16;
                    buf[2] = 'r';
                    buf[3] = 16;
                    break;
                case BEEPHI:
                    buf[0]='r';
                    buf[1] = 16;
                    buf[2] = ' ';
                    buf[3] = 16;
                    break;
                case BEEPLOW:
                    buf[0]='t';
                    buf[1] = 16;
                    buf[2] = ' ';
                    buf[3] = 16;
                    break;
            }

            // actually send the command over the port

            try {
                synchronized (sendLock) {
                    n = mPortDevice.mPort.write(buf, 1000);
                }
            } catch (IOException e) {
                mMainAct.dump(e.getMessage());
            }
        } else {
            mMainAct.dump("Port not open");
        }

        //output an info message
        if(n>0){
            mMainAct.dump(c + " sent");
            //mMainAct.setStatusRobotState(mMovingState+"");
            if (c == Commands.FORWARD || c == Commands.STOP || c == Commands.REVERSE
                    || c == Commands.SPINRIGHT || c == Commands.SPINLEFT
                    || c == Commands.HALFRIGHT || c == Commands.HALFLEFT) {
                mMovingState = c;
            }
        }else{
            mMainAct.dump("Tried to send " + c);
            //mMainAct.setStatusRobotState("~"+mMovingState);
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

            case WAITFOROBST:

                int count = 0;
                for (PointF p : mObstacleList) {
                    if (p.x == 0 && p.y == 0)
                        continue;
                    count++;
                }
                if (count < 1) {
                    mMainAct.speak("Path clear");
                    changeMode(Modes.GOTOTARGET);
                }

                break;

            case STOP:
                switch (mMovingState) {
                    case FORWARD:
                        int npts = 0;
                        for (PointF p : mObstacleList) {
                            if (p.x == 0 && p.y == 0)
                                continue;
                            npts++;
                        }
                        if (npts > 5)
                            changeMode(Modes.WAITFOROBST);
                        break;
                }
                break;

            case SEARCHLOC:

                break;

            case ENGAGE:
                if (System.currentTimeMillis() > mEngageTime + mEngageGoTime) {
                    sendCommand(Commands.STOP);
                    changeMode(Modes.DISENGAGE);
                }
                break;
            case DISENGAGE:
                if (System.currentTimeMillis() > mEngageTime + mEngageGoTime) {
                    sendCommand(Commands.STOP);
                    changeMode(Modes.STOP);
                }
                break;
        }
    }

    private void engageTarget() {
        changeMode(Modes.ENGAGE);
    }

    // logic to drive toward target
    private void goToTarget() {
        try{
            if (mTargetList.size() > 0) {
                mToTarget = mTargetList.get(mCurrentTarget).pos.subtract(mCurTranslation);
            } else
                mToTarget = new Target(new Vec3(0, 0, 0), 0,"").pos.subtract(mCurTranslation);

            double dist = Math.sqrt(mToTarget.x * mToTarget.x + mToTarget.y * mToTarget.y);


            // look for obstacles
            if (mMainAct.mTango.mDepthMode && mMovingState == Commands.FORWARD) {

//                float distAhead=.7f;
//                mPtInFront = extendUZ(new PointF(0,distAhead),(float)mYRot,mCurTranslation.toPointFXY());

                int count = 0;
                for (PointF p : mObstacleList) {
                    if (p.x == 0 && p.y == 0)
                        continue;
                    count++;
                }
                if (count > 3) {
                    float distToNext = ptDist(mCurTranslation.toPointFXY(), mTargetList.get(mCurrentTarget).pos.toPointFXY());
                    if ((distToNext < .6f && mTargetList.get(mCurrentTarget).name.equalsIgnoreCase("target")
                            || distToNext < .2f)) {
                        //ignore the obstacles
                    } else {
                        changeMode(Modes.WAITFOROBST);
                        return;
                    }
                }
            }


            if(mOnTarget){
                if (dist <= mSettings.threshDistBig) { //on target don't move
                    if (mCurrentTarget == mTargetList.size() - 1) { // on last target
                        if(mUseTargetRotation && !mOnTargetRot)
                            changeDirection(mTargetList.get(mCurrentTarget).rot, 0, Commands.STOP);
                        if(mOnTargetRot || !mUseTargetRotation){
                            changeMode(Modes.STOP);
                            //sendCommand(Commands.BEEPLOWHI);

                            if(mTargetList.get(mCurrentTarget).name.equals("Target")){
                                mMainAct.dump("At Final Target");
                                mMainAct.speak("engaging final target");
                                engageTarget();
                            }
                        }
                        return;//don't do anything else

                    }else{ // more targets to go
                        mMainAct.dump("Switched to Target " + mCurrentTarget);
                        //sendCommand(Commands.BEEPLOWHI);
                        mCurrentTarget++;
                        mOnTarget=false;
                        mOnTargetRot=false;
                        //mMainAct.speak("going to next target");
                        return;
                    }
                }else{// not on target
                    mOnTarget=false;
                    mOnTargetRot=false;
                }
            }

            //at this point maybe we got close enough to the target, set the flag and return
            if (dist < mSettings.threshDistSmall ||
                    (mCurrentTarget != mTargetList.size() - 1 && dist < mSettings.threshDistBig)) { //on target stop (next update will start going to next target)
                sendCommand(Commands.STOP);
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

    //changed all the spins to halfturns so now there is lots of repeats and unnecessary redundancy

    private void changeDirection(double targetDir, double dist, Commands afterCommand) {

        double turnAngle=makeAngleInProperRange(targetDir - mYRot);
        double mag=Math.abs(turnAngle);
        boolean needToTurnLeft=turnAngle>0; //need to turn left

        double anglePerDist = mag / dist;
        mMainAct.setStatusPoseData(new Vec3(mag, dist, anglePerDist), (float) turnAngle);

        switch(mMovingState){
            case STOP:
                if (mag > mSettings.threshAngleBig) {//need to change direction
                    mOnTargetRot = false;
                    if (dist == 0 || mag > Math.PI / 2)
                        sendCommand(needToTurnLeft ? Commands.SPINLEFT : Commands.SPINRIGHT);
                    else
                        sendCommand(needToTurnLeft ? Commands.HALFLEFT : Commands.HALFRIGHT);


                } else {
                    mOnTargetRot = true;
                    sendCommand(afterCommand);
                }
                break;

            case FORWARD:
                if (mag > mSettings.threshAngleSmall) {//need to change
                    mOnTargetRot=false;

                    if (dist == 0)
                        sendCommand(needToTurnLeft ? Commands.SPINLEFT : Commands.SPINRIGHT);
                    else
                        sendCommand(needToTurnLeft ? Commands.HALFLEFT : Commands.HALFRIGHT);
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
                    sendCommand(Commands.HALFRIGHT);
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
                    sendCommand(Commands.HALFLEFT);
                }
                break;

        }
    }

    float ptDist(PointF a, PointF b) {
        return new PointF(a.x - b.x, a.y - b.y).length();
    }

}
