package com.ursaminoralpha.littlerobot;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StatusFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class StatusFragment extends Fragment{

    public static final int RED=0xFF800000;
    public static final int GREEN=0xFF008000;
    public static final int BLACK=0xFF000000;
    public static final int ORANGE=0xFF8d6101;

    public class Status{
        String adfName="";
        boolean localized;
        boolean learning;
        String poseStatus;
        Vec3 position=new Vec3();
        double rotation;
        boolean serialFound;
        boolean serialConnected;
        String ip="";
        int port;
        boolean serverRunning;
        int connections;
    }
    public Status mStat;

    private OnFragmentInteractionListener mListener;

    public StatusFragment(){
        mStat=new Status();
    }

    public void ADFName(String s){
        if(mStat.adfName.equals(s))
            return;
        mStat.adfName=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusADBName)).setText(mStat.adfName);
    }
    public void localized(boolean s){
        if(mStat.localized==s)
            return;
        mStat.localized=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusLocalized)).setTextColor(mStat.localized? GREEN:RED);
        ((TextView)act.findViewById(R.id.statusLocalized)).setText(mStat.localized? "YES" : "NO");
    }
    public void learning(boolean s){
        if(mStat.learning==s)
            return;
        mStat.learning=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusLearning)).setTextColor(mStat.learning? ORANGE: BLACK);
        ((TextView)act.findViewById(R.id.statusLearning)).setText(mStat.learning? "YES" : "NO");
    }
    public void poseStatus(String s){
        if(mStat.poseStatus.equals(s))
            return;
        mStat.poseStatus=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusPoseStatus)).setTextColor(mStat.poseStatus.equals("VALID")? GREEN: RED);
        ((TextView)act.findViewById(R.id.statusPoseStatus)).setText(mStat.poseStatus);
    }
    public void serialFound(boolean s){
        if(mStat.serialFound==s)
            return;
        mStat.serialFound=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusSerialFound)).setTextColor(mStat.serialFound? GREEN:RED);
        ((TextView)act.findViewById(R.id.statusSerialFound)).setText(mStat.serialFound? "YES" : "NO");
    }
    public void serialCon(boolean s){
        if(mStat.serialConnected==s)
            return;
        mStat.serialConnected=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusSerialCon)).setTextColor(mStat.serialConnected? GREEN:RED);
        ((TextView)act.findViewById(R.id.statusSerialCon)).setText(mStat.serialConnected? "YES" : "NO");
    }
    public void remoteIP(String s){
        if(mStat.ip.equals(s))
            return;
        mStat.ip=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusRemoteIP)).setText(mStat.ip + "");
    }
    public void remotePort(int s){
        if(mStat.port==s)
            return;
        mStat.port=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusRemotePort)).setText(mStat.port + "");
    }
    public void remoteRunning(boolean s){
        if(mStat.serverRunning==s)
            return;
        mStat.serverRunning=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusRemoteServer)).setTextColor(mStat.serverRunning? GREEN:RED);
        ((TextView)act.findViewById(R.id.statusRemoteServer)).setText(mStat.serverRunning? "YES" : "NO");
    }
    public void remoteConnections(int s){
        if(mStat.connections==s)
            return;
        mStat.connections=s;
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusRemoteCon)).setTextColor(mStat.connections>0?GREEN:RED);
        ((TextView)act.findViewById(R.id.statusRemoteCon)).setText(mStat.connections+"");
    }
    public void setPose(Vec3 position, double rotation){
        FragmentActivity act=getActivity();
        if(act==null)return;
        ((TextView)act.findViewById(R.id.statusTranslation)).setText(mStat.position.toString());
        ((TextView)act.findViewById(R.id.statusRotation)).setText(String.format("%.1f",mStat.rotation*(180/Math.PI)));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri){
        if(mListener != null){
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if(context instanceof OnFragmentInteractionListener){
            mListener=(OnFragmentInteractionListener)context;
        }else{
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mListener=null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener{
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
