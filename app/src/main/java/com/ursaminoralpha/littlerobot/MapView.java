package com.ursaminoralpha.littlerobot;

        import android.content.Context;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Matrix;
        import android.graphics.Paint;
        import android.graphics.PointF;
        import android.graphics.RectF;
        import android.util.AttributeSet;
        import android.util.Log;
        import android.view.GestureDetector;
        import android.view.MotionEvent;
        import android.view.ScaleGestureDetector;
        import android.view.View;

        import java.util.ArrayList;
        import java.util.Timer;
        import java.util.TimerTask;

/**
 * Created by Magpie on 4/17/2016.
 */
public class MapView extends View implements RotationGesture.OnRotationListener,ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener{
    ScaleGestureDetector mScaleDet;
    GestureDetector mGestDet;
    RotationGesture mRotGest;
    float mLastRot;


    static final float FTtM=0.3048f;
    float scaleWtoS=1;
    PointF mSize=new PointF(); //device width
    PointF mExtents=new PointF(); //real world meters
    PointF mCenter=new PointF(); // real world meters
    float mAspect;
    float mRot;
    Matrix mWorldToScreen=new Matrix();
    float mScale=120;

    Matrix mScreenToWorld=new Matrix();
    Matrix mScreenScaleToWorld=new Matrix();
    Matrix mScreenScaleTranslate=new Matrix();
    static MainActivity mMainAct;
    Paint xAxisPaint=new Paint();
    Paint zAxisPaint=new Paint();
    Paint mRobotPaint=new Paint();
    Paint mWheelPaint=new Paint();
    Paint mBlackPaint=new Paint();
    PointF mRobotLoc=new PointF();
    Matrix mRobotModel=new Matrix();
    float mRobotRot=0;
    float mRoboPts[]={1, 1, -1, 1, -1, 1, 0, -1, 0, -1, 1, 1, -1.1f, .4f, 1.1f, .4f,
            -1.1f, .9f, -1.1f, -.1f, 1.1f, .9f, 1.1f, -.1f};

    ArrayList<PointColor> mTargets=new ArrayList<>();
    ArrayList<Integer> mTargetCol=new ArrayList<>();
    //ArrayList<PointF> mDepthPts=new ArrayList<>();

    static final int NDEPTHPTS = 9 * 9;
    PointF[] mDepthPts=new PointF[NDEPTHPTS];
    float[] mDepthValue = new float[NDEPTHPTS];
    int mDepthIndex=0;

    private static final int NUM_PAINTS=10;
    Paint paint[]=new Paint[NUM_PAINTS];

    class PointColor{
        PointF pt;
        Integer col;

        PointColor(PointF pt, Integer col){
            this.pt=pt;
            this.col=col;
        }
    }



    public void addTarget(float x, float y, Integer col){
        mTargets.add(new PointColor(new PointF(x, y), col));
        postInvalidate();
    }

    public void addDepthPt(float u,float v,float z){//float[3]
        PointF pt=new PointF(); //in world coords, want to show z dist in front of robot, with u,v = .5 (middle of camera), will need some calibration
        pt.y = (v - .5f) * 10 - 3;
        pt.x=(u-.5f)*10; //0 for uv .5
        mDepthPts[mDepthIndex]=new PointF(pt.x,pt.y);
        mDepthValue[mDepthIndex] = z;
        mDepthIndex++;if(mDepthIndex==NDEPTHPTS)mDepthIndex=0;

        Log.e("NUM", pt.x + " " + pt.y);

        //schedule timer to remove point in 1 second
        //new Timer().schedule(new RemoveDepthPtTask(index),1000);
    }
    class RemoveDepthPtTask extends TimerTask{
        int index;
        RemoveDepthPtTask(int i){index=i;}
        public void run(){
            removeDepthPt(index);
        }
    }

    private void removeDepthPt(int index){
        // mDepthPts.remove(index);
    }

    private void drawDepthPts(Canvas canvas){

        //PointF[] pts=mDepthPts.toArray(new PointF[mDepthPts.size()]);

        for (int i = 0; i < NDEPTHPTS; i++) {
            float[] fpt = {mDepthPts[i].x, mDepthPts[i].y};
            mRobotModel.mapPoints(fpt);
            mWorldToScreen.mapPoints(fpt);
            paint[4].setColor(Color.rgb(0,150,100));
            float d = mDepthValue[i] * 5;
            d = Math.min(d, 10);
            canvas.drawCircle(fpt[0], fpt[1], d, paint[4]);
        }
    }

    public void clearTargets(){
        mTargets.clear();
        postInvalidate();
    }

    public void setRobot(float x, float y, float rot){
        mRobotLoc.x=x;
        mRobotLoc.y=y;
        mRobotRot=rot;

        rot*=180/Math.PI;
        rot=rot - 90;

        mRobotModel=new Matrix();
        mRobotModel.setScale(.1f, .1f);
        mRobotModel.postRotate(rot);
        mRobotModel.postTranslate(x, y);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas){
        if(isInEditMode())
            return;
        drawGrid(canvas);
        drawAxex(canvas);
        drawRobot(canvas);
        canvas.drawLine(1, 1, 1, mSize.y - 1, mBlackPaint);

        for(int i=0;i<mTargets.size();i++){
            PointF dp=toDevice(mTargets.get(i).pt);
            paint[0].setColor(mTargets.get(i).col);
            canvas.drawCircle(dp.x, dp.y, 5, paint[0]);
            canvas.drawText(""+(i+1),dp.x+5,dp.y+5,zAxisPaint);
        }

        drawDepthPts(canvas);
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
        mWorldToScreen.mapPoints(pts);
        mWorldToScreen.mapPoints(dot);
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
        float x=(p.x - mCenter.x)*mSize.x/mExtents.x;
        float y=(p.y - mCenter.y)*mSize.y/mExtents.y;
        float pt[]={p.x, p.y};
        float dest[]=new float[2];
        mWorldToScreen.mapPoints(dest, pt);
        return new PointF(dest[0], dest[1]);
    }

//    public MapView(Context context){
//        super(context);
//        init(context);
//    }

    public MapView(Context context, AttributeSet attrs){
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
        mRotGest=new RotationGesture(this);
        //this.setOnTouchListener(this);
        for(int i=0; i<NUM_PAINTS; i++)
            paint[i]=new Paint();
        for(int i=0;i<NDEPTHPTS;i++)
            mDepthPts[i]=new PointF(0,0);
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
        setRobot(1, 1, -30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        mSize=new PointF(w, h);
        //mMainAct.dump(w + " x " + h);
        mAspect=mSize.x/mSize.y;
        //mMainAct.dump("aspect: " + mAspect);
        setExtCenterRot(2.5f, 2.5f, 0, 0, 5);


    }

    void setTransform(){
        RectF screen=new RectF(0, 0, mSize.x, mSize.y);
        RectF world=new RectF(-mExtents.x, -mExtents.y, mExtents.x, mExtents.y);
        mWorldToScreen.setRectToRect(world, screen, Matrix.ScaleToFit.CENTER);
        mWorldToScreen.preScale(1, -1);
        mWorldToScreen.invert(mScreenScaleToWorld);
        mWorldToScreen.preTranslate(-mCenter.x, -mCenter.y);
        mWorldToScreen.invert(mScreenScaleTranslate);
        mWorldToScreen.preRotate(mRot);
        mWorldToScreen.invert(mScreenToWorld);
    }

    void transform(float dx, float dy, float scl,float rot){
        mScale*=scl;

        mWorldToScreen.preRotate(rot);
        mWorldToScreen.postTranslate(dx,-dy);
        mWorldToScreen.preScale(scl,scl);
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
        mRotGest.onTouchEvent(event);
        mGestDet.onTouchEvent(event);
        mScaleDet.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
        transform(-distanceX,distanceY,1,0);
        return true;
    }

    @Override
    public void OnRotation(RotationGesture rotationDetector){
        float rot=rotationDetector.getAngle();
        transform(0,0,1,rot-mLastRot);
        mLastRot=rot;
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
