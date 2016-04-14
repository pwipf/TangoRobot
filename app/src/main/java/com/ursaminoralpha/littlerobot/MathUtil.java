package com.ursaminoralpha.littlerobot;

public class MathUtil{
    // Math and vector stuff
    // This one makes sure an angle is between PI and -PI. Probably not terribly efficient.
    static double makeAngleInProperRange(double angle){
        while(angle<-Math.PI){
            angle=Math.PI - (-Math.PI - angle);
        }
        while(angle>Math.PI){
            angle=-Math.PI + (angle - Math.PI);
        }
        return angle;
    }

    //get an euler angle from a quaternion, in Tango galaxy
    static double quaternionToAngle(double q[], int x0y1z2){
        switch(x0y1z2){
            case 0://x
                return Math.atan2(2*(q[3]*q[0] + q[1]*q[2]), 1 - 2*(q[1]*q[1] + q[0]*q[0]));
            case 1://y
                return Math.atan2(2*(q[3]*q[2] + q[1]*q[0]), 1 - 2*(q[1]*q[1] + q[2]*q[2]));
            case 2://z
                return Math.asin(2*(q[3]*q[1] - q[2]*q[0]));
        }
        return 0;
    }}
