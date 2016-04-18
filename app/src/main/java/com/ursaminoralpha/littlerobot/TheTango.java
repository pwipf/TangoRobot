package com.ursaminoralpha.littlerobot;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.SystemClock;

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

import java.util.ArrayList;

import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;
import static com.ursaminoralpha.littlerobot.MathUtil.quaternionToAngle;
//import com.google.atap.tango.ux;

public class TheTango{
    private static MainActivity mMainAct;

    private static Tango mTango;
    private static boolean mTangoReady;
    private static boolean mIsTangoServiceConnected;
    private static boolean mPermissionsReady;
    private static boolean mLocalized;
    private static boolean mLearningMode;
    private static long mPreviousPoseTime;
    private static int mStatus;
    private static String mCurrentADFName;
    private static AsyncTask mSaveTask;
    private static AsyncTask mPermissionsTask;
    private static String mLastUUID;
    private Robot mRobot;

    TheTango(MainActivity mainActivity, final boolean learningMode, final String adfName, Robot robot){
        mMainAct=mainActivity;
        mRobot=robot;

        if(!Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
            mMainAct.startActivityForResult
                    (Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
        }

        mTango=new Tango(mMainAct, new Runnable(){
            @Override
            public void run(){
                mMainAct.dump("Tango Ready");
                mTangoReady=true;
            }
        });

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
                mPermissionsReady=true; // if no permissions, no adf, but still ready to go!
                startTangoWithADF(learningMode,adfName);
                return 0;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String getUUIDFromADFFileName(String fileName){
        if(Tango.hasPermission(mMainAct, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)){
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
        }
        return null;
    }

    private void startTangoWithADF(boolean learningMode, String adfFileName){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            startTangoWithAdfUUID(learningMode,getUUIDFromADFFileName(adfFileName));
        }
    }

    public void start(){
        startTangoWithAdfUUID(mLearningMode,mLastUUID);
    }
    //This starts the tango service
    //synchronized because of the mIsTangoServiceConnected flag
    private synchronized void startTangoWithAdfUUID(boolean learningMode, String adfUUID){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            String tempName;
            //set tango config
            TangoConfig config=mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, learningMode);
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,adfUUID==null? "" : adfUUID);

            // get the name of the ADF set in mConfig to store in mCurrentADFName
            String uuid=config.getString(TangoConfig.KEY_STRING_AREADESCRIPTION);
            if(uuid == null || uuid.length() == 0){
                tempName="NOT USING";
            }else{
                byte[] n=mTango.loadAreaDescriptionMetaData(uuid).get(TangoAreaDescriptionMetaData.KEY_NAME);
                tempName= (n==null? "NOT FOUND" : new String(n));
            }

            // try to set listeners and start service
            try{
                setTangoListeners();
                mTango.connect(config);
                // set some info
                mIsTangoServiceConnected=true;
                mLastUUID=adfUUID;
                mCurrentADFName=tempName;
                mLearningMode=learningMode;
                mMainAct.setLearningStatus(learningMode);
            }catch(TangoOutOfDateException e){
                mMainAct.dump("Tango Service out of date!");
            }catch(TangoErrorException e){
                mMainAct.dump("Tango Error! Restart the app!");
            }
        }
    }


    public void stopTango(){
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
        try{
            mTango.disconnect();
            mIsTangoServiceConnected=false;
            mLearningMode=false;
            mMainAct.setLearningStatus(mLearningMode);
            mMainAct.dump("Tango Service Disconnect");
        }catch(TangoErrorException e){
            mMainAct.dump("Tango Error!");
        }
    }

    void restartTango(boolean learningMode, String adfUUID){
        stopTango();
        startTangoWithAdfUUID(learningMode,adfUUID);
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
                }

                if(pose.statusCode!=mStatus){
                    mStatus=pose.statusCode;
                    changed=true;
                    mMainAct.dump("Status: " + statusText(mStatus));
                }

                if(changed){
                    mMainAct.mStatusFrag.localized(mLocalized);
                    mMainAct.mStatusFrag.poseStatus(statusText(mStatus));
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
                    mMainAct.mStatusFrag.setPose(translation,rot);

                    //update robot localization status if changed
                    if(mLocalized != mRobot.isLocalized()){
                        mRobot.setLocalized(mLocalized);
                    }
                    mRobot.setPose(translation,rot);
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0){
                // Ignoring XyzIj data
            }

            @Override
            public void onTangoEvent(final TangoEvent e){
                if(e.eventType!=TangoEvent.EVENT_FISHEYE_CAMERA){
                    mMainAct.dump(e.eventKey + ": " + e.eventValue);
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
                return "INITIALIZING";
            case TangoPoseData.POSE_INVALID:
                return "INVALID";
            case TangoPoseData.POSE_VALID:
                return "VALID";
            case TangoPoseData.POSE_UNKNOWN:
                return "UNKNOWN";
        }
        return "OTHER";
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
                        mMainAct.dump("Saved ADF as: " + fileName);
                    }
                    mMainAct.dump("Finished Saving, restarting...");
                    restartTango(false, uuid);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public void startLearnADFmode(String loadADFuuid){
        restartTango(true,loadADFuuid);
    }

    public void stopLearnADFmode(String loadADFuuid){
        restartTango(false,loadADFuuid);
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
