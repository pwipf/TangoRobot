package com.ursaminoralpha.littlerobot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.TimerTask;

/**
 * Created by Magpie on 4/17/2016.
 */
public class MapView1stPerson extends View implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener{
    ScaleGestureDetector mScaleDet;
    GestureDetector mGestDet;
    float mLastRot=0;

    float mLastX,mLastY;

    static final float FTtM=0.3048f;
    float scaleWtoS=1;
    PointF mSize=new PointF(); //device width
    PointF mExtents=new PointF(); //real world meters
    PointF mCenter=new PointF(); // real world meters
    float mAspect;
    float mRot;
    Matrix mWorldToScreen=new Matrix();
    Matrix mWorldToScreenCenter=new Matrix();
    float mScale=1;

    Matrix mScreenToWorld=new Matrix();
    Matrix mScreenScaleToWorld=new Matrix();
    Matrix mScreenScaleTranslate=new Matrix();
    static MainActivity mMainAct;
    Paint xAxisPaint=new Paint();
    Paint zAxisPaint=new Paint();
    Paint mRobotPaint=new Paint();
    Paint mWheelPaint=new Paint();
    Paint mTargetPaint=new Paint();
    Paint mBlackPaint=new Paint();
    Paint obstPaint=new Paint();
    PointF mRobotLoc=new PointF();
    Matrix mRobotModel=new Matrix();
    Matrix mWorldInverse=new Matrix();
    Matrix mWorldToScreenCenterInverse=new Matrix();
    Matrix mRobotModelInverse=new Matrix();
    float mRobotRot=0;
    float mRoboPts[]={1, 1, -1, 1, -1, 1, 0, -1, 0, -1, 1, 1, -1.1f, .4f, 1.1f, .4f,
            -1.1f, .9f, -1.1f, -.1f, 1.1f, .9f, 1.1f, -.1f};


    static final int NDEPTHPTS = 7;
    PointF[] mDepthPts=new PointF[NDEPTHPTS];
    float[] mDepthValue = new float[NDEPTHPTS];
    int mDepthIndex=0;


    static final int NOBSTPTS = 30;
    float[] obstPt = new float[NOBSTPTS];
    float[] obstBuf = new float[NOBSTPTS];
    int nObstPt,nObstPtUsed;


    ArrayList<PointF> mTargets=new ArrayList<>();
    ArrayList<String> mTargetNames=new ArrayList<>();

    private static final int NUM_PAINTS=10;
    Paint paint[]=new Paint[NUM_PAINTS];

    @Override
    protected void onDraw(Canvas canvas){
        if(isInEditMode())
            return;

        drawGrid(canvas);
        drawAxex(canvas);
        drawRobot(canvas);

        drawTargets(canvas);

        canvas.drawRect(1, 1, mSize.x, mSize.y - 1, mBlackPaint);
        drawDepthPts(canvas);
        drawObstPts(canvas);
    }

    public void addTarget(float x, float y, String name){
        mTargets.add(new PointF(x, y));
        mTargetNames.add(name);
        postInvalidate();
    }

    public void drawTargets(Canvas canvas){
        int n=1;
        drawing = true;
        for(PointF pt :mTargets){
            PointF dp = toDevice(pt);
            canvas.drawCircle(dp.x,dp.y,5,mTargetPaint);
            canvas.drawText(mTargetNames.get(n-1),dp.x+5,dp.y+5,zAxisPaint);
            n++;
        }
        drawing = false;
    }

    public void clearObstacles(){
        nObstPt=0;
        nObstPtUsed=0;
        postInvalidate();
    }

    public void addObstPt(float x,float z){
        float[] t={x,z};

            obstPt[nObstPt * 2] = t[0];
            obstPt[nObstPt * 2 + 1] = t[1];
            nObstPt++;
            if (nObstPt == obstPt.length / 2)
                nObstPt = 0;

            nObstPtUsed++;
            nObstPtUsed=Math.min(nObstPtUsed,obstPt.length/2);

    }
    private void drawObstPts(Canvas canvas){
        mWorldToScreen.mapPoints(obstBuf,obstPt);
        float diam = 4*mScale;
        for(int i=0;i<nObstPtUsed;i++){
            canvas.drawCircle(obstBuf[i*2],obstBuf[i*2+1],diam,obstPaint);
        }
        //canvas.drawPoints(obstBuf,obstPaint);
    }

    public void addDepthPt(float x, float y, float z) {

        mDepthPts[mDepthIndex] = new PointF(x, z);
        mDepthValue[mDepthIndex] = z;
        mDepthIndex++;if(mDepthIndex==NDEPTHPTS)mDepthIndex=0;

    }

    private void drawDepthPts(Canvas canvas){

        float diam = 6*mScale;
        for (int i = 0; i < NDEPTHPTS; i++) {
            float[] fpt = {mDepthPts[i].x, mDepthPts[i].y};
            //mRobotModel.mapPoints(fpt);
            mWorldToScreenCenter.mapPoints(fpt);
            paint[4].setColor(Color.rgb(0,150,100));
            float d = mDepthValue[i] * 5;
            if (d == 0) {
                paint[4].setColor(Color.BLACK);
            }
            canvas.drawCircle(fpt[0], fpt[1], diam, paint[4]);
        }
    }

    boolean drawing = false;
    public void clearTargets(){
        while (drawing) {
            SystemClock.sleep(10);
        }
        mTargets.clear();
        mTargetNames.clear();
        postInvalidate();
    }

    public void setRobot(float x, float y, float rot){
        //mRobotLoc.x=x;
        //mRobotLoc.y=y;
        //mRobotRot=rot;

        rot*=180/Math.PI;
        rot=rot - 90;



//        mRobotModel=new Matrix();
//        mRobotModel.setScale(.1f*mScale, .1f*mScale);


        mCenter.x=x;
        mCenter.y=y;
        mRot=-rot;
//        setTransform();
//        postInvalidate();

        mWorldInverse=new Matrix();
        mWorldInverse.preTranslate(mCenter.x,mCenter.y);
        mWorldInverse.preRotate(-mRot);

        setTransform();
        postInvalidate();


        //mRobotModel.postRotate(-rot+180);
        //mRobotModel.postTranslate(x, y);
    }


    void drawAxex(Canvas canvas){
        float[] xpts={0, 0, 1, 0, 1, 0, .8f, .2f, 1, 0, .8f, -.2f};
        float[] zpts={0, 0, 0, 1, 0, 1, .2f, .8f, 0, 1, -.2f, .8f};
        mWorldToScreen.mapPoints(xpts);
        mWorldToScreen.mapPoints(zpts);
        canvas.drawLines(xpts, xAxisPaint);
        canvas.drawLines(zpts, zAxisPaint);
    }

    void drawRobot(Canvas canvas){
        float[] dot={0, 0};
        float[] pts=new float[mRoboPts.length];
        mRobotModel.mapPoints(pts, mRoboPts);
        mRobotModel.mapPoints(dot);
        mWorldToScreenCenter.mapPoints(pts);
        mWorldToScreenCenter.mapPoints(dot);
        canvas.drawLines(pts, 0, 16, mRobotPaint);
        canvas.drawLines(pts, 16, 8, mWheelPaint);
        canvas.drawCircle(dot[0], dot[1], 5, zAxisPaint);
    }

    void drawGrid(Canvas canvas){
        ArrayList<Float> list=new ArrayList<>();
        float xf=mAspect*3.5f, yf=3.5f;
        for(float x=0; x<=mExtents.x*xf + mCenter.x; x+=FTtM){
            list.add(x);
            list.add(mExtents.y*yf + mCenter.y);
            list.add(x);
            list.add(-mExtents.y*yf + mCenter.y);
        }
        for(float x=0; x>=-mExtents.x*xf + mCenter.x; x-=FTtM){
            list.add(x);
            list.add(mExtents.y*yf + mCenter.y);
            list.add(x);
            list.add(-mExtents.y*yf + mCenter.y);
        }
        for(float y=0; y<=mExtents.y*yf + mCenter.y; y+=FTtM){
            list.add(mExtents.x*xf + mCenter.x);
            list.add(y);
            list.add(-mExtents.x*xf + mCenter.x);
            list.add(y);
        }
        for(float y=0; y>=-mExtents.y*yf + mCenter.y; y-=FTtM){
            list.add(mExtents.x*xf + mCenter.x);
            list.add(y);
            list.add(-mExtents.x*xf + mCenter.x);
            list.add(y);
        }
        float[] pts=new float[list.size()];
        for(int i=0; i<list.size(); i++)
            pts[i]=list.get(i);
        mWorldToScreen.mapPoints(pts);
        canvas.drawLines(pts, paint[2]);
    }

    PointF toDevice(PointF p){
        float pt[]={p.x, p.y};
        float dest[]=new float[2];
        mWorldToScreen.mapPoints(dest, pt);
        return new PointF(dest[0], dest[1]);
    }

//    public MapView(Context context){
//        super(context);
//        init(context);
//    }

    public MapView1stPerson(Context context, AttributeSet attrs){
        super(context, attrs);
        if(isInEditMode())
            return;
        mMainAct=(MainActivity)context;
        mScaleDet=new ScaleGestureDetector(context, this);
        mGestDet=new GestureDetector(context, this);
        init(context);
    }

    private void init(Context context){
        mScaleDet=new ScaleGestureDetector(context, this);
        mGestDet=new GestureDetector(context, this);
        //this.setOnTouchListener(this);
        for(int i=0; i<NUM_PAINTS; i++)
            paint[i]=new Paint();

        for (int i = 0; i < NDEPTHPTS; i++)
            mDepthPts[i] = new PointF();

        for(int i=0;i<mRoboPts.length;i++)
            mRoboPts[i]*=.1f;

        paint[0].setStyle(Paint.Style.FILL);
        paint[0].setColor(Color.RED);
        paint[1].setColor(Color.rgb(0, 180, 0));
        paint[1].setStrokeWidth(3);
        paint[1].setStyle(Paint.Style.STROKE);
        paint[2].setColor(Color.rgb(180, 180, 180));
        paint[2].setStrokeWidth(0);
        paint[3].setStyle(Paint.Style.STROKE);
        paint[3].setStrokeWidth(2);
        paint[3].setColor(Color.BLUE);
        paint[4].setStyle(Paint.Style.STROKE);
        paint[4].setStrokeWidth(2);
        paint[4].setColor(Color.rgb(200,100,40));
        obstPaint.setStyle(Paint.Style.STROKE);
        obstPaint.setColor(Color.rgb(180,0,0));
        obstPaint.setStrokeWidth(3);
         xAxisPaint.setColor(Color.RED);
        xAxisPaint.setStrokeWidth(2);
        zAxisPaint.setColor(Color.BLUE);
        zAxisPaint.setStrokeWidth(2);
        mRobotPaint.setColor(Color.rgb(200, 150, 10));
        mRobotPaint.setStrokeWidth(4);
        mWheelPaint.setColor(Color.BLACK);
        mWheelPaint.setStrokeWidth(4);
        mBlackPaint.setColor(Color.BLACK);
        mBlackPaint.setStrokeWidth(2);
        mBlackPaint.setStyle(Paint.Style.STROKE);
        mTargetPaint.setColor(Color.MAGENTA);
        mTargetPaint.setStyle(Paint.Style.FILL);
        setRobot(1, 1, -30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        mSize=new PointF(w, h);
        mMainAct.dump(w + " x " + h);
        mAspect=mSize.x/mSize.y;
        mMainAct.dump("aspect: " + mAspect);
        setExtCenterRot(2.5f, 2.5f, 0, 0, 90);


    }

    void setTransform(){
        RectF screen=new RectF(0, 0, mSize.x, mSize.y);
        RectF world=new RectF(-mExtents.x, -mExtents.y, mExtents.x, mExtents.y);
        mWorldToScreen.setRectToRect(world, screen, Matrix.ScaleToFit.CENTER);
        mWorldToScreen.preScale(mScale, -mScale);
        mWorldToScreenCenter.set(mWorldToScreen);
        mWorldToScreen.invert(mWorldToScreenCenterInverse);
        mWorldToScreen.preRotate(mRot);
        mWorldToScreen.preTranslate(-mCenter.x, -mCenter.y);
        mWorldToScreen.invert(mScreenScaleTranslate);
        mWorldToScreen.invert(mScreenToWorld);
        //mWorldToScreen.preScale(mScale,mScale);
    }

    void transform(float dx, float dy, float scl,float rot){
        mScale*=scl;
        //mWorldToScreen.preScale(scl,scl);
        setTransform();
        //mRobotModel.preScale(scl,scl);
        //mRobotModel.invert(mRobotModelInverse);
        postInvalidate();
    }

    public void setExtCenterRot(float x, float y, float cx, float cy, float rot){
        if(y<=x)
            mExtents=new PointF(x, x);
        else
            mExtents=new PointF(y, y);
        mCenter.x=cx;
        mCenter.y=cy;
        mRot=rot;
        setTransform();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        //mMainAct.dump("Fingers " + event.getPointerCount());
        mGestDet.onTouchEvent(event);
        mScaleDet.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
        //transform(-distanceX,distanceY,1,0);
        return true;
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector){

        float f=mScaleDet.getScaleFactor();
        //mMainAct.dump("scalefactor "+f);

        transform(0,0,f,0);

        return true;
    }


    ///////////////unused methods that require implementation
    @Override
    public boolean onDown(MotionEvent e){
        return true;
    }
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
        return false;
    }
    @Override
    public void onShowPress(MotionEvent e){

    }
    @Override
    public boolean onSingleTapUp(MotionEvent e){
        mMainAct.dump("tap");
        return true;
    }
    @Override
    public void onLongPress(MotionEvent e){
        mMainAct.dump("long");
    }
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector){
        return true;
    }
    @Override
    public void onScaleEnd(ScaleGestureDetector detector){
    }
}
