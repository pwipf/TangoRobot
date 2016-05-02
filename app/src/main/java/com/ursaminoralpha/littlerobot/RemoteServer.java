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
    TARGETADDED(Type.BOTH,2),
    TARGETSCLEARED(Type.NONE,0),
    DEPTHDATA(Type.FLOAT,3),
    ADDOBSTACLE(Type.FLOAT,2);
    Type type;
    int numVals;
    SendDataType(Type t, int n){type=t;numVals=n;}
    enum Type{STRING,FLOAT,NONE,BOTH}
}

public class RemoteServer{
    
    public interface RemoteReceiver{
        void onStatusUpdate(String ip, int port, boolean running, int connections);
        void onNewConnection(String remoteIP);
        void onReceivedMessage(String message);
    }

    private static final int MAXCONNECTIONS=3;
    private ServerThread mServerThread;
    private RemoteReceiver mReceiver;
    private int mPort;
    private String mIP;
    private boolean mEcho;

    public RemoteServer(RemoteReceiver remoteReceiver, Integer port, boolean echo){
        mReceiver=remoteReceiver;
        mPort=port;
        mServerThread=new ServerThread(port);
        mEcho=echo;
    }

    public void start(){
        mIP=getIPAddress();
        mReceiver.onStatusUpdate(mIP,0,false,0);
        Log.i("REMOTESERVER","Starting server...\nIP " + getIPAddress() + "\nPort: "+mPort);
        new Thread(mServerThread).start();
   }

    public void restart(){
        stop();
        start();
    }

    public void stop(){
        Log.i("REMOTESERVER","Stopping");
        mServerThread.stop();
    }

    public void sendData(SendDataType type, String sval, float[] fvals){
        mServerThread.sendData(type,sval,fvals);
    }

    public void setEcho(boolean echo){
        mEcho=echo;
    }

    private class ServerThread implements Runnable{
        int mPort;
        ServerSocket mServerSocket;
        ConnectionThread[] mConnectionArray=new ConnectionThread[MAXCONNECTIONS];
        int mConIndex=0;
        //Constructor
        ServerThread(int port){
            mPort=port;
        }
        @Override
        public void run(){
            try{
                Log.i("SERVERTHREAD","start of server thread");
                mReceiver.onStatusUpdate(mIP,0,true,0);
                mServerSocket=new ServerSocket(mPort);
                mReceiver.onStatusUpdate(mIP,mPort,true,0);
                while(mServerSocket!=null && !mServerSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    ConnectionThread c;
                    Log.i("SERVERTHREAD","waiting for connection");
                    c=new ConnectionThread(mServerSocket.accept());
                    mConnectionArray[mConIndex]=c;
                    mConIndex++;if(mConIndex==MAXCONNECTIONS)mConIndex=0;
                    new Thread(c).start();
                    countConnections();
                }
            }catch(IOException e){/*probably socket closed*/}
            stop();
            mReceiver.onStatusUpdate(mIP,0,false,0);
            Log.i("SERVERTHREAD","end of server thread");
        }
        //sendMessage()
        void sendData(SendDataType type, String sval, float[] fvals){
            for(ConnectionThread c:mConnectionArray){
                if(c!=null)
                    c.sendData(type,sval,fvals);
            }
        }
        //stop()
        public void stop(){
            for(ConnectionThread c :mConnectionArray){
                if(c!=null)
                    c.stop();
            }
            if(mServerSocket != null){
                try{
                    mServerSocket.close();
                }catch(IOException e){}
            }
        }
        public void countConnections(){ // TODO concurrent modification exception possible, probably on close
            int count=0;
            for(ConnectionThread d:mConnectionArray){
                if(d!=null)
                    if ((d.mSocket != null && !d.mSocket.isClosed()))
                    count++;
            }
            mReceiver.onStatusUpdate(mIP,mPort,true,count);
        }
    }


    private class ConnectionThread implements Runnable{
        public Socket mSocket;
        private BufferedReader mIn;
        private DataOutputStream mOut;

        //Constructor
        ConnectionThread(Socket socket){
            mSocket=socket;
        }

        @Override
        public void run(){
            try{
                Log.i("CONNETIONTHREAD","start thread");
                mIn=new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOut=new DataOutputStream(mSocket.getOutputStream());
                mReceiver.onNewConnection(mSocket.getInetAddress().toString());
                while(mSocket!=null && !mSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    Log.i("CONNETIONTHREAD","waiting for readline");
                    String rx=mIn.readLine();
                    if(rx == null){
                        if (mSocket != null)
                            mSocket.close();
                        break;
                    }
                    if(mEcho)
                        sendData(SendDataType.STRINGCOMMAND,rx,null);
                    mReceiver.onReceivedMessage(rx);
                }
            }catch(IOException e){/*probably socket closed*/
                Log.w("CONNETIONTHREAD", "IOEXC " + e.getMessage());
                mSocket = null;
            }
            mServerThread.countConnections();
            Log.i("CONNETIONTHREAD","end thread");
        }

        public synchronized void sendData(SendDataType type, String sval, float[] fvals){
            if(mSocket == null || !mSocket.isConnected() || mOut == null)
                return;
            try{
                byte[] b;
                mOut.writeInt(type.ordinal());
                switch(type.type){
                    case STRING:
                        b=sval.getBytes(StandardCharsets.UTF_8);
                        mOut.writeInt(b.length);
                        mOut.write(b);
                        break;
                    case FLOAT:
                        for(int i=0; i<type.numVals; i++)
                            mOut.writeFloat(fvals[i]);
                        break;
                    case NONE:
                        break;
                    case BOTH:
                        for(int i=0; i<type.numVals; i++)
                            mOut.writeFloat(fvals[i]);

                        b=sval.getBytes(StandardCharsets.UTF_8);
                        mOut.writeInt(b.length);
                        mOut.write(b);

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
                    Log.i("STP","closing socket");
                    mSocket.close();
                }catch(IOException e){}
            }
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

