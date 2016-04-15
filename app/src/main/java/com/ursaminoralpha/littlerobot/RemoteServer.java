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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;


public class RemoteServer{
    private ServerSocket serverSocket;
    private Socket readSocket;
    Thread serverThread = null;
    MainActivity mainActivity;

    public RemoteServer(MainActivity parent) {
        this.mainActivity=parent;
    }

    public void serverStart(){
        serverStop();
        String ip=getIPAddress();
        serverThread = new Thread(new ServerThread());
        serverThread.start();
        mainActivity.dump("Starting server...\nIP " + ip + "\nPort: 6242");
    }

    public void serverRestart(){
        serverStart();
    }

    public void serverStop(){
        if(serverSocket!=null){
            try{
                if(serverSocket.isBound()){
                    if(readSocket != null && readSocket.isConnected())
                        readSocket.close();
                    serverSocket.close();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        if(serverThread!=null && serverThread.isAlive()){
            serverThread.interrupt();
        }
    }


    class ServerThread implements Runnable {
        public void run() {
            mainActivity.setServerStatus("IP: " + getIPAddress() + " :" + 6242);
            try{
                serverSocket = new ServerSocket(6242);
                readSocket = serverSocket.accept();
                BufferedReader in=new BufferedReader(new InputStreamReader(readSocket.getInputStream()));

                while(!Thread.currentThread().isInterrupted()){
                    mainActivity.setServerStatus("Remote Connected");
                    String read = in.readLine();
                    if(read==null){
                        serverRestart();
                    }else{
                        //got a string, do something with it
                        sendMessage(read);
                    }
                }
            }catch(IOException e){mainActivity.dump("Server exc: "+e.getMessage());}

            mainActivity.setServerStatus("Remote Server Stopped");
        }
    }

    void sendMessage(String msg){
        if(msg.equals("Forward")){
            mainActivity.actionCommand(Robot.Commands.FORWARD);
        }
        else if(msg.equals("Reverse")){
            mainActivity.actionCommand(Robot.Commands.REVERSE);
        }
        else if(msg.equals("Stop")){
            mainActivity.actionCommand(Robot.Commands.STOP);
        }
        else if(msg.equals("Right")){
            mainActivity.actionCommand(Robot.Commands.SPINRIGHT);
        }
        else if(msg.equals("Left")){
            mainActivity.actionCommand(Robot.Commands.SPINLEFT);
        }
        else if(msg.equals("Go To Target")){
            mainActivity.actionGo();
        }
        else if(msg.equals("Add Target")){
            mainActivity.actionAddTarget();
        }
        else if(msg.equals("Clear Targets")){
            mainActivity.actionClearTargets();
        }
        else if(msg.equals("Stop Everything")){
            mainActivity.actionStopEverything();
        }
        else if(msg.equals("Learn ADF")){
            mainActivity.actionLearnADF();
            mainActivity.dump("Learn Command Rxd");
        }
        else if(msg.equals("Save ADF")){
            mainActivity.actionSaveADF();
            mainActivity.dump("Save Command Rxd");
        }
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "No IP Address Found";
    }
}
