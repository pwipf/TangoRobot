package com.ursaminoralpha.littlerobot;

import android.graphics.PointF;

// simple little vector class
public class Vec3{
    final double x, y, z;

    Vec3(double x, double y, double z){
        this.x=x;
        this.y=y;
        this.z=z;
    }

    Vec3(float f[]){
        this.x=f[0];
        this.y=f[1];
        this.z=f[2];
    }

    // this constructor handles a 4d array as a quaternion, and a 3d array as just a vector
    Vec3(double q[]){
        //convert quaternion to euler angles
        if(q.length==4){
            x=Math.atan2(2*(q[3]*q[0] + q[1]*q[2]), 1 - 2*(q[0]*q[0] + q[1]*q[1]));
            y=Math.asin(2*(q[3]*q[1] - q[0]*q[2]));
            z=Math.atan2(2*(q[3]*q[2] + q[0]*q[1]), 1 - 2*(q[1]*q[1] + q[2]*q[2]));

            //or just copy a 3d array
        } else{
            this.x=q[0];
            this.y=q[1];
            this.z=q[2];
        }
    }

    PointF toPointFXY(){
        return new PointF((float)x,(float)y);
    }

    Vec3(){
        this.x=this.y=this.z=0.0;
    }

    Vec3 subtract(Vec3 v){
        return new Vec3(x - v.x, y - v.y, z - v.z);
    }

    Vec3 add(Vec3 v){
        return new Vec3(x + v.x, y + v.y, z + v.z);
    }

    @Override
    public String toString(){
        return String.format("%+.02f %+.02f %+.02f", x, y, z);
    }


}