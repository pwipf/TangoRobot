package com.ursaminoralpha.littlerobot;

// TCP Server to connect a remote control
// Commands are strings followed by \n.
// "Forward\n", "Go To Target\n", etc.
// create object and call serverStart() to start.
//
// Runs separate thread which waits on connection and
// then the same thread continues and waits for data.
//
// Calls mainactivity.setStatusRemoteServer to tell it's status.

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum SendDataType{
    POSITIONROTATION(Type.FLOAT,4),
    STRINGCOMMAND(Type.STRING,0),
    TARGETADDED(Type.FLOAT,2),
    TARGETSCLEARED(Type.NONE,0);
    Type type;
    int numVals;
    SendDataType(Type t, int n){type=t;numVals=n;}
    enum Type{STRING,FLOAT,NONE}
}

public class RemoteServer{
    ServerThread mServerThread;
    Socket mMainSocket;
    MainActivity mMainAct;
    int mPort;
    String mIP;
    int mConections;
    boolean mRunning;

    public RemoteServer(MainActivity parent, Integer port){
        mMainAct=parent;
        mPort=port;
        mServerThread=new ServerThread(port);
    }

    public void start(){
        mIP=getIPAddress();
        mMainAct.setStatusRemoteServer(mIP,0,false,0);
        mMainAct.dump("Starting server...\nIP " + getIPAddress() + "\nPort: "+mPort);
        new Thread(mServerThread).start();
   }

    public void restart(){
        stop();
        start();
    }

    public void stop(){
        mMainAct.dump("Stopping server");
        mServerThread.stop();
    }

    public void sendData(SendDataType type, String sval, float[] fvals){
        mServerThread.sendData(type,sval,fvals);
    }

    private class ServerThread implements Runnable{
        int mPort;
        ServerSocket mServerSocket;
        List<ConnectionThread> mConnections=new ArrayList<>();
        //Constructor
        ServerThread(int port){
            mPort=port;
        }
        @Override
        public void run(){
            try{
                Log.w("SERVERTHREAD","start of server server");
                mMainAct.setStatusRemoteServer(mIP,0,true,0);
                mServerSocket=new ServerSocket(mPort);
                mMainAct.setStatusRemoteServer(mIP,mPort,true,0);
                Log.e("TAG","SetremoteStat");
                while(mServerSocket!=null && !mServerSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    ConnectionThread c;
                    Log.w("SERVERTHREAD","waiting for connection");
                    c=new ConnectionThread(mServerSocket.accept(), true);
                    mConnections.add(c);
                    new Thread(c).start();
                    countConnections();
                }
            }catch(IOException e){/*probably socket closed*/}
            stop();
            mMainAct.setStatusRemoteServer(mIP,0,false,0);
        }
        //sendMessage()
        void sendData(SendDataType type, String sval, float[] fvals){
            for(ConnectionThread c:mConnections){
                c.sendData(type,sval,fvals);
            }
        }
        //stop()
        public void stop(){
            for(ConnectionThread c :mConnections){
                c.stop();
            }
            if(mServerSocket != null){
                try{
                    mServerSocket.close();
                }catch(IOException e){}
            }
        }
        public void countConnections(){
            int count=0;
            for(ConnectionThread d:mConnections){
                if ((d.mSocket != null && !d.mSocket.isClosed()))
                    count++;
            }
            mMainAct.setStatusRemoteServer(mIP,mPort,true,count);
        }
    }


    private class ConnectionThread implements Runnable{
        public Socket mSocket;
        private boolean mEcho;
        private BufferedReader mIn;
        private DataOutputStream mOut;

        //Constructor
        ConnectionThread(Socket socket, boolean echo){
            mSocket=socket;
            mEcho=echo;
            mMainAct.dump("New Connection with " + socket.getInetAddress());
        }

        @Override
        public void run(){
            try{
                Log.w("CONNETIONTHREAD","start of connection");

                mIn=new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOut=new DataOutputStream(mSocket.getOutputStream());
                sendData(SendDataType.STRINGCOMMAND,"Hello from Tango",null);
                mMainAct.sendAllStatusItems();
                while(mSocket!=null && !mSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    Log.w("CONNETIONTHREAD","waiting for readline");
                    String rx=mIn.readLine();
                    if(rx == null){
                        if (mSocket != null)
                            mSocket.close();
                        break;
                    }
                    if(mEcho)
                        sendData(SendDataType.STRINGCOMMAND,rx,null);
                    sendActionMessage(rx);
                }
            }catch(IOException e){/*probably socket closed*/
                Log.e("CONNETIONTHREAD", "IOEXC " + e.getMessage());
                mSocket = null;
            }
            mServerThread.countConnections();
        }

        public synchronized void sendData(SendDataType type, String sval, float[] fvals){
            if(mSocket == null || !mSocket.isConnected() || mOut == null)
                return;
            try{
                mOut.writeInt(type.ordinal());
                switch(type.type){
                    case STRING:
                        byte[] b=sval.getBytes(StandardCharsets.UTF_8);
                        mOut.writeInt(b.length);
                        mOut.write(b);
                        break;
                    case FLOAT:
                        for(int i=0; i<type.numVals; i++)
                            mOut.writeFloat(fvals[i]);
                        break;
                    case NONE:
                        break;
                }
            }catch(IOException e){
                e.printStackTrace();
                mSocket = null;
            }
        }

        public void stop(){
            if(mSocket != null && !mSocket.isClosed()){
                try{
                    Log.w("STP","closing socket");
                    mSocket.close();
                }catch(IOException e){}
            }
        }
    }


    private void sendActionMessage(String msg){
        switch(msg){
            case "Forward":
                mMainAct.actionCommand(Robot.Commands.FORWARD);
                break;
            case "Reverse":
                mMainAct.actionCommand(Robot.Commands.REVERSE);
                break;
            case "Stop":
                mMainAct.actionCommand(Robot.Commands.STOP);
                break;
            case "Right":
                mMainAct.actionCommand(Robot.Commands.SPINRIGHT);
                break;
            case "Left":
                mMainAct.actionCommand(Robot.Commands.SPINLEFT);
                break;
            case "Rightish":
                mMainAct.actionCommand(Robot.Commands.HALFRIGHT);
                break;
            case "Leftish":
                mMainAct.actionCommand(Robot.Commands.HALFLEFT);
                break;
            case "Go To Target":
                mMainAct.actionGo();
                break;
            case "Add Target":
                mMainAct.actionAddTarget();
                break;
            case "Clear Targets":
                mMainAct.actionClearTargets();
                break;
            case "Stop Everything":
                mMainAct.actionStopEverything();
                break;
            case "Learn ADF":
                mMainAct.actionLearnADF();
                mMainAct.dump("Learn Command Rxd");
                break;
            case "Save ADF":
                mMainAct.actionSaveADF();
                mMainAct.dump("Save Command Rxd");
                break;
            case "Start Path":
                mMainAct.mRobot.startSavingPath();
                break;
            case "End Path":
                mMainAct.mRobot.stopSavingPath();
                break;
            case "Trace Path Forward":
                mMainAct.mRobot.tracePathForward();
                break;
            case "Trace Path Reverse":
                mMainAct.mRobot.tracePathReverse();
                break;
        }
        if(msg.startsWith("Save ADF")){
            Log.w("SAVE","saveadfw name "+msg);
            String[] substr=msg.split("%");
            if(substr.length<2)
                return;
            mMainAct.dump("Save as command ("+substr[1]+")");
            mMainAct.actionSaveADFName(substr[1]);
        }
    }

    public static String getIPAddress(){
        try{
            for(NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())){
                for(InetAddress addr : Collections.list(nif.getInetAddresses())){
                    if(!addr.isLoopbackAddress() && addr instanceof Inet4Address){
                        return addr.getHostAddress();
                    }
                }
            }
        }catch(SocketException e){
        }
        return "No IP";
    }
}

