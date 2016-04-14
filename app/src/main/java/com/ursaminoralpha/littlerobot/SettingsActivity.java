package com.ursaminoralpha.littlerobot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    Settings mCurrent;
    EditText mDistBig;
    EditText mDistSmall;
    EditText mAngleBig;
    EditText mAngleSmall;
    EditText mUpdateInterval;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mCurrent=getIntent().getParcelableExtra("settings");

        setContentView(R.layout.activity_settings);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Settings");

        findViewById(R.id.buttonOk).setOnClickListener(this);
        findViewById(R.id.buttonCancel).setOnClickListener(this);


        mDistBig=(EditText) findViewById(R.id.inputDistBig);
        mDistSmall=(EditText) findViewById(R.id.inputDistSmall);
        mAngleBig=(EditText) findViewById(R.id.inputAngleBig);
        mAngleSmall=(EditText) findViewById(R.id.inputAngleSmall);
        mUpdateInterval=(EditText) findViewById(R.id.inputUpdate);
        setValues();

//        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.pickDistBig);
//        numberPicker.setValue(10);
//        numberPicker.setMaxValue(100);
//        numberPicker.setMinValue(0);
//        numberPicker.setWrapSelectorWheel(true);
//        numberPicker.setOnValueChangedListener( new NumberPicker.
//                OnValueChangeListener() {
//            @Override
//            public void onValueChange(NumberPicker picker, int
//                    oldVal, int newVal) {
//                mCurrent.threshDistBig=newVal;
//            }
//        });


//        FloatingActionButton fab=(FloatingActionButton)findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view){
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onClick(View view){
        switch(view.getId()){
            case R.id.buttonOk:
                mCurrent.threshDistBig=Float.parseFloat(mDistBig.getText().toString())/100;
                mCurrent.threshDistSmall=Float.parseFloat(mDistSmall.getText().toString())/100;
                mCurrent.threshAngleSmall=Float.parseFloat(mAngleSmall.getText().toString())/(180/(float)Math.PI);
                mCurrent.threshAngleBig=Float.parseFloat(mAngleBig.getText().toString())/(180/(float)Math.PI);
                mCurrent.updateInterval=Float.parseFloat(mUpdateInterval.getText().toString());
                writePrefs();
                break;
            case R.id.buttonCancel:
                break;

        }
        Intent i=new Intent();
        i.putExtra("settings",mCurrent);
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    void setValues(){
        mDistSmall.setText(String.format("%.1f",mCurrent.threshDistSmall*100));
        mDistBig.setText(String.format("%.1f",mCurrent.threshDistBig*100));
        mAngleBig.setText(String.format("%.1f",mCurrent.threshAngleBig*(180/Math.PI)));
        mAngleSmall.setText(String.format("%.1f",mCurrent.threshAngleSmall*(180/Math.PI)));
        mUpdateInterval.setText(String.format("%.0f",mCurrent.updateInterval));
    }

    void writePrefs(){
        SharedPreferences pref=this.getSharedPreferences("Prefs",Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putFloat("ThreshDistBig",mCurrent.threshDistBig);
        editor.putFloat("ThreshDistSmall",mCurrent.threshDistSmall);
        editor.putFloat("ThreshAngleBig",mCurrent.threshAngleBig);
        editor.putFloat("ThreshAngleSmall",mCurrent.threshAngleSmall);
        editor.putFloat("UpdateRate",mCurrent.updateInterval);
        editor.commit();
    }

}
