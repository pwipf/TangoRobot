package com.ursaminoralpha.littlerobot;


        import android.content.Context;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.os.AsyncTask;
        import android.os.SystemClock;
        import android.util.Log;


        import java.util.ArrayList;
        import java.util.List;

        import static com.ursaminoralpha.littlerobot.MathUtil.makeAngleInProperRange;
        import static com.ursaminoralpha.littlerobot.MathUtil.quaternionToAngle;
//import com.google.atap.tango.ux;

public class TangoFake implements SensorEventListener{
    private static MainActivity mMainAct;

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
    private static String mLastADFFilename;
    private Robot mRobot;

    SensorManager mSensorManager;
    private final Sensor mAccel,mRotSensor;

    TangoFake(MainActivity mainActivity, final boolean learningMode,boolean depth, final String adfName, Robot robot){
        mMainAct=mainActivity;
        mRobot=robot;
        mTangoReady=true;
        mPermissionsReady=true;

                mMainAct.dump("Tango Ready");
                mTangoReady=true;
                 mPermissionsTask = new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params){
                mMainAct.dump("Waiting for ADF permission...");
                    SystemClock.sleep(50);
                    SystemClock.sleep(50);
                mPermissionsReady=true;
                startTangoWithADF(learningMode,adfName);
                return 0;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        mSensorManager=(SensorManager)mMainAct.getSystemService(Context.SENSOR_SERVICE);
        mAccel=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if(mRotSensor==null)
            mMainAct.dump("null rotation");
        if(mAccel==null)
            mMainAct.dump("null accel");
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    float[] gravity=new float[3];
    float[] linear_acceleration=new float[3];
    boolean newT,newR;
    double rot;
    Vec3 pos=new Vec3(0,0,0);
    @Override
    public void onSensorChanged(SensorEvent event){
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        if(event.sensor==mRotSensor){
            rot=event.values[0]/10;
        }
        if(event.sensor==mAccel){
            final float alpha = 0.8f;

            float factor=.1f;

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]*factor;
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]*factor;
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]*factor;

            linear_acceleration[0] = (event.values[0]*factor - gravity[0]);
            linear_acceleration[1] = (event.values[1]*factor - gravity[1]);
            linear_acceleration[2] = (event.values[2]*factor - gravity[2]);
            newT=true;
            pos=new Vec3(linear_acceleration[1]+pos.x,-linear_acceleration[0]+pos.y,linear_acceleration[2]+pos.z);
       }
        if(newT){
            mMainAct.setStatusPoseData(pos,(float)rot);
            mRobot.setPose(pos,rot);
            mMainAct.sendToRemote(pos,(float)rot);
            for(int i=1;i<10;i++){
                float u=1.0f/10*i;
                mMainAct.sendToRemoteDepth(u,0.5f,.5f);
            }

            newT=false;
            //newR=false;
        }
        //double rot=quaternionToAngle(pose.rotation, 1);
        //need to add 90 to rotation to line up with x-axis (basis for atan2(y,x) returned angles)
       // rot=makeAngleInProperRange(rot + Math.PI/2);



    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    public String getUUIDFromADFFileName(String fileName){
        mMainAct.dump("looking for adf from uuid");
                    mMainAct.dump("Found ADF");
        return null;
    }



    public void startTangoWithADF(boolean learningMode, String adfFileName){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            startTangoWithAdfUUID(learningMode,getUUIDFromADFFileName(adfFileName));
        }
    }

    public void start(){
        startTangoWithAdfUUID(mLearningMode,mLastUUID);
        mMainAct.dump("Starting with learning: "+mLearningMode);
        mMainAct.dump("UUID: "+mLastUUID);

    }


    //This starts the tango service
    private void startTangoWithAdfUUID(boolean learningMode, String adfUUID){
        if(!mIsTangoServiceConnected && mPermissionsReady){
            mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME );
            mSensorManager.registerListener(this, mRotSensor, SensorManager.SENSOR_DELAY_NORMAL);
            //set tango config
            // get the name of the ADF set in mConfig to store in mCurrentADFName
            String tempName;
            tempName="NOT USING";
            mMainAct.setStatusADFName(tempName);
            Log.w("TAG","start tango "+tempName);

            // try to set listeners and start service

            setTangoListeners();
            // set some info
            mIsTangoServiceConnected=true;
            mLastUUID=adfUUID;
            mCurrentADFName=tempName;
            mLearningMode=learningMode;
            mLocalized = false;
            mMainAct.setLearningStatus(learningMode);
        }
    }


    public void stop(){
        mSensorManager.unregisterListener(this);
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
                mIsTangoServiceConnected = false;
                mLearningMode = false;
                mMainAct.setLearningStatus(mLearningMode);
                mMainAct.dump("Tango Service Disconnect");
        }
    }

    void restartTango(boolean learningMode, String adfUUID){
        stop();
        startTangoWithAdfUUID(learningMode,adfUUID);
    }


    private void setTangoListeners(){
        mMainAct.dump("setTangoListeners");
        // Select coordinate frame pairs

                boolean newLoc=true;

                mLocalized=false;

                boolean changed=false;

                if(mLocalized!=newLoc){
                    mLocalized=newLoc;
                    changed=true;
                    mMainAct.dump("Localized: " + mLocalized);
                }

                    changed=true;
                    mMainAct.dump("Status: " + statusText(mStatus));


                if(changed){
                    mMainAct.setStatusTango(mLocalized,statusText(mStatus));
                }



                // Throttle updates to Robot

                    //convert quaternion to simple azimuth (z rotation);
                    double rot=-.3f;
                    //need to add 90 to rotation to line up with x-axis (basis for atan2(y,x) returned angles)
                    rot=makeAngleInProperRange(rot + Math.PI/2);

                    Vec3 translation=new Vec3(1,.5f,1);
                    mMainAct.setStatusPoseData(translation,(float)rot);
                     //update robot localization status if changed
                    if(mLocalized != mRobot.isLocalized()){
                        mRobot.setLocalized(mLocalized);
                    }

                    mRobot.setPose(translation,rot);
                    mMainAct.sendToRemote(translation,(float)rot);

            }




    //helper function
    public String statusText(int status){
                return "VALID";

    }

    public void saveADF(final String fileName){
        if(mLearningMode && mLocalized){
            mSaveTask=new AsyncTask<Void,Integer,String>(){
                @Override
                protected String doInBackground(Void... params){
                    mMainAct.dump("Saving Area Description...");
                        return "Fake UUID";
                }

                @Override
                protected void onPostExecute(String uuid){
                    //save name of file
                    if(uuid != null){
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

    public void stopLearnADFmode(){
        restartTango(false,mLastUUID);
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
