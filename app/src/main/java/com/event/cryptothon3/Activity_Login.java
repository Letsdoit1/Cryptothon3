package com.event.cryptothon3;

import static com.event.cryptothon3.NetworkChecker.isNetworkAvailable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.event.cryptothon3.models.RegistrationDetails;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Activity_Login extends AppCompatActivity {
    public static final String TAG = "Activity_Login";
    private FirebaseFunctions mFunctions;

    private String deviceId;

    String errorReceived;

    String alreadyRegistered;

    String code = null;
    String msg = null;

    String availableTime;
    AlertDialog alertDialog;
    private ScrollView scrollView;

    private VideoView animView2;

//    private MediaPlayer buttonSound;

    private View gifOverlay;
    private String currentScore = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mFunctions = FirebaseFunctions.getInstance();
        if (FirebaseHelper.EMULATOR_RUNNING)
            mFunctions.useEmulator("10.0.2.2", 5001);

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        scrollView = findViewById(R.id.scrollView);
        FirebaseHelper.setEdgeToEdgeInsets(scrollView);
        ImageButton btnLogin = findViewById(R.id.btnLogin);
        animView2 = findViewById(R.id.animView2);
        gifOverlay=findViewById(R.id.gifOverlay);

//        buttonSound = MediaPlayer.create(Activity_Login.this, R.raw.button_sound);

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                scrollView.getWindowVisibleDisplayFrame(r);
                int screenHeight = scrollView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // Assume that the keyboard is at least 15% of the screen height
                    int[] location = new int[2];
                    btnLogin.getLocationOnScreen(location);
                    int y = location[1];
                    scrollView.smoothScrollTo(0, y + btnLogin.getHeight());
                }
            }
        });

        ((EditText)findViewById(R.id.txtPwd)).setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                                || (actionId == EditorInfo.IME_ACTION_GO)
                                || (actionId == EditorInfo.IME_ACTION_DONE)
                                || (actionId == EditorInfo.IME_ACTION_SEND)) {
                            findViewById(R.id.btnLogin).performClick();
                        }
                        return false;
                    }
                }
        );
    }

    protected void onResume() {
        super.onResume();
        findViewById(R.id.btnLogin).setEnabled(true);
        // Animation for login button
        FrameLayout flLoginButton = findViewById(R.id.layoutBtn);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
        flLoginButton.startAnimation(animation);
        findViewById(R.id.btnLogin).setEnabled(true);

//        buttonSound = MediaPlayer.create(Activity_Login.this, R.raw.button_sound);

        EditText txtPwd = findViewById(R.id.txtPwd);
        ImageView imgLogo = findViewById(R.id.imgIcon);
        ImageView imgEnterPwd = findViewById(R.id.imgEnterPwd);
        Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.move_in_animation);
        imgLogo.startAnimation(animation1);
        imgEnterPwd.startAnimation(animation);
        txtPwd.startAnimation(animation);
    }

    public void onClickedLoginBtn(View view) {

//        if (buttonSound.isPlaying()) buttonSound.pause();
//        buttonSound.seekTo(0);
//        buttonSound.start();

        String pwd = ((TextView)findViewById(R.id.txtPwd)).getText().toString().toUpperCase();
        if(pwd == null || pwd.trim().isEmpty()) {
            Toast.makeText(Activity_Login.this, "Please enter shared password to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable(Activity_Login.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);


        animView2.setVisibility(View.VISIBLE);
        gifOverlay.setVisibility(View.VISIBLE);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loading);
        animView2.setVideoURI(videoUri);
        animView2.start();


       // findViewById(R.id.layoutBtn).setVisibility(View.GONE);
        findViewById(R.id.btnLogin).setEnabled(false);

      /*  animView2.setVisibility(View.VISIBLE); // Show ImageView
        gifOverlay.setVisibility(View.VISIBLE);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading) // Replace with your GIF resource
                .into(imageViewGif);*/
        checkPwdAndRegister(deviceId, pwd)
                .addOnCompleteListener(new OnCompleteListener<RegistrationDetails>() {
                    @Override
                    public void onComplete(@NonNull Task<RegistrationDetails> task) {

                        animView2.setVisibility(View.GONE);
                        gifOverlay.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {

                            Exception e = task.getException();
                            String error = null;
                            if (e instanceof FirebaseFunctionsException)
                                error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                            else
                                error = "FirebaseFunctionException Msg = " + e.getMessage();
                            error = "DeviceId=" + deviceId + ", registration(), " + error;
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                            return;
                        }
                        if (code != null){
                            Intent intent = new Intent(Activity_Login.this, Activity_EventState.class);
                            intent.putExtra("EVENT_CODE",code);
                            intent.putExtra("EVENT_MSG", msg);
                            intent.putExtra("Available_Time",availableTime);
                            intent.putExtra("Team_Score",currentScore);
                            startActivity(intent);
                            finish();
                            return;
                        }
                        RegistrationDetails rd = task.getResult();
                        if (rd == null){
                            Log.d("ERROR_MSG", errorReceived);
                            showRestartDialog(getResources().getString(R.string.error),errorReceived);
                        } else if (rd != null && rd.isWrongTeamCode()) {
                            Toast.makeText(Activity_Login.this, getString(R.string.wrong_pwd_msg), Toast.LENGTH_SHORT).show();
                        } else if (rd.isRegisteredSuccessfully()) {
                            Intent intent = new Intent(Activity_Login.this, MainActivity.class);
                            intent.putExtra("TEAM_CODE", pwd);
                            startActivity(intent);
                            finish();
                        } else {
                            String tmsg = "Max number of devices already registered.";
                            ArrayList<String> registeredDevices = rd.getRegisteredDevices();
                            for (int i=0; i<registeredDevices.size(); i++){
                                tmsg += "Device" + (i+1) + ": " + registeredDevices.get(i) + ", ";
                            }

                            Toast.makeText(Activity_Login.this, tmsg, Toast.LENGTH_LONG).show();

                        }

                        findViewById(R.id.btnLogin).setEnabled(true);
                        //findViewById(R.id.layoutBtn).setVisibility(View.VISIBLE);
                    }
                });
    }

    private Task<RegistrationDetails> checkPwdAndRegister(String deviceId, String pwd) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("teamCode", pwd);
        return mFunctions.getHttpsCallable("checkPwdAndRegister")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, RegistrationDetails>() {
                    @Override
                    public RegistrationDetails then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();

                        if(result == null) {
                            errorReceived = "checkPwdAndRegister(): null result received from Server";
                            return null;
                        } else if(result.containsKey("error")) {
                            errorReceived = "checkPwdAndRegister(): Error Received: " + result.get("error").toString();
                            return null;
                        }
                        if (result.containsKey("code")){
                            code = result.get("code").toString();
                            msg = result.get("msg").toString();
                            if(result.containsKey("time") && result.get("time") != null)
                                availableTime=result.get("time").toString();
                            if(result.containsKey("teamScore") && result.get("time") != null)
                                currentScore = result.get("teamScore").toString();
                            return null;
                        }
                        if(result.containsKey("alreadyRegistered")) {
                            alreadyRegistered = (String) result.get("alreadyRegistered");
                            Toast.makeText(Activity_Login.this, "Device already registered with pwd: "+alreadyRegistered, Toast.LENGTH_SHORT).show();
                            RegistrationDetails rd = new RegistrationDetails();
                            rd.setWrongTeamCode(false);
                            rd.setRegisteredSuccessfully(true);
                            return rd;
                        }
                        RegistrationDetails rd = new RegistrationDetails();
                        rd.setWrongTeamCode((boolean) result.get("wrongTeamCode"));
                        rd.setRegisteredSuccessfully((boolean) result.get("registeredSuccessfully"));
                        rd.setRegisteredDevices((ArrayList<String>) result.get("registeredDevices"));

                        return rd;
                    }
                });
    }

//    public void togglePasswordVisibility(View view) {
//        EditText passwordEditText = findViewById(R.id.txtPwd);
//        ImageButton toggleButton = findViewById(R.id.btnShowPwd);
//
//        if (passwordEditText.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
//            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//            toggleButton.setImageResource(R.drawable.ic_visibility_off);
//        } else {
//            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//            toggleButton.setImageResource(R.drawable.ic_visibility_off);
//        }
//
//        // Move cursor to the end of the text for better user experience
//        passwordEditText.setSelection(passwordEditText.getText().length());
//    }

//    private void showErrorDialog(String message) {
//        findViewById(R.id.btnLogin).setEnabled(true);
//        new AlertDialog.Builder(this)
//                .setTitle("Error")
//                .setMessage(message)
//                .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        onResume(); // Refresh the activity
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setCancelable(false)
//                .show();
//    }

    private void showRefreshDialog(String message){
        findViewById(R.id.btnLogin).setEnabled(true);

        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Login.this);
        View view = LayoutInflater.from(Activity_Login.this).inflate(R.layout.dialog_layout,viewGroup,false);
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
        findViewById(R.id.btnLogin).setEnabled(true);
        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Login.this);
        View view = LayoutInflater.from(Activity_Login.this).inflate(R.layout.dialog_layout,viewGroup,false);
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
                startActivity(new Intent(Activity_Login.this, Activity_welcome_screen.class));
            }
        });
        view.findViewById(R.id.txtDialogCancel).setVisibility(View.GONE);
        alertDialog.show();
    }
}
