package com.event.cryptothon3;

import android.os.Build;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FirebaseHelper {
    public static final boolean EMULATOR_RUNNING = false;

    public static void setEdgeToEdgeInsets(View activityView){
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            return;
        }

//        View view = findViewById(activityUIResource);
        ViewCompat.setOnApplyWindowInsetsListener(activityView,(v, windowsInsets)-> {
            Insets insets = windowsInsets.getInsets((WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout()));
            v.setPadding(insets.left,insets.top,insets.right,insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
