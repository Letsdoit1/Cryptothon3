package com.event.cryptothon3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Activity_EventState extends AppCompatActivity {
    TextView txtMsg;

    long lastClickTime = 0;
    long DOUBLE_CLICK_TIME_DELTA = 499;

    String deviceId;

    Integer availableTime;

    CountDownTimer mCounter;

    String teamName="";

    String teamScore="";

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventstate);

        View view = findViewById(R.id.activity_layout_eventstate);
        FirebaseHelper.setEdgeToEdgeInsets(view);

        Intent intent = getIntent();
        String msg = intent.getStringExtra("EVENT_MSG");
        String code = intent.getStringExtra("EVENT_CODE");
        String aT=intent.getStringExtra("Available_Time");

//        teamName=intent.getStringExtra("Team_Name");
        teamScore=intent.getStringExtra("Team_Score");


        if(aT != null) {
            availableTime = Integer.valueOf(aT);
            createAndShowTimer(availableTime, 1000);
        }else{
            ((TextView)findViewById(R.id.lblTimer)).setVisibility(View.INVISIBLE);
        }

        txtMsg = findViewById(R.id.textView);
//        ((TextView)findViewById(R.id.teamName)).setText(teamName);

        // TDB
        if(teamScore == null || teamScore.isEmpty() )
        {
//            findViewById(R.id.teamName).setVisibility(View.GONE);
            findViewById(R.id.teamScoreLable).setVisibility(View.INVISIBLE);
        }else{
            ((TextView)findViewById(R.id.teamScoreLable)).setText("Score: " +teamScore);
        }
        txtMsg.setMovementMethod(ScrollingMovementMethod.getInstance());
        txtMsg.setText(msg);

        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Activity_EventState.this, Activity_welcome_screen.class);
                startActivity(intent);
                finish();
            }
        });



        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        txtMsg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(Activity_EventState.this, "DeviceId: " + deviceId, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
//        VideoView videoview = (VideoView) findViewById(R.id.videoView);
//        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                mp.setLooping(true);
//            }
//        });
//        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.background_video);
//        videoview.setVideoURI(uri);
//        videoview.start();

        super.onResume();
        TextView textView = findViewById(R.id.textView);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
        textView.startAnimation(animation);
    }

    public void lblLogoClickShowDeviceId(View view) {
        long clickTime = SystemClock.elapsedRealtime();

        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
            Toast.makeText(Activity_EventState.this, "DeviceId: " + deviceId, Toast.LENGTH_SHORT).show();
        }
        lastClickTime = clickTime;
    }

    private void createAndShowTimer(Integer countdown, Integer tick){

        if (mCounter!=null)
            mCounter.cancel();
        mCounter = new CountDownTimer(countdown, tick) {
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long hour = (millisUntilFinished / 3600000);
                long min = (millisUntilFinished / 60000) % 60;
                long sec = (millisUntilFinished / 1000) % 60;
                ((TextView)findViewById(R.id.lblTimer)).setText(f.format(hour) + ":" + f.format(min) + ":" + f.format(sec));
            }
            public void onFinish() {
                Intent intent = new Intent(Activity_EventState.this, Activity_welcome_screen.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }


}