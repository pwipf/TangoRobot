package com.ursaminoralpha.littlerobot;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusFragment extends Fragment{

    public static final int RED = 0xFF800000;
    public static final int GREEN = 0xFF008000;
    public static final int BLACK = 0xFF000000;
    public static final int ORANGE = 0xFF8d6101;
    public static final int DEFAULTCOLOR = 0xFF8d6101;

    MainActivity mMainAct;

    enum StatusItem{ADFNAME("ADFName","NOT FOUND",RED,GREEN),
        LOCALIZED("Localized","NO",RED,GREEN),
        LEARNING("Learning","NO",BLACK,ORANGE),
        POSESTAT("PoseStatus","VALID",GREEN,RED),
        POSITION("Position","0.00 0.00 0.00",RED,GREEN),
        ROTATION("Rotation","0.0",RED,GREEN),
        SERIALFOUND("SerialFound","NO",RED,GREEN),
        SERIALCON("SerialConnected","NO",RED,GREEN),
        REMOTEIP("RemoteIP","No IP",RED,GREEN),
        REMOTEPORT("RemotePort","6242",GREEN,ORANGE),
        REMOTERUN("RemoteRunning","NO",RED,GREEN),
        REMOTECON("RemoteConnected","0",RED,GREEN),
        ROBOMODE("RobotMode","STOP",GREEN,ORANGE),
        ROBOLASTCOM("RobotLastCom","STOP",GREEN,ORANGE);
        String string;
        String compare;
        int color1,color2;
        String currentString="";
        StatusItem(String s,String c,int c1,int c2){string=s;compare=c;color1=c1;color2=c2;}
        @Override public String toString(){return string;}
    }

    static final TextView[] mViews=new TextView[StatusItem.values().length];

    //constructor

    private static void setItem(StatusItem item, String string){
        mViews[item.ordinal()].setTextColor(string.equals(item.compare)? item.color1 : item.color2);
        mViews[item.ordinal()].setText(string);
        item.currentString=string;
    }

    public static void setStatusItem(StatusItem item, boolean b){
        setItem(item,b? "YES":"NO");
    }

    public static void setStatusItem(StatusItem item, String string){
        setItem(item,string);
    }

    public static String getString(StatusItem item){
        return item.currentString;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        mViews[StatusItem.ADFNAME.ordinal()]=(TextView) view.findViewById(R.id.statusADBName);
        mViews[StatusItem.LOCALIZED.ordinal()] = (TextView) view.findViewById(R.id.statusLocalized);
        mViews[StatusItem.LEARNING.ordinal()] = (TextView) view.findViewById(R.id.statusLearning);
        mViews[StatusItem.POSESTAT.ordinal()] = (TextView) view.findViewById(R.id.statusPoseStatus);
        mViews[StatusItem.POSITION.ordinal()] = (TextView) view.findViewById(R.id.statusTranslation);
        mViews[StatusItem.ROTATION.ordinal()] = (TextView) view.findViewById(R.id.statusRotation);
        mViews[StatusItem.SERIALFOUND.ordinal()] = (TextView) view.findViewById(R.id.statusSerialFound);
        mViews[StatusItem.SERIALCON.ordinal()] = (TextView) view.findViewById(R.id.statusSerialCon);
        mViews[StatusItem.REMOTEIP.ordinal()] = (TextView) view.findViewById(R.id.statusRemoteIP);
        mViews[StatusItem.REMOTEPORT.ordinal()] = (TextView) view.findViewById(R.id.statusRemotePort);
        mViews[StatusItem.REMOTERUN.ordinal()] = (TextView) view.findViewById(R.id.statusRemoteServer);
        mViews[StatusItem.REMOTECON.ordinal()] = (TextView) view.findViewById(R.id.statusRemoteCon);
        mViews[StatusItem.ROBOMODE.ordinal()] = (TextView) view.findViewById(R.id.statusRobotMode);
        mViews[StatusItem.ROBOLASTCOM.ordinal()] = (TextView) view.findViewById(R.id.statusRobotLastCom);
        return view;
    }
}
