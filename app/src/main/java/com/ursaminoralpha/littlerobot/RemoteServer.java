package com.ursaminoralpha.littlerobot;

// TCP Server to connect a remote control
// Commands are strings followed by \n.
// "Forward\n", "Go To Target\n", etc.
// create object and call serverStart() to start.
//
// Runs separate thread which waits on connection and
// then the same thread continues and waits for data.
//
// Calls mainactivity.setServerStatus to tell it's status.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RemoteServer{
    ServerThread mServerThread;
    MainActivity mainActivity;
    int mPort;

    public RemoteServer(MainActivity parent, int port){
        mainActivity=parent;
        mPort=port;
        mServerThread=new ServerThread(port);
    }

    public void start(){
        mainActivity.dump("Starting server...\nIP " + getIPAddress() + "\nPort: "+mPort);
        new Thread(mServerThread).start();
   }

    public void restart(){
        stop();
        start();
    }

    public void stop(){
        mainActivity.dump("Stopping server");
        mServerThread.stop();
    }

    public void sendMessage(String mess){
        mServerThread.sendMessage(mess);
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
            mainActivity.setServerStatus("IP: " + getIPAddress() + " :" + 6242);
            try{
                mServerSocket=new ServerSocket(mPort);
                mainActivity.setServerStatus("Remote Server Listening for connection");
                while(!mServerSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    ConnectionThread c;
                    c=new ConnectionThread(mServerSocket.accept(), true);
                    mConnections.add(c);
                    new Thread(c).start();
                    int count=0;
                    for(ConnectionThread d:mConnections)
                        if(!d.mSocket.isClosed())
                            count++;
                    mainActivity.setServerStatus(count+" Remote Connections");
                }
            }catch(IOException e){
                mainActivity.dump("ServerThread.run() Excp: " + e.getMessage());
            }
            stop();
            mainActivity.setServerStatus("Remote Server Stopped");
        }
        //sendMessage()
        void sendMessage(String mess){
            for(ConnectionThread c :mConnections){
                c.sendMessage(mess);
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
                }catch(IOException e){
                    mainActivity.dump("ServerThread.stop() Excp: " + e.getMessage());
                }
            }
        }
    }


    private class ConnectionThread implements Runnable{
        public Socket mSocket;
        private boolean mEcho;
        private BufferedReader mIn;
        private PrintWriter mOut;

        //Constructor
        ConnectionThread(Socket socket, boolean echo){
            mSocket=socket;
            mEcho=echo;
            mainActivity.dump("New Connection with " + socket.getInetAddress());
        }

        @Override
        public void run(){
            try{
                mIn=new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOut=new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
                mOut.println("Hello from Tango");
                while(!mSocket.isClosed() && !Thread.currentThread().isInterrupted()){
                    String rx=mIn.readLine();
                    if(rx == null)
                        break;
                    if(mEcho)
                        mOut.println(rx);
                    sendActionMessage(rx);
                }
            }catch(IOException e){
                mainActivity.dump("Connection.run() Excp: " + e.getMessage());
            }
        }

        public void sendMessage(String mess){
            if(mSocket == null || !mSocket.isConnected() || mOut == null)
                return;
            mOut.println(mess);
        }

        public void stop(){
            if(mSocket != null && !mSocket.isClosed()){
                try{
                    mSocket.close();
                }catch(IOException e){
                    mainActivity.dump("Connection.stop() Excp: " + e.getMessage());
                }
            }
        }
    }


    private void sendActionMessage(String msg){
        switch(msg){
            case "Forward":
                mainActivity.actionCommand(Robot.Commands.FORWARD);
                break;
            case "Reverse":
                mainActivity.actionCommand(Robot.Commands.REVERSE);
                break;
            case "Stop":
                mainActivity.actionCommand(Robot.Commands.STOP);
                break;
            case "Right":
                mainActivity.actionCommand(Robot.Commands.SPINRIGHT);
                break;
            case "Left":
                mainActivity.actionCommand(Robot.Commands.SPINLEFT);
                break;
            case "Go To Target":
                mainActivity.actionGo();
                break;
            case "Add Target":
                mainActivity.actionAddTarget();
                break;
            case "Clear Targets":
                mainActivity.actionClearTargets();
                break;
            case "Stop Everything":
                mainActivity.actionStopEverything();
                break;
            case "Learn ADF":
                mainActivity.actionLearnADF();
                mainActivity.dump("Learn Command Rxd");
                break;
            case "Save ADF":
                mainActivity.actionSaveADF();
                mainActivity.dump("Save Command Rxd");
                break;
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
        return "No IP Address Found";
    }
}

