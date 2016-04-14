package com.ursaminoralpha.littlerobot;


// This is a data structure to send settings to the Settings Activity,
// which is a pain in the but.

import android.os.Parcel;
import android.os.Parcelable;

public class Settings implements Parcelable{
    float threshDistBig;
    float threshDistSmall;
    float threshAngleBig;
    float threshAngleSmall;
    float updateInterval;
    //constructor
    Settings(float up,float dsmall,float dbig,float asmall, float abig){
        threshAngleBig=abig;
        threshAngleSmall=asmall;
        threshDistBig=dbig;
        threshDistSmall=dsmall;
        updateInterval=up;
    }
    Settings(){
    }

    //constructor that parcelable uses to recreate
    private Settings(Parcel in) {
        updateInterval = in.readFloat();
        threshDistSmall= in.readFloat();
        threshDistBig= in.readFloat();
        threshAngleSmall= in.readFloat();
        threshAngleBig= in.readFloat();
    }

    //required for parcelable
    @Override
    public int describeContents() {
        return 0;
    }


    // writes out the settings to the parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(updateInterval);
        out.writeFloat(threshDistSmall);
        out.writeFloat(threshDistBig);
        out.writeFloat(threshAngleSmall);
        out.writeFloat(threshAngleBig);
    }

    // I don't know how this works.
    // I guess this is what gets called when you call getIntent().getParcelableExtra("key")
    public static final Parcelable.Creator<Settings> CREATOR = new Parcelable.Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel in) {
            return new Settings(in);
        }
        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };
}
