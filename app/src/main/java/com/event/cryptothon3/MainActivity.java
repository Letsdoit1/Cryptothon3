package com.event.cryptothon3;

import static com.event.cryptothon3.NetworkChecker.isNetworkAvailable;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.event.cryptothon3.models.QuestionData;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "CryptothonMainActivity";
    FirebaseFunctions mFunctions;
    String deviceId;
    QuestionData questionData;
    String teamCode;
    String hint;
    AlertDialog.Builder builder;
    private ImageButton btnUnlockHint;
    private TextInputLayout hintBox;
    private RelativeLayout hintUI;
    private TextInputEditText hintText;
    private CircularProgressIndicator spinner;
    CountDownTimer mCounter;
    private int oldQuestionLevel;
    private String hintsUsed;
    private String totalHints;
    private String availableHints;
    private  TextView btnText;
    String code = null;
    String msg = null;
    String updateScoreAfterTakingHint = null;

//    private MediaPlayer buttonSound;

    String availableTime;
    private MediaPlayer wrongAnsSound;
    private String currentScore="";

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (!isNetworkAvailable(MainActivity.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        if(hint!=null) {
            btnUnlockHint.setVisibility(View.GONE);
            hintBox.setVisibility(View.VISIBLE);
            hintText.setText(hint);
            btnText.setVisibility(View.GONE);
        }
        findViewById(R.id.btnSubmit).setEnabled(false);
        findViewById(R.id.btnUnlockHint).setEnabled(false);

//        showRefreshDialog(getResources().getString(R.string.internet_notavailable));
//        showRestartDialog(getResources().getString(R.string.error),"Error Msg");

        // Button Animation
        FrameLayout flSubmitButton = findViewById(R.id.layoutBtn);
//        FrameLayout flHintButton = findViewById(R.id.layoutBtnHint);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
        flSubmitButton.startAnimation(animation);
//        flHintButton.startAnimation(animation);

        Intent intent = getIntent();
        teamCode = intent.getStringExtra("TEAM_CODE");

//        buttonSound = MediaPlayer.create(MainActivity.this, R.raw.button_sound);
        wrongAnsSound = MediaPlayer.create(MainActivity.this, R.raw.wrongans_sound);

//        intent = new Intent(MainActivity.this, Activity_EventState.class);
//        intent.putExtra("EVENT_CODE",code);
//        intent.putExtra("EVENT_MSG", "Game not started.");
//        startActivity(intent);
//        finish();

        callToGetQuestion();
    }
    private void callToGetQuestion(){

        if (!isNetworkAvailable(MainActivity.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();
        code = null;
        getQuestion()
                .addOnCompleteListener(new OnCompleteListener<QuestionData>() {
                    @Override
                    public void onComplete(@NonNull Task<QuestionData> task) {
						findViewById(R.id.spinner).setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            String error = null;
                            if (e instanceof FirebaseFunctionsException)
                                error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                            else
                                error = "FirebaseFunctionException Code = " + e.getMessage();
                            error = "DeviceId=" + deviceId + ", getQuestion(), " + error;
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                            return;
                        }

                        if (code != null){
                            Intent intent = new Intent(MainActivity.this, Activity_EventState.class);
                            intent.putExtra("EVENT_CODE",code);
                            intent.putExtra("EVENT_MSG", msg);
                            intent.putExtra("Available_Time",availableTime);
//                            intent.putExtra("Team_Name",questionData.getTeamName());
                            intent.putExtra("Team_Score",currentScore);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                            return;
                        }
                        String error = null;
                        questionData = task.getResult();
                        if (questionData!=null){
                            if(questionData.getError()!=null){
                                error = "DeviceId=" + deviceId + ", getQuestion() Error from Server, " + questionData.getError();
                            }else {
                                updateUI();
                            }
                        }
                        else{
                            error = "DeviceId=" + deviceId + ", getQuestion(), " + "Question Data not received.";
                        }
                        if(error!=null){
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                        }
                    }
                });
    }
    private Task<QuestionData> getQuestion() {
        Map<String,Object> data = new HashMap<>();
        data.put("teamCode", teamCode);
        data.put("deviceId",deviceId);
        return mFunctions.getHttpsCallable("getQuestion")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, QuestionData>() {
                    @Override
                    public QuestionData then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        QuestionData qd = new QuestionData();
                        if(result == null || result.size()==0){
                            qd.setError("No Data received from Server.");
                        }else if (result.containsKey("code")){
                            code = result.get("code").toString();
                            msg = result.get("msg").toString();
                            if(result.containsKey("time") && result.get("time") != null)
                                availableTime=result.get("time").toString();
                            if(result.containsKey("teamScore") && result.get("time") != null)
                                currentScore = result.get("teamScore").toString();
                            return qd;
                        }else if (result.containsKey("error")) {
                            qd.setError((String)result.get("error"));
                        } else {
                            qd.setTime((Integer) result.get("time"));
                            qd.setLevel((Integer) result.get("level"));
                            qd.setRank((String) result.get("rank"));
                            qd.setMaxRank((Integer) result.get("maxRank"));
                            qd.setQuestion((String) result.get("question"));
                            qd.setHint((String) result.get("hint"));
                            if(qd.getHint()!=null)
                                hint = qd.getHint();
                            qd.setTeamName((String)result.get("teamName"));
                            qd.setAnsLength((Integer) result.get("ansLength"));
                            if(result.get("hintVisibility") == null)
                                qd.sethintVisibility(false);
                            else
                                qd.sethintVisibility((Boolean) result.get("hintVisibility"));
                            availableHints=result.get("availableHints").toString();
                            totalHints=result.get("totalHints").toString();
                            hintsUsed=String.valueOf(Integer.valueOf(totalHints)-Integer.valueOf(availableHints));
                        }
                        return qd;
                    }
                });
    }

    public void btnClickedSubmit(View view) {
//        if (buttonSound.isPlaying()) buttonSound.pause();
//        buttonSound.seekTo(0);
//        buttonSound.start();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        String ans = ((TextView)findViewById(R.id.txtAnswer)).getText().toString();
        if(ans == null || ans.trim().isEmpty())
        {
            Snackbar.make(view, "Answer is empty. Enter some value.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if(ans.length()!=questionData.getAnsLength()) {
            if (wrongAnsSound.isPlaying()) wrongAnsSound.pause();
            wrongAnsSound.seekTo(0);
            wrongAnsSound.start();
            Snackbar.make(view, "Wrong Answer, Try again.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable(MainActivity.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        findViewById(R.id.btnSubmit).setEnabled(false);
        findViewById(R.id.btnUnlockHint).setEnabled(false);
        findViewById(R.id.spinner).setVisibility(View.VISIBLE);

      /*  imageViewGif.setVisibility(View.VISIBLE); // Show ImageView
        gifOverlay.setVisibility(View.VISIBLE);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading) // Replace with your GIF resource
                .into(imageViewGif);*/
        code = null;
        checkAnswer(ans)
                .addOnCompleteListener(new OnCompleteListener<QuestionData>() {
                    @Override
                    public void onComplete(@NonNull Task<QuestionData> task) {
						findViewById(R.id.spinner).setVisibility(View.GONE);

                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            String error = null;
                            if (e instanceof FirebaseFunctionsException)
                                error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                            else
                                error = "FirebaseFunctionException Code = " + e.getMessage();
                            error = "DeviceId=" + deviceId + ", getQuestion(), " + error;
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                            return;
                        }
                        String error = null;
                        questionData = task.getResult();
                        if (questionData!=null){
                            if(questionData.getError()!=null){
                                error = "DeviceId=" + deviceId + ", getQuestion() Error from Server, " + questionData.getError();
                            }

                            if(questionData.getIsSuccess()){
                                VideoView videoview = findViewById(R.id.videoView);
                                videoview.setVisibility(View.VISIBLE);
                                videoview.setOnCompletionListener (new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mediaPlayer) {
                                        videoview.setVisibility(View.GONE);
                                        findViewById(R.id.btnSubmit).setEnabled(true);
                                        findViewById(R.id.btnUnlockHint).setEnabled(true);
                                        findViewById(R.id.txtAnswer).setEnabled(true);
                                        if (code != null){
                                            Intent intent = new Intent(MainActivity.this, Activity_EventState.class);
                                            intent.putExtra("EVENT_CODE",code);
                                            intent.putExtra("EVENT_MSG", msg);
                                            intent.putExtra("Available_Time",availableTime);
//                                            intent.putExtra("Team_Name",questionData.getTeamName());
                                            intent.putExtra("Team_Score",currentScore);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                });
                                Uri uri;
                                if(questionData.getEarlyBird())
                                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.early_bird_winner);
                                else
                                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.well_done);
                                videoview.setVideoURI(uri);
                                videoview.start();
                                findViewById(R.id.btnSubmit).setEnabled(false);
                                findViewById(R.id.btnUnlockHint).setEnabled(false);
                                findViewById(R.id.txtAnswer).setEnabled(false);
//                              Snackbar.make(view, "Successful !!", Snackbar.LENGTH_SHORT).show();
                            }else if(!questionData.getIsSuccess()){
                                if (wrongAnsSound.isPlaying()) wrongAnsSound.pause();
                                wrongAnsSound.seekTo(0);
                                wrongAnsSound.start();
//                                    Toast.makeText(MainActivity.this,"Wrong Answer, Try again.",Toast.LENGTH_SHORT).show();
                                Snackbar.make(view, "Wrong Answer, Try again.", Snackbar.LENGTH_SHORT).show();
                                if (code != null){
                                    Intent intent = new Intent(MainActivity.this, Activity_EventState.class);
                                    intent.putExtra("EVENT_CODE",code);
                                    intent.putExtra("EVENT_MSG", msg);
                                    intent.putExtra("Available_Time",availableTime);
//                                    intent.putExtra("Team_Name",questionData.getTeamName());
                                    intent.putExtra("Team_Score",currentScore);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                            }
                            if(code == null)
                                updateUI();
                        }
                        else{
                            error = "DeviceId=" + deviceId + ", checkResult(), " + "Data not received.";
                        }
                        if(error!=null){
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                        }
                    }
                });
    }
    private Task<QuestionData> checkAnswer(String ans) {
        Map<String,Object> data = new HashMap<>();
        data.put("level", questionData.getLevel());
        data.put("ans",ans);
        if(questionData.getHint()!=null)
            data.put("hintTaken", true);
        else
            data.put("hintTaken", false);
        data.put("teamCode", teamCode);
        data.put("deviceId",deviceId);
        findViewById(R.id.btnSubmit).setEnabled(false);
        return mFunctions.getHttpsCallable("checkAnswer")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, QuestionData>() {
                    @Override
                    public QuestionData then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        QuestionData qd = new QuestionData();
                        if(result == null || result.size()==0){
                          qd.setError("No Data received from Server.");
                        }

                        if (result.containsKey("code")){
                            code = result.get("code").toString();
                            msg = result.get("msg").toString();
                            qd.setCode((String)result.get("code"));
                            qd.setTeamName((String)result.get("teamName"));
                            if (result.containsKey("isSuccess") && result.get("isSuccess") != null){
                                Boolean temp = (Boolean)result.get("isSuccess");
                                qd.setIsSuccess(temp);
                            }
                            else qd.setIsSuccess(false);
                            if (result.containsKey("earlyBird") && result.get("earlyBird") != null){
                                Boolean temp = (Boolean)result.get("earlyBird");
                                qd.setEarlyBird(temp);
                            }
                            else qd.setEarlyBird(false);
                            if(result.containsKey("time") && result.get("time") != null)
                                availableTime=result.get("time").toString();
                            if(result.containsKey("teamScore"))
                                currentScore = result.get("teamScore").toString();
                            return qd;
                        } else if (result.containsKey("error")) {
                            qd.setError((String)result.get("error"));
                        } else {
                            qd.setTime((Integer) result.get("time"));
                            qd.setLevel((Integer) result.get("level"));
                            qd.setRank((String) result.get("rank"));
                            qd.setMaxRank((Integer) result.get("maxRank"));
                            qd.setQuestion((String) result.get("question"));
                            qd.setHint((String) result.get("hint"));
                            qd.setTeamName((String) result.get("teamName"));
                            qd.setAnsLength((Integer) result.get("ansLength"));
                            qd.setIsSuccess((Boolean) result.get("isSuccess"));
                            qd.setEarlyBird((Boolean) result.get("earlyBird"));
                            qd.sethintVisibility((Boolean) result.get("hintVisibility"));
                            availableHints=result.get("availableHints").toString();
                            totalHints=result.get("totalHints").toString();
                            hintsUsed=String.valueOf(Integer.valueOf(totalHints)-Integer.valueOf(availableHints));
                            hintUI=findViewById(R.id.hintUI);
                            if (qd.gethintVisibility()){
                                hintUI.setVisibility(View.VISIBLE);
                            } else {
                                hintUI.setVisibility(View.GONE);
                            }
                        }
                        return qd;
                    }
                });
    }
    private void updateUI(){
        findViewById(R.id.txtAnswer).setEnabled(true);
        findViewById(R.id.btnSubmit).setEnabled(true);
        findViewById(R.id.btnUnlockHint).setEnabled(true);
        boolean hintVisibility = questionData.gethintVisibility();
        hintUI=findViewById(R.id.hintUI);
        if (hintVisibility){
            hintUI.setVisibility(View.VISIBLE);
        } else {
            hintUI.setVisibility(View.GONE);
        }
        ((TextView)findViewById(R.id.hintview)).setText("Hints used: "+hintsUsed+"/"+totalHints);

        ((TextView) findViewById(R.id.lblTimer)).setText(questionData.getTime().toString());
        createAndShowTimer(questionData.getTime(),1000);
        ((TextInputLayout) findViewById(R.id.question)).setHint("Level " + questionData.getLevel().toString() + ":");
        ((TextInputEditText) findViewById(R.id.lblQuestion)).setText(questionData.getQuestion());
        ((TextView) findViewById(R.id.teamname)).setText(questionData.getTeamName());
        ((TextView) findViewById(R.id.lblLevel)).setText("Score: "+questionData.getRank());
        ((TextInputLayout) findViewById(R.id.lytAnswer)).setCounterMaxLength(questionData.getAnsLength());
        if(oldQuestionLevel != questionData.getLevel())
            ((TextInputEditText) findViewById(R.id.txtAnswer)).setText("");
        oldQuestionLevel = questionData.getLevel();
        btnUnlockHint = findViewById(R.id.btnUnlockHint);
        btnText = findViewById(R.id.btntext);
        EditText editText = (TextInputEditText) findViewById(R.id.txtAnswer);
        editText.setFilters(new InputFilter[] {
                new InputFilter.AllCaps() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        return String.valueOf(source).toLowerCase();
                    }
                },
                new InputFilter.LengthFilter(questionData.getAnsLength())
        });
        hint = questionData.getHint();
        hintText.setText(hint);
        if(hint != null) {
            btnUnlockHint.setVisibility(View.GONE);
            hintBox.setVisibility(View.VISIBLE);
            btnText.setVisibility(View.GONE);
        }else{
            btnUnlockHint.setVisibility(View.VISIBLE);
            hintBox.setVisibility(View.GONE);
            btnText.setVisibility(View.VISIBLE);
        }

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
                ((TextView)findViewById(R.id.lblTimer)).setText("Level Time Up");
                callToGetQuestion();
            }
        }.start();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ScrollView scrollView = findViewById(R.id.scrollView);
        FirebaseHelper.setEdgeToEdgeInsets(scrollView);

        mFunctions = FirebaseFunctions.getInstance();
        if(FirebaseHelper.EMULATOR_RUNNING)
            mFunctions.useEmulator("10.0.2.2",5001);

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        hintBox=findViewById(R.id.hint);
        hintUI=findViewById(R.id.hintUI);
        hintText=findViewById(R.id.lblHintText);

//        buttonSound = MediaPlayer.create(MainActivity.this, R.raw.button_sound);
        wrongAnsSound = MediaPlayer.create(MainActivity.this, R.raw.wrongans_sound);

        ImageButton btnSubmit = findViewById(R.id.btnSubmit);

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                scrollView.getWindowVisibleDisplayFrame(r);
                int screenHeight = scrollView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // Assume that the keyboard is at least 15% of the screen height
                    int[] location = new int[2];
                    btnSubmit.getLocationOnScreen(location);
                    int y = location[1];
                    scrollView.smoothScrollTo(0, y + btnSubmit.getHeight());
                }
            }
        });

        ((TextInputEditText)findViewById(R.id.txtAnswer)).setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                                || (actionId == EditorInfo.IME_ACTION_GO)
                                || (actionId == EditorInfo.IME_ACTION_DONE)
                                || (actionId == EditorInfo.IME_ACTION_SEND)) {
                            findViewById(R.id.btnSubmit).performClick();
                        }
                        return false;
                    }
                }
        );

    }
    public void btnUnlockHint(View view) {


//        if (buttonSound.isPlaying()) buttonSound.pause();
//        buttonSound.seekTo(0);
//        buttonSound.start();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        if (!isNetworkAvailable(MainActivity.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        if(hintsUsed.equals(totalHints))
        {
            Toast.makeText(MainActivity.this,"All Hints "+hintsUsed+"/"+totalHints+" are Already Exhausted.",Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.btnUnlockHint).setEnabled(false);


            builder=new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("Help")
                    .setMessage("Are you sure? 25% Score will be deducted. Current Status " + hintsUsed+"/"+totalHints)
                    .setCancelable(false)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                            sendDialogDataToActivity("No");
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                            sendDialogDataToActivity("Yes");
                        }
                    })
                    .show();
    }
    private void sendDialogDataToActivity(String data) {
        findViewById(R.id.btnUnlockHint).setEnabled(true);
        if(data.equals("No"))
            return;

        if (!isNetworkAvailable(MainActivity.this)) {
            showRefreshDialog(getResources().getString(R.string.internet_notavailable));
            return;
        }
        if(mFunctions == null)
            mFunctions = FirebaseFunctions.getInstance();

        findViewById(R.id.btnSubmit).setEnabled(false);
        findViewById(R.id.btnUnlockHint).setEnabled(false);
        findViewById(R.id.spinner).setVisibility(View.VISIBLE);

        code = null;
        unlockHint()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        findViewById(R.id.btnSubmit).setEnabled(true);
                        findViewById(R.id.spinner).setVisibility(View.GONE);

                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            String error = null;
                            if (e instanceof FirebaseFunctionsException)
                                error = "FirebaseFunctionException Code = " + ((FirebaseFunctionsException) e).getCode() + ", " + e.getMessage();
                            else
                                error = "FirebaseFunctionException Code = " + e.getMessage();
                            error = "DeviceId=" + deviceId + ", unlockHint(), " + error;
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                            return;
                        }

                        String hintMsg = task.getResult();

                        if(hintMsg.equals("<<Error>>") || hintMsg==null){
                            String error = "DeviceId=" + deviceId + ", unlockHint(), " + "Error getting hint while unlocking.";
                            Log.w(TAG, error);
                            showRestartDialog(getResources().getString(R.string.error),error);
                        }
                        if (code != null){
                            Intent intent = new Intent(MainActivity.this, Activity_EventState.class);
                            intent.putExtra("EVENT_CODE",code);
                            intent.putExtra("EVENT_MSG", msg);
                            intent.putExtra("Available_Time",availableTime);
//                            intent.putExtra("Team_Name",questionData.getTeamName());
                            intent.putExtra("Team_Score",currentScore);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        }else {
                            hint = hintMsg;
                            btnUnlockHint.setVisibility(View.GONE);
                            hintBox.setVisibility(View.VISIBLE);
                            hintText.setText(hint);
                            btnText.setVisibility(View.GONE);
                            if(updateScoreAfterTakingHint!=null)
                                ((TextView) findViewById(R.id.lblLevel)).setText("Score: "+updateScoreAfterTakingHint);
                        }
                    }
                });
    }
    private Task<String> unlockHint() {

        Map<String,Object> data = new HashMap<>();
        data.put("level", questionData.getLevel());
        data.put("teamCode", teamCode);
        data.put("deviceId", deviceId);

        return mFunctions.getHttpsCallable("unlockHint")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        if (result.containsKey("code")){
                            code = result.get("code").toString();
                            msg = result.get("msg").toString();
                            if(result.containsKey("time"))
                                availableTime=result.get("time").toString();
                            if(result.containsKey("teamScore"))
                                currentScore = result.get("teamScore").toString();
                            return "<<event>>";
                        }

                        if(result.containsKey("availableHints"))
                        {
                            availableHints=result.get("availableHints").toString();
                        }
                        if(result.containsKey("totalHints"))
                        {
                            totalHints=result.get("totalHints").toString();
                        }
                        if(result.containsKey("teamScore"))
                        {
                            updateScoreAfterTakingHint = result.get("teamScore").toString();
                        }
                        hintsUsed=String.valueOf(Integer.valueOf(totalHints)-Integer.valueOf(availableHints));
                        ((TextView)findViewById(R.id.hintview)).setText("Hints used: "+hintsUsed+"/"+totalHints);
                        if(result.containsKey("hint"))
                            return (String)(result.get("hint"));
                        else
                            return "<<Error>>";
                    }
                });
    }
    public void lblTeamNameClickShowDeviceId(View view) {
        Snackbar.make(view, "DeviceId: "+deviceId, Snackbar.LENGTH_SHORT).show();
    }
//    private void showErrorDialog(String message) {
//        findViewById(R.id.btnSubmit).setEnabled(true);
//        findViewById(R.id.btnUnlockHint).setEnabled(true);
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
        findViewById(R.id.btnSubmit).setEnabled(true);
        findViewById(R.id.btnUnlockHint).setEnabled(true);

        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_layout,viewGroup,false);
//        builder.setCancelable(false);
        builder.setView(view);

        AlertDialog alertDialog = builder.create();
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
        findViewById(R.id.btnSubmit).setEnabled(true);
        findViewById(R.id.btnUnlockHint).setEnabled(true);

        ViewGroup viewGroup = findViewById(android.R.id.content);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_layout,viewGroup,false);
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
                startActivity(new Intent(MainActivity.this, Activity_welcome_screen.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            }
        });
        view.findViewById(R.id.txtDialogCancel).setVisibility(View.GONE);
        alertDialog.show();
    }
}