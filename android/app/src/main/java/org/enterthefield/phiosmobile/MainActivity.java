package org.enterthefield.phiosmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Native, user-selectable Android HOME activity.
 *
 * Not an Android ROM, a device administrator, notification listener or
 * replacement for system call/emergency capabilities.
 */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(9, 16, 28);
    private static final int PANEL = Color.rgb(20, 34, 53);
    private static final int CYAN = Color.rgb(121, 232, 235);
    private static final int TEXT = Color.rgb(233, 244, 255);
    private static final int MUTED = Color.rgb(153, 175, 195);
    private static final String PREFS = "phios-native-launcher-v1";
    private static final String PINNED = "pinned";
    private LauncherApps launcherApps;
    private SharedPreferences preferences;
    private Set<String> pinned = new HashSet<>();
    private List<LauncherActivityInfo> installed = Collections.emptyList();
    private LinearLayout appList;
    private TextView appCount;
    private TextView emptyView;
    private EditText search;
    private boolean favoritesOnly;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        pinned = new HashSet<>(preferences.getStringSet(PINNED, Collections.emptySet()));
        launcherApps = getSystemService(LauncherApps.class);
        buildHome();
    }

    @Override protected void onResume() {
        super.onResume();
        reloadInstalledApps();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable surface(int color, int cornerRadiusDp, int borderColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(1), borderColor);
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextColor(color);
        result.setTextSize(size);
        result.setFontFeatureSettings("kern");
        if (bold) result.setTypeface(null, android.graphics.Typeface.BOLD);
        return result;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackground(surface(PANEL, 14, Color.rgb(50, 79, 100)));
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setMinHeight(dp(48));
        b.setOnClickListener(view -> action.run());
        return b;
    }

    private void buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setFitsSystemWindows(true);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(dp(16), dp(12) + bars.top, dp(16), dp(12) + bars.bottom);
                return insets;
            });
            root.setFitsSystemWindows(false);
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView phi = text("Φ", 44, CYAN, false);
        header.addView(phi);
        LinearLayout identity = new LinearLayout(this);
        identity.setPadding(dp(10), 0, 0, 0);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.addView(text("PHIOS MOBILE", 19, TEXT, true));
        identity.addView(text("NATIVE LAUNCHER · RUNG 2", 10, MUTED, true));
        header.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(button("✳ SURF", this::openSurf));
        root.addView(header);

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT);
        clock.setTextSize(44);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-1, -2);
        clockParams.topMargin = dp(22);
        root.addView(clock, clockParams);

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, MMMM d");
        date.setFormat24Hour("EEEE, MMMM d");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        root.addView(date);

        TextView headline = text("A different kind of mobile.", 19, TEXT, true);
        headline.setPadding(dp(17), dp(18), dp(17), dp(4));
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackground(surface(PANEL, 18, Color.rgb(49, 95, 110)));
        hero.addView(headline);
        TextView caption = text("Your apps, your launcher. No cloud or firmware changes required.", 12, MUTED, false);
        caption.setPadding(dp(17), 0, dp(17), dp(18));
        hero.addView(caption);
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.topMargin = dp(17);
        root.addView(hero, heroParams);

        LinearLayout actions = new LinearLayout(this);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(49));
        actionParams.topMargin = dp(16);
        root.addView(actions, actionParams);
        Button all = button("All apps", () -> {
            favoritesOnly = false;
            renderApps();
        });
        Button favorites = button("Pinned", () -> {
            favoritesOnly = true;
            renderApps();
        });
        Button sysSettings = button("System ⚙", this::openSettings);
        for (Button action : new Button[]{all, favorites, sysSettings}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
            p.setMargins(dp(2), 0, dp(2), 0);
            actions.addView(action, p);
        }

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setHint("Search launchable apps");
        search.setTextSize(14);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(surface(Color.rgb(12, 24, 39), 12, Color.rgb(46, 73, 93)));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.topMargin = dp(12);
        root.addView(search, searchParams);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderApps(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout listHeading = new LinearLayout(this);
        listHeading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams listHeadingParams = new LinearLayout.LayoutParams(-1, dp(44));
        root.addView(listHeading, listHeadingParams);
        listHeading.addView(text("YOUR APPS", 12, CYAN, true), new LinearLayout.LayoutParams(0, -2, 1));
        appCount = text("DISCOVERING", 10, MUTED, true);
        listHeading.addView(appCount);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(appList);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        emptyView = text("Finding launcher activities…", 13, MUTED, false);
        emptyView.setPadding(dp(12), dp(16), dp(12), dp(16));
        appList.addView(emptyView);

        TextView footnote = text("Long-press an app to pin it. To switch back: Settings → Apps → Default apps → Home app.", 10, MUTED, false);
        footnote.setPadding(0, dp(10), 0, 0);
        root.addView(footnote);
        setContentView(root);
    }

    private void reloadInstalledApps() {
        if (launcherApps == null) {
            installed = Collections.emptyList();
            appCount.setText("UNAVAILABLE");
            showEmpty("Android LauncherApps service unavailable.");
            return;
        }
        try {
            installed = new ArrayList<>(launcherApps.getActivityList(null, Process.myUserHandle()));
            final Collator collator = Collator.getInstance();
            installed.sort((a, b) -> collator.compare(String.valueOf(a.getLabel()), String.valueOf(b.getLabel())));
            renderApps();
        } catch (RuntimeException error) {
            installed = Collections.emptyList();
            appCount.setText("UNAVAILABLE");
            showEmpty("Could not discover applications on this device.");
        }
    }

    private void showEmpty(String message) {
        appList.removeAllViews();
        emptyView.setText(message);
        appList.addView(emptyView);
    }

    private void renderApps() {
        if (appList == null) return;
        List<LauncherActivityInfo> visible = new ArrayList<>();
        String query = search == null ? "" : search.getText().toString();
        for (LauncherActivityInfo app : installed) {
            String key = app.getComponentName().flattenToString();
            if (favoritesOnly && !pinned.contains(key)) continue;
            if (LauncherLogic.matchesQuery(String.valueOf(app.getLabel()), query)) visible.add(app);
        }
        visible.sort((a, b) -> {
            boolean pa = pinned.contains(a.getComponentName().flattenToString());
            boolean pb = pinned.contains(b.getComponentName().flattenToString());
            if (pa != pb) return pa ? -1 : 1;
            return Collator.getInstance().compare(String.valueOf(a.getLabel()), String.valueOf(b.getLabel()));
        });
        appList.removeAllViews();
        appCount.setText(visible.size() + (favoritesOnly ? " PINNED" : " APPS"));
        if (visible.isEmpty()) {
            showEmpty(favoritesOnly ? "No pinned apps match. Long-press an app in All apps to pin it."
                    : "No launchable apps match this search.");
            return;
        }
        for (LauncherActivityInfo info : visible) {
            addAppRow(info);
        }
    }

    private void addAppRow(LauncherActivityInfo info) {
        String componentKey = info.getComponentName().flattenToString();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackground(surface(PANEL, 13, Color.rgb(39, 63, 82)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(62));
        rowParams.bottomMargin = dp(8);

        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.getIcon(0)); }
        catch (RuntimeException ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
        row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView name = text(String.valueOf(info.getLabel()), 14, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(name);
        titles.addView(text(pinned.contains(componentKey) ? "★ Pinned" : "Tap to open · Hold to pin", 10, pinned.contains(componentKey) ? CYAN : MUTED, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        row.setContentDescription(String.valueOf(info.getLabel()) + (pinned.contains(componentKey) ? ", pinned" : ""));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> {
            try {
                launcherApps.startMainActivity(info.getComponentName(), Process.myUserHandle(), null, null);
            } catch (RuntimeException error) {
                Toast.makeText(this, "Android could not launch this app.", Toast.LENGTH_SHORT).show();
            }
        });
        row.setOnLongClickListener(view -> {
            if (pinned.contains(componentKey)) pinned.remove(componentKey);
            else pinned.add(componentKey);
            preferences.edit().putStringSet(PINNED, new HashSet<>(pinned)).apply();
            renderApps();
            Toast.makeText(this, pinned.contains(componentKey) ? "Pinned" : "Unpinned", Toast.LENGTH_SHORT).show();
            return true;
        });
        appList.addView(row, rowParams);
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Settings app unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseDefaultHome() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Open Settings → Default apps → Home app.", Toast.LENGTH_LONG).show();
        }
    }

    private void openStatus() {
        new AlertDialog.Builder(this)
            .setTitle("PhiOS Mobile · Native Preview")
            .setMessage("This is an Android HOME launcher, not a phone OS or custom ROM.\n\n"
                    + "App discovery uses Android LauncherApps for the current user only. "
                    + "Pinned app identifiers stay in local app preferences; cloud backup is disabled. "
                    + "No Internet, notification-listener, administrator or QUERY_ALL_PACKAGES permission is requested.\n\n"
                    + "Calls, cellular, lock screen, verified boot, and emergency services remain controlled by Android.")
            .setPositiveButton("Choose default Home", (dialog, which) -> chooseDefaultHome())
            .setNeutralButton("Clear pinned list", (dialog, which) -> new AlertDialog.Builder(this)
                    .setMessage("Remove all locally saved pinned app shortcuts?")
                    .setPositiveButton("Remove", (confirmDialog, confirmation) -> {
                        pinned.clear();
                        preferences.edit().remove(PINNED).apply();
                        renderApps();
                    }).setNegativeButton("Cancel", null).show())
            .setNegativeButton("Close", null)
            .show();
    }

    private void openSurf() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(9), dp(12), dp(9), dp(12));

        final AlertDialog[] holder = new AlertDialog[1];
        String[] options = {"All apps", "Pinned apps", "Search", "System settings", "Status / Home", "Close"};
        Runnable[] actions = {
            () -> { favoritesOnly = false; search.setText(""); renderApps(); },
            () -> { favoritesOnly = true; search.setText(""); renderApps(); },
            () -> { search.requestFocus(); ((android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); },
            this::openSettings,
            this::openStatus,
            () -> {}
        };
        PhiSurfDial dial = new PhiSurfDial(this, index -> {
            holder[0].dismiss();
            actions[index].run();
        });
        container.addView(dial, new LinearLayout.LayoutParams(-1, dp(292)));

        TextView hint = text("Drag from the center and release, or use a button below.", 11, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(6), dp(2), dp(6), dp(10));
        container.addView(hint);
        for (int index = 0; index < 3; index++) {
            LinearLayout line = new LinearLayout(this);
            for (int offset = 0; offset < 2; offset++) {
                int target = index * 2 + offset;
                Button b = button(options[target], () -> {
                    holder[0].dismiss();
                    actions[target].run();
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(47), 1);
                p.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(b, p);
            }
            container.addView(line);
        }
        holder[0] = new AlertDialog.Builder(this)
                .setTitle("ΦSURF")
                .setView(container)
                .setNegativeButton("Dismiss", (dialog, which) -> {})
                .create();
        holder[0].show();
    }

    @Override public void onBackPressed() {
        if (favoritesOnly || (search != null && search.length() > 0)) {
            favoritesOnly = false;
            search.setText("");
            renderApps();
        } else {
            // Remain in the home activity instead of exiting into an arbitrary task.
            super.onBackPressed();
        }
    }
}
