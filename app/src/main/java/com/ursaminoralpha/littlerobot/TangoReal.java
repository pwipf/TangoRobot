package com.ursaminoralpha.littlerobot;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import com.projecttango.tangosupport.*;


import java.util.ArrayList;

import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;
import static com.ursaminoralpha.littlerobot.MathUtil.quaternionToAngle;
//import com.google.atap.tango.ux;

public class TangoReal{
    private static MainActivity mMainAct;

    private static Tango mTango;
    private static TangoCameraIntrinsics mIntrinsics;
    private static TangoPoseData mLatestPose;
    private static boolean mTangoReady;
    private static boolean mIsTangoServiceConnected;
    private static boolean mPermissionsReady;
    private static boolean mLocalized;
    private static boolean mLearningMode;
    private static boolean mDepthMode;

    private static long mPreviousPoseTime;
    private static int mStatus;
    private static String mCurrentADFName;
    private static AsyncTask mSaveTask;
    private static AsyncTask mPermissionsTask;
    private static String mLastUUID;
    private Robot mRobot;

    double mLatestTimeStamp;
    TangoPointCloudManager mCloudManager;
    public static final TangoCoordinateFramePair FRAME_PAIR = new TangoCoordinateFramePair(
            TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
            TangoPoseData.COORDINATE_FRAME_DEVICE);

    TangoReal(MainActivity mainActivity, final boolean learningMode, boolean depthMode, final String currentUUID, Robot robot){
        mMainAct=mainActivity;
        mRobot=robot;
        mLastUUID=currentUUID;
        mLearningMode=learningMode;
        mDepthMode=depthMode;
        mCloudManager = new TangoPointCloudManager();
        Log.w("???", "???");

        if(!Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            mMainAct.startActivityForResult
                    (Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
        }


//        mPermissionsReady=true;
//        mTangoReady=true;
//        mTango=new Tango(mMainAct);
//        mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
//        mLastUUID=getUUIDFromADFFileName(adfName);

        mTango=new Tango(mMainAct, new Runnable(){
            @Override
            public void run(){
                mMainAct.dump("Tango Ready");
                mTangoReady=true;
            }
        });


        //Why??? does not find any saved UUIDs later without this line. I don't know why.
        mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        mPermissionsTask = new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params){
                mMainAct.dump("Waiting for ADF permission...");
                while(!mTangoReady && !isCancelled()){
                    SystemClock.sleep(50);
                }
                while(!Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)
                        && !isCancelled()){
                    SystemClock.sleep(50);
                }
                if(!Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
                    mMainAct.dump("Could not get ADF Permission");
                }
                mPermissionsReady=true;
                startTangoWithAdfUUID(mLearningMode,mDepthMode,mLastUUID);
                return 0;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String getUUIDFromADFFileName(String fileName){
        mMainAct.dump("looking for adf from uuid");
        //if(Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            ArrayList<String> fullUUIDList=mTango.listAreaDescriptions();
            if(fullUUIDList.size()>0){
                for(String uuid : fullUUIDList){
                    TangoAreaDescriptionMetaData metadata=mTango.loadAreaDescriptionMetaData(uuid);
                    final String name=new String(metadata.get(TangoAreaDescriptionMetaData.KEY_NAME));
                    if(name.equals(fileName)){
                        mMainAct.dump("Found ADF");
                        return uuid;
                    }
                }
                mMainAct.dump("Could not find ADF " + fileName);
            }else
                mMainAct.dump("NO ADFs Found");
        return null;
        }



    public void startTangoWithADF(boolean learningMode, boolean depthMode, String adfFileName){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            startTangoWithAdfUUID(learningMode,depthMode,getUUIDFromADFFileName(adfFileName));
        }
    }

    public void start(){
        startTangoWithAdfUUID(mLearningMode,mDepthMode,mLastUUID);
        mMainAct.dump("Starting with learning: "+mLearningMode+", depth: "+mDepthMode);
        mMainAct.dump("UUID: "+mLastUUID);
    }

    //This starts the tango service
    private void startTangoWithAdfUUID(boolean learningMode, boolean depthMode, String adfUUID){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            //set tango config
            TangoConfig config=mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, learningMode);
            config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH,depthMode);
            config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, depthMode);
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,adfUUID==null? "" : adfUUID);

            // get the name of the ADF set in mConfig to store in mCurrentADFName
            String tempName;
            String uuid=config.getString(TangoConfig.KEY_STRING_AREADESCRIPTION);
            if(uuid == null || uuid.length() == 0){
                tempName="NOT USING";
            }else{
                byte[] n=mTango.loadAreaDescriptionMetaData(uuid).get(TangoAreaDescriptionMetaData.KEY_NAME);
                tempName= (n==null? "NOT FOUND" : new String(n));
            }
            mMainAct.setStatusADFName(tempName);

            // try to set listeners and start service
            try{
                setTangoListeners();
                mTango.connect(config);
                // set some info

                mIsTangoServiceConnected=true;
                mLastUUID=adfUUID;
                mCurrentADFName=tempName;
                mLearningMode=learningMode;
                mLocalized = false;
                mStatus = TangoPoseData.POSE_UNKNOWN;
                mMainAct.setLearningStatus(learningMode);
            }catch(TangoOutOfDateException e){
                mMainAct.dump("Tango Service out of date!");
            }catch(TangoErrorException e){
                mMainAct.dump("Tango Error! Restart the app!");
            }
        }
    }


    public void stop(){
        if (mProbeThread != null) {
            mProbeThread.interrupt();
        }
        disconnectTango();
        if(mPermissionsTask!=null && mPermissionsTask.getStatus()!=AsyncTask.Status.FINISHED)
            mPermissionsTask.cancel(true);
        if(mIsTangoServiceConnected){
            if(mSaveTask != null && mSaveTask.getStatus() != AsyncTask.Status.FINISHED){
                new AsyncTask<Void,Void,Void>(){
                    @Override
                    protected Void doInBackground(Void... params){
                        while(mSaveTask.getStatus() != Status.FINISHED)
                            SystemClock.sleep(100);
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void integer){
                        disconnectTango();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else{
                disconnectTango();
            }
        }
    }
    private void disconnectTango(){
        if(mIsTangoServiceConnected) {
            try {
                mTango.disconnect();
                mIsTangoServiceConnected = false;
                mLearningMode = false;
                mMainAct.setLearningStatus(mLearningMode);
                mMainAct.dump("Tango Service Disconnect");
            } catch (TangoErrorException e) {
                mMainAct.dump("Tango Error!");
            }
        }
    }

    void restartTango(boolean learningMode, boolean depthMode, String uuid){
        stop();
        startTangoWithAdfUUID(learningMode,depthMode,uuid);
    }


    private void setTangoListeners(){
        mMainAct.dump("setTangoListeners");
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
                long currentTime=(long)(pose.timestamp*1000);

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
                    mMainAct.dump("Localized: " + mLocalized);
                    mMainAct.speak(mLocalized ? "Localized" : "Localization Lost");

                    if(mLocalized && mLearningMode){
                        mRobot.clearObstacles();
                        mRobot.clearTargets();
                        mRobot.stopSavingPath();
                        mRobot.startSavingPath();
                    }
                }

                if(pose.statusCode!=mStatus){
                    mStatus=pose.statusCode;
                    changed=true;
                    mMainAct.dump("Status: " + statusText(mStatus));
                }

                if(changed){
                    mMainAct.setStatusTango(mLocalized,statusText(mStatus));
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


                // Throttle updates to Robot
                if(currentTime>=mPreviousPoseTime+mRobot.mSettings.updateInterval){
                    mPreviousPoseTime=currentTime;

                    //convert quaternion to simple azimuth (z rotation);
                    double rot=quaternionToAngle(pose.rotation, 1);
                    //need to add 90 to rotation to line up with x-axis (basis for atan2(y,x) returned angles)
                    rot=makeAngleInProperRange(rot + Math.PI/2);

                    Vec3 translation=new Vec3(pose.translation);

                    //mMainAct.setStatusPoseData(translation,(float)rot);

                    //update robot localization status if changed
                    if(mLocalized != mRobot.isLocalized()){
                        mRobot.setLocalized(mLocalized);
                    }
                    //mLatestPose=pose;
                    mRobot.setPose(translation,rot);
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0){


                mCloudManager.updateXyzIj(arg0);
                mLatestTimeStamp = arg0.timestamp;


//                float[] dpt=TangoSupport.getDepthAtPointNearestNeighbor(arg0,mIntrinsics,mLatestPose,0.5f,0.5f);
//
//                if(dpt !=null) {
//                    mMainAct.sendToRemoteDepth(0.5f, 0.5f, dpt[2]);
//                    Log.w("XYZIJ ", "dpt: " + dpt[0] + " " + dpt[1] + " " + dpt[2]);
//                }

            }

            @Override
            public void onTangoEvent(final TangoEvent e){
                if(e.eventType!=TangoEvent.EVENT_FISHEYE_CAMERA && e.eventType!=TangoEvent.EVENT_FEATURE_TRACKING){
                    mMainAct.dump(e.eventKey + ": " + e.eventValue);
                }
            }

            @Override
            public void onFrameAvailable(int arg0){
                // Ignoring onFrameAvailable Events
            }

        });

        mIntrinsics = mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

        mMainAct.dump("Finished SetTangoListeners, Starting Prober");

        mProbeThread = new Thread(new DepthProberTask());
        mProbeThread.start();
    }

    boolean mProbing;
    Thread mProbeThread;

    class DepthProberTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {//Log.w("TAG", "probe");
                float u = 0, v = 0;
                Vec3 vec = null;
                for (int i = 1; i < 10; i++) {

                    u=i*.1f;
                    float z = 0;
                    float uavg=0;
                    int count=0;
                    //for (int j = 1; j < 10; j++) {
                    //v=.9f-(j*.01f);
                    float w;
                    for(int k=-5;k<5;k++){
                        w=k*.01f;
                        vec = (getDepthAtPosition(u+w, mRobot.mSettings.obstacleHeight, mLatestTimeStamp));
                        if (vec != null) {
                            z += vec.z;
                            uavg+=(vec.x);
                            count++;
                        }
                    }
                    //}
                    if(count>0) {
                        z /= count;
                        uavg/=count;
                        mMainAct.sendToRemoteDepth(uavg,v,z);

                        if(z<1){
                            mMainAct.addObstacle(uavg,z);
                        }
                    } else {
                        mMainAct.sendToRemoteDepth(u,v,0);
                    }
                }
                SystemClock.sleep(500);
            }//end while loop
            mProbing = false;
        }
    }

    private Vec3 getDepthAtPosition(float u, float v, double rgbTimestamp) {
        TangoXyzIjData xyzIj = mCloudManager.getLatestXyzIj();
        if (xyzIj == null) {
            return null;
        }

        // We need to calculate the transform between the color camera at the
        // time the user clicked and the depth camera at the time the depth
        // cloud was acquired.
        try {
            TangoPoseData colorTdepthPose = TangoSupport.calculateRelativePose(
                    rgbTimestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                    xyzIj.timestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH);
            float[] point = TangoSupport.getDepthAtPointNearestNeighbor(xyzIj, mIntrinsics,
                    colorTdepthPose, u, v);
            if (point == null) {

                return null;
            }
            return new Vec3(point[0], point[1], point[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

//        return ScenePoseCalculator.getPointInEngineFrame(
//                new Vector3(point[0], point[1], point[2]),
//                ScenePoseCalculator.matrixToTangoPose(mExtrinsics.getDeviceTDepthCamera()),
//                mTango.getPoseAtTime(rgbTimestamp, FRAME_PAIR));
    }


    //helper function
    public String statusText(int status){
        switch(status){
            case TangoPoseData.POSE_INITIALIZING:
                return "INITIALIZING";
            case TangoPoseData.POSE_INVALID:
                return "INVALID";
            case TangoPoseData.POSE_VALID:
                return "VALID";
            default:
                return "UNKNOWN";
        }
    }

    public void saveADF(final String fileName){
        if(mLearningMode && mLocalized){
            mSaveTask=new AsyncTask<Void,Integer,String>(){
                @Override
                protected String doInBackground(Void... params){
                    mMainAct.dump("Saving Area Description...");
                    try{
                        return mTango.saveAreaDescription();
                    }catch(TangoErrorException e){
                        e.printStackTrace();
                        mMainAct.dump("saveADF() Excp: " + e.getMessage());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String uuid){
                    //save name of file
                    if(uuid != null){
                        TangoAreaDescriptionMetaData meta=mTango.loadAreaDescriptionMetaData(uuid);
                        meta.set(TangoAreaDescriptionMetaData.KEY_NAME, fileName.getBytes());
                        mTango.saveAreaDescriptionMetadata(uuid, meta);
                        mLastUUID=uuid;
                        mMainAct.mCurrentUUID=mLastUUID;
                        mMainAct.runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                mMainAct.writePrefs();
                            }
                        });
                        mMainAct.dump("Saved ADF as: " + fileName);
                    }
                    mMainAct.dump("Finished Saving, restarting...");
                    restartTango(false,mDepthMode, mLastUUID);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public void startLearnADFmode(String loadADFuuid){
        restartTango(true,false,loadADFuuid);
    }

    public void stopLearnADFmode(){
        mRobot.stopSavingPath();
        restartTango(false,mDepthMode,mLastUUID);
    }

    public void setDepthMode(boolean on){
        if(on == mDepthMode)
            return;
        restartTango(mLearningMode,on,mLastUUID);
    }

    // getters
    public static String getCurrentADFName(){
        return mCurrentADFName;
    }

    public boolean isLearningMode(){
        return mLearningMode;
    }

    public static String getCurrentAdfUUID(){
        return mLastUUID;
    }
}
