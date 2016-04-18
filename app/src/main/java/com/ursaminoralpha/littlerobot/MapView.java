package com.ursaminoralpha.littlerobot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Magpie on 4/17/2016.
 */
public class MapView extends View implements View.OnTouchListener{
    static final float FTtM=0.3048f;
    float scaleWtoS=1;
    PointF mSize=new PointF(); //device width
    PointF mExtents=new PointF(); //real world meters
    PointF mCenter=new PointF(); // real world meters
    float mAspect;
    float mRot;
    Matrix mWorldToScreen=new Matrix();
    Matrix mScreenToWorld=new Matrix();
    Matrix mScreenScaleToWorld=new Matrix();
    Matrix mScreenScaleTranslate=new Matrix();
    static MainActivity mMainAct;
    Paint xAxisPaint=new Paint();
    Paint zAxisPaint=new Paint();
    Paint mRobotPaint = new Paint();
    Paint mWheelPaint = new Paint();
    PointF mRobotLoc=new PointF();
    Matrix mRobotModel=new Matrix();
    float mRobotRot=0;
    float mRoboPts[]={1,1,-1,1, -1,1,0,-1, 0,-1,1,1, -1.1f,.4f,1.1f,.4f,
            -1.1f,.9f,-1.1f,-.1f, 1.1f,.9f,1.1f,-.1f};


    class PointColor{
        PointF pt;
        Integer col;
        PointColor(PointF pt,Integer col){
            this.pt=pt;this.col=col;
        }
    }
    ArrayList<PointColor> mTargets = new ArrayList<>();
    ArrayList<Integer> mTargetCol=new ArrayList<>();

    private static final int NUM_PAINTS=10;
    Paint paint[]=new Paint[NUM_PAINTS];

    public void addTarget(PointF p,Integer col){
        mTargets.add(new PointColor(p,col));
        postInvalidate();
    }

    public void setRobot(float x,float y,float rot){
        mRobotLoc.x=x;
        mRobotLoc.y=y;
        mRobotRot=rot;

        rot*=180/Math.PI;
        rot=rot-90;

        mRobotModel=new Matrix();
        mRobotModel.setScale(.1f,.1f);
        mRobotModel.postRotate(rot);
        mRobotModel.postTranslate(x,y);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas){
        if(isInEditMode())
            return;
        canvas.drawRect(1,0,mSize.x,mSize.y-1,paint[3]);
        drawAxex(canvas);
        drawRobot(canvas);
        drawGrid(canvas);

        for(PointColor p:mTargets){
            PointF dp=toDevice(p.pt);
            paint[0].setColor(p.col);
            canvas.drawCircle(dp.x, dp.y, 5, paint[0]);
            canvas.drawCircle(dp.x, dp.y, 25, paint[3]);
        }

    }

    void drawAxex(Canvas canvas){
        float[] xpts={0,0,1,0, 1,0,.8f,.2f, 1,0,.8f,-.2f};
        float[] zpts={0,0,0,1, 0,1,.2f,.8f, 0,1,-.2f,.8f};
        mWorldToScreen.mapPoints(xpts);
        mWorldToScreen.mapPoints(zpts);
        canvas.drawLines(xpts, xAxisPaint);
        canvas.drawLines(zpts, zAxisPaint);
    }
    void drawRobot(Canvas canvas){
        float[] dot={0,0};
        float[] pts=new float[mRoboPts.length];
        mRobotModel.mapPoints(pts,mRoboPts);
        mRobotModel.mapPoints(dot);
        mWorldToScreen.mapPoints(pts);
        mWorldToScreen.mapPoints(dot);
        canvas.drawLines(pts,0,16, mRobotPaint);
        canvas.drawLines(pts,16,8,mWheelPaint);
        canvas.drawCircle(dot[0],dot[1],5,zAxisPaint);
    }
    void drawGrid(Canvas canvas){
        ArrayList<Float> list=new ArrayList<>();
        float xf=mAspect*3.5f,yf=3.5f;
        for(float x=0; x<=mExtents.x*xf+mCenter.x; x+=FTtM){
            list.add(x);list.add(mExtents.y*yf+mCenter.y);
            list.add(x);list.add(-mExtents.y*yf+mCenter.y);
        }
        for(float x=0; x>=-mExtents.x*xf+mCenter.x; x-=FTtM){
            list.add(x);list.add(mExtents.y*yf+mCenter.y);
            list.add(x);list.add(-mExtents.y*yf+mCenter.y);
        }
        for(float y=0; y<=mExtents.y*yf+mCenter.y; y+=FTtM){
            list.add(mExtents.x*xf+mCenter.x);list.add(y);
            list.add(-mExtents.x*xf+mCenter.x);list.add(y);
        }
        for(float y=0; y>=-mExtents.y*yf+mCenter.y; y-=FTtM){
            list.add(mExtents.x*xf+mCenter.x);list.add(y);
            list.add(-mExtents.x*xf+mCenter.x);list.add(y);
        }
        float[] pts=new float[list.size()];
        for(int i=0;i<list.size();i++)
            pts[i]=list.get(i);
        mWorldToScreen.mapPoints(pts);
        canvas.drawLines(pts,paint[2]);
    }

    PointF toDevice(PointF p){
        float x=(p.x-mCenter.x)*mSize.x/mExtents.x;
        float y=(p.y-mCenter.y)*mSize.y/mExtents.y;
        float pt[] ={p.x,p.y};
        float dest[]=new float[2];
        mWorldToScreen.mapPoints(dest,pt);
        return new PointF(dest[0],dest[1]);
    }

    public MapView(Context context){
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs){
        super(context, attrs);
        if(isInEditMode())
            return;
        mMainAct=(MainActivity)context;
        init();
    }
    private void init(){
        this.setOnTouchListener(this);
        for(int i=0; i<NUM_PAINTS; i++)
            paint[i]=new Paint();
        paint[0].setStyle(Paint.Style.FILL);
        paint[0].setColor(Color.RED);
        paint[1].setColor(Color.rgb(0,180,0));
        paint[1].setStrokeWidth(3);
        paint[1].setStyle(Paint.Style.STROKE);
        paint[2].setColor(Color.rgb(100,100,100));
        paint[2].setStrokeWidth(0);
        paint[3].setStyle(Paint.Style.STROKE);
        paint[3].setStrokeWidth(0);
        paint[3].setColor(Color.BLUE);
        xAxisPaint.setColor(Color.RED);
        xAxisPaint.setStrokeWidth(2);
        zAxisPaint.setColor(Color.BLUE);
        zAxisPaint.setStrokeWidth(2);
        mRobotPaint.setColor(Color.rgb(200,150,10));
        mRobotPaint.setStrokeWidth(4);
        mWheelPaint.setColor(Color.BLACK);
        mWheelPaint.setStrokeWidth(4);
        setRobot(1,1,-30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        mSize=new PointF(w,h);
        mMainAct.dump(w+" x "+h);
        mAspect=mSize.x/mSize.y;
        mMainAct.dump("aspect: "+mAspect);
        setExtCenterRot(1.5f,1.5f,0,0,2);



    }

    void setTransform(){
        RectF screen=new RectF(0,0,mSize.x,mSize.y);
        RectF world=new RectF(-mExtents.x,-mExtents.y,mExtents.x,mExtents.y);
        mWorldToScreen.setRectToRect(world,screen, Matrix.ScaleToFit.CENTER);
        mWorldToScreen.preScale(1,-1);
        mWorldToScreen.invert(mScreenScaleToWorld);
        mWorldToScreen.preTranslate(-mCenter.x,-mCenter.y);
        mWorldToScreen.invert(mScreenScaleTranslate);
        mWorldToScreen.preRotate(mRot);
        mWorldToScreen.invert(mScreenToWorld);
    }

    public void setExtCenterRot(float x,float y, float cx,float cy,float rot){
        if(y<=x)
            mExtents=new PointF(x,x);
        else
            mExtents=new PointF(y,y);
        mCenter.x=cx;mCenter.y=cy;
        mRot=rot;
        setTransform();
    }


  //  public static class TouchListener  implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event){
            float pt[]={event.getX(),event.getY()};
            mScreenScaleToWorld.mapPoints(pt);
            //mMainAct.dump(String.format("%.2f, %.2f",pt[0],pt[1]));
            mCenter.x=-pt[0];mCenter.y=-pt[1];
            setTransform();
            postInvalidate();
            return false;
        }
  //  }
}
