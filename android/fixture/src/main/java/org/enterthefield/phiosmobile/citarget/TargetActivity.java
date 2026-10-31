package org.enterthefield.phiosmobile.citarget;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Harmless launcher-visible fixture installed only in ephemeral emulator CI. */
public final class TargetActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(9, 16, 28));
        TextView title = new TextView(this);
        title.setText("PHIOS CI TARGET LIVE");
        title.setContentDescription("PHIOS CI TARGET LIVE");
        title.setTextColor(Color.rgb(121, 232, 235));
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Launched as a real Android app by PhiOS Mobile.");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(14);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);
        setContentView(root);
    }
}
