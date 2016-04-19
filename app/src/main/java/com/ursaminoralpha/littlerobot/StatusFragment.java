package com.ursaminoralpha.littlerobot;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusFragment extends Fragment {

    public static final int RED = 0xFF800000;
    public static final int GREEN = 0xFF008000;
    public static final int BLACK = 0xFF000000;
    public static final int ORANGE = 0xFF8d6101;

    public class Status {
        String adfName = "";
        boolean localized;
        boolean learning;
        String poseStatus = "";
        Vec3 position = new Vec3();
        double rotation;
        boolean serialFound;
        boolean serialConnected;
        String ip = "";
        int port;
        boolean serverRunning;
        int connections;
        String roboMode = "";
        String roboOther = "";
    }

    TextView mTextADFName;
    TextView mTextLocalized;
    TextView mTextLearning;
    TextView mTextPoseStatus;
    TextView mTextPosition;
    TextView mTextRotation;
    TextView mTextSerialFound;
    TextView mTextSerialCon;
    TextView mTextIP;
    TextView mTextPort;
    TextView mTextServer;
    TextView mTextServerCon;
    TextView mTextRoboMode;
    TextView mTextRoboOther;

    public Status mStat;


    public StatusFragment() {
        mStat = new Status();
    }

    public void ADFName(String s) {
        if (mStat.adfName.equals(s))
            return;
        mStat.adfName = s;
        mTextADFName.setTextColor
                (mStat.adfName.equals("NOT FOUND") ? RED : GREEN);
        mTextADFName.setText(mStat.adfName);
    }

    public void localized(boolean s) {
        if (mStat.localized == s)
            return;
        mStat.localized = s;
        mTextLocalized.setTextColor(mStat.localized ? GREEN : RED);
        mTextLocalized.setText(mStat.localized ? "YES" : "NO");
    }

    public void learning(boolean s) {
        if (mStat.learning == s)
            return;
        mStat.learning = s;
        mTextLearning.setTextColor(mStat.learning ? ORANGE : BLACK);
        mTextLearning.setText(mStat.learning ? "YES" : "NO");
    }

    public void poseStatus(String s) {
        if (mStat.poseStatus.equals(s))
            return;
        mStat.poseStatus = s;
        mTextPoseStatus.setTextColor(mStat.poseStatus.equals("VALID") ? GREEN : RED);
        mTextPoseStatus.setText(mStat.poseStatus);
    }

    public void serialFound(boolean s) {
        if (mStat.serialFound == s)
            return;
        mStat.serialFound = s;
        mTextSerialFound.setTextColor(mStat.serialFound ? GREEN : RED);
        mTextSerialFound.setText(mStat.serialFound ? "YES" : "NO");
    }

    public void serialCon(boolean s) {
        if (mStat.serialConnected == s)
            return;
        mStat.serialConnected = s;
        mTextSerialCon.setTextColor(mStat.serialConnected ? GREEN : RED);
        mTextSerialCon.setText(mStat.serialConnected ? "YES" : "NO");
    }

    public void remoteIP(String s) {
        if (mStat.ip.equals(s))
            return;
        mStat.ip = s;
        mTextIP.setText(mStat.ip + "");
    }

    public void remotePort(int s) {
        if (mStat.port == s)
            return;
        mStat.port = s;
        mTextPort.setText(mStat.port + "");
    }

    public void remoteRunning(boolean s) {
        if (mStat.serverRunning == s)
            return;
        mStat.serverRunning = s;
        mTextServer.setTextColor(mStat.serverRunning ? GREEN : RED);
        mTextServer.setText(mStat.serverRunning ? "YES" : "NO");
    }

    public void remoteConnections(int s) {
        if (mStat.connections == s)
            return;
        mStat.connections = s;
        mTextServerCon.setTextColor(mStat.connections > 0 ? GREEN : RED);
        mTextServerCon.setText(mStat.connections + "");
    }

    public void roboMode(String s) {
        if (mStat.roboMode.equals(s))
            return;
        mStat.roboMode = s;
        mTextRoboMode.setText(mStat.roboMode + "");
    }

    public void roboOther(String s) {
        if (mStat.roboOther.equals(s))
            return;
        mStat.roboOther = s;
        mTextRoboOther.setTextColor(mStat.roboOther.equals("GOOD") ? GREEN : RED);
        mTextRoboOther.setText(mStat.roboOther + "");
    }

    public void setPose(Vec3 position, double rotation) {
        mStat.position = position;
        mStat.rotation = rotation;
        mTextPosition.setText(mStat.position.toString());
        mTextRotation.setText(String.format("%.1f", mStat.rotation * (180 / Math.PI)));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        mTextADFName = (TextView) view.findViewById(R.id.statusADBName);
        mTextLocalized = (TextView) view.findViewById(R.id.statusLocalized);
        mTextLearning = (TextView) view.findViewById(R.id.statusLearning);
        mTextPoseStatus = (TextView) view.findViewById(R.id.statusPoseStatus);
        mTextPosition = (TextView) view.findViewById(R.id.statusTranslation);
        mTextRotation = (TextView) view.findViewById(R.id.statusRotation);
        mTextSerialFound = (TextView) view.findViewById(R.id.statusSerialFound);
        mTextSerialCon = (TextView) view.findViewById(R.id.statusSerialCon);
        mTextIP = (TextView) view.findViewById(R.id.statusRemoteIP);
        mTextPort = (TextView) view.findViewById(R.id.statusRemotePort);
        mTextServer = (TextView) view.findViewById(R.id.statusRemoteServer);
        mTextServerCon = (TextView) view.findViewById(R.id.statusRemoteCon);
        mTextRoboMode = (TextView) view.findViewById(R.id.statusRobotMode);
        mTextRoboOther = (TextView) view.findViewById(R.id.statusRobotOther);
        return view;
    }
}
