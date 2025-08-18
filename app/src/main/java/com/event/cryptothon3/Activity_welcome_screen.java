package com.event.cryptothon3;

import static com.event.cryptothon3.NetworkChecker.isNetworkAvailable;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.event.cryptothon3.models.RegistrationStatus;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class Activity_welcome_screen extends AppCompatActivity {
    private static final String TAG = "Activity_welcome_screen";
    private FirebaseFunctions mFunctions;
    String teamPwd;
    String deviceId;
    String code;
    String msg;

    String availableTime;
    long DOUBLE_CLICK_TIME_DELTA = 499;
    long lastClickTime = 0;
    AlertDialog alertDialog;
    private String currentScore="";

    VideoView videoview;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);
        VideoView vv = findViewById(R.id.videoView);
        deviceId=Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);


//        buttonSound = MediaPlayer.create(Activity_welcome_screen.this, R.raw.button_sound);

        vv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(Activity_welcome_screen.this, "DeviceId: " + deviceId,Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
        // Video Animation
        VideoView videoview = findViewById(R.id.videoView);
        videoview.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.audio_video_of_front_page_animation);
        videoview.setVideoURI(uri);
        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // This method is called when the video playback is complete
                videoview.stopPlayback(); // Stop the VideoView
            }
        });
        videoview.start();
        // Button Animation
        FrameLayout flStartButton = findViewById(R.id.layoutBtn);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
        flStartButton.startAnimation(animation);
        findViewById(R.id.btnStart).setEnabled(true);

        if (!isNetworkAvailable(Activity_welcome_screen.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }else{
            if(alertDialog!=null)
                alertDialog.dismiss();
        }

//        buttonSound = MediaPlayer.create(Activity_welcome_screen.this, R.raw.button_sound);

    }

    private Task<RegistrationStatus> getRegistrationStatus(String deviceId) {
        Map<String,Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        return mFunctions.getHttpsCallable("isRegisteredDevice")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, RegistrationStatus>() {
                    @Override
                    public RegistrationStatus then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        RegistrationStatus rs = new RegistrationStatus();
                        rs.setRegistered((Boolean) result.get("registrationStatus"));
                        rs.setTeamPassword((String) result.get("teamPassword"));
                        return rs;
                    }
                });
    }

    private Task<String> getEventState() {
        Map<String,Object> data = new HashMap<>();
        data.put("teamCode", teamPwd);
        return mFunctions.getHttpsCallable("getEventState")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        code = result.get("code").toString();
                        msg = result.get("msg").toString();
                        if(result.containsKey("time") && result.get("time") != null)
                            availableTime=result.get("time").toString();
                        if(result.containsKey("teamScore") && result.get("time") != null)
                            currentScore = result.get("teamScore").toString();
                        return code;
                    }
                });
    }

    public void onStartQuizClicked(View view) {
        if (!isNetworkAvailable(Activity_welcome_screen.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        videoview = (VideoView) findViewById(R.id.videoView);
        videoview.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.loading);
        videoview.setVideoURI(uri);
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // This method is called when the video playback is complete
                videoview.stopPlayback(); // Stop the VideoView
            }
        });
        videoview.start();


//        if (buttonSound.isPlaying()) buttonSound.pause();
//        buttonSound.seekTo(0);
//        buttonSound.start();


        ImageButton btnOk = findViewById(R.id.btnStart);
        TextView btnText=findViewById(R.id.btnText);

        btnOk.setVisibility(View.GONE);
        btnText.setVisibility(View.GONE);
        btnOk.setEnabled(false);

        mFunctions = FirebaseFunctions.getInstance();

        if (FirebaseHelper.EMULATOR_RUNNING)
            mFunctions.useEmulator("10.0.2.2", 5001);
        //Getting DeviceID or AnroidID
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        getRegistrationStatus(deviceId)
                .addOnCompleteListener(new OnCompleteListener<RegistrationStatus>() {
                    @Override
                    public void onComplete(@NonNull Task<RegistrationStatus> task) {

                        if (!task.isSuccessful()) {
                            btnOk.setVisibility(View.VISIBLE);
                            btnText.setVisibility(View.VISIBLE);
                            Exception e = task.getException();
                            String error = null;
                            if (e instanceof FirebaseFunctionsException)
                                error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                            else
                                error = "FirebaseFunctionException Msg = " + e.getMessage();
                            error = "DeviceId=" + deviceId + ", getEventState(), " + error;
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                            return;
                        }
                        RegistrationStatus isRegistered = null;
                        isRegistered = task.getResult();
                        // findViewById(R.id.btnStart).setVisibility(View.VISIBLE);
                        if (isRegistered!=null && isRegistered.isRegistered()){
                            teamPwd = isRegistered.getTeamPassword();

                            getEventState()
                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                        @Override
                                        public void onComplete(@NonNull Task<String> task) {

                                            if (!task.isSuccessful()) {
                                                btnOk.setVisibility(View.VISIBLE);
                                                btnText.setVisibility(View.VISIBLE);
                                                Exception e = task.getException();
                                                String error = null;
                                                if (e instanceof FirebaseFunctionsException)
                                                    error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                                                else
                                                    error = "FirebaseFunctionException Msg = " + e.getMessage();
                                                error = "DeviceId=" + deviceId + ", isRegistered(), " + error;
                                                Log.w(TAG, error);

                                                showRestartDialog(getResources().getString(R.string.error),error);
                                                return;
                                            }
                                            String res = null;
                                            res = task.getResult();
                                            if (!code.equals("EventRunning")){
                                                Intent intent = new Intent(Activity_welcome_screen.this, Activity_EventState.class);
                                                intent.putExtra("EVENT_CODE",code);
                                                intent.putExtra("EVENT_MSG", msg);
                                                intent.putExtra("Available_Time",availableTime);
                                                intent.putExtra("Team_Score",currentScore);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                                startActivity(intent);
                                                finish();
                                                return;
                                            }

                                            if(teamPwd!=null){
                                                Intent intent = new Intent(Activity_welcome_screen.this, MainActivity.class);
                                                intent.putExtra("TEAM_CODE",teamPwd);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                startActivity(new Intent(Activity_welcome_screen.this, Activity_Login.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                                                finish();
                                            }
                                        }
                                    });
                        }else{
                            startActivity(new Intent(Activity_welcome_screen.this, Activity_Login.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                            finish();
                        }
                    }
                });
    }

    public void lblLogoClickShowDeviceId(View view) {
        long clickTime = SystemClock.elapsedRealtime();

        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
            Toast.makeText(Activity_welcome_screen.this, "DeviceId: " + deviceId,Toast.LENGTH_SHORT).show();
        }
        lastClickTime = clickTime;
    }

    private void showRefreshDialog(String message){
        findViewById(R.id.btnStart).setEnabled(true);

        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(Activity_welcome_screen.this);
        View view = LayoutInflater.from(Activity_welcome_screen.this).inflate(R.layout.dialog_layout,viewGroup,false);
//        builder.setCancelable(false);
        builder.setView(view);

        alertDialog = builder.create();
        try { alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); }catch(Exception e){}
        ((TextView)view.findViewById(R.id.txtDialogOnButton)).setText(R.string.ok);
        view.findViewById(R.id.btnDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
//                onResume(); // Refresh the activity
            }
        });
        view.findViewById(R.id.txtDialogCancel).setVisibility(View.GONE);
        view.findViewById(R.id.txtDialogCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        ((TextView)view.findViewById(R.id.textView)).setText(message);
        alertDialog.show();
    }
    private void showRestartDialog(String message, String errorTxt){
        findViewById(R.id.btnStart).setEnabled(true);
        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(Activity_welcome_screen.this);
        View view = LayoutInflater.from(Activity_welcome_screen.this).inflate(R.layout.dialog_layout,viewGroup,false);
        builder.setCancelable(false);
        builder.setView(view);

        AlertDialog alertDialog = builder.create();
        try { alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); }catch(Exception e){}
        TextView txtMsgView = view.findViewById(R.id.textView);
        txtMsgView.setText(message);
        ((ImageView)view.findViewById(R.id.logoView)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                txtMsgView.setText(errorTxt);
                txtMsgView.setTextSize(12);
                return true;
            }
        });
        ((TextView)view.findViewById(R.id.txtDialogOnButton)).setText(R.string.restart);
        view.findViewById(R.id.btnDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Close error activity
                startActivity(new Intent(Activity_welcome_screen.this, Activity_welcome_screen.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            }
        });
        view.findViewById(R.id.txtDialogCancel).setVisibility(View.GONE);
        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoview != null && videoview.isPlaying()) {
            videoview.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoview != null) {
            videoview.stopPlayback();
        }
    }
}
