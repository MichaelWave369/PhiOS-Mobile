package org.enterthefield.phiosmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.HorizontalScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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
    private static final int EXPORT_PROFILE_REQUEST = 4101;
    private static final int IMPORT_PROFILE_REQUEST = 4102;
    private static final int EXPORT_SOURCE_SHELF_REQUEST = 4103;
    private static final int IMPORT_SOURCE_SHELF_REQUEST = 4104;
    private static final String SOURCE_SHELF_KEY = "portable-source-shelf-r3";
    private LauncherApps launcherApps;
    private SharedPreferences preferences;
    private Set<String> pinned = new HashSet<>();
    private List<LauncherActivityInfo> installed = Collections.emptyList();
    private LinearLayout appList;
    private TextView appCount;
    private TextView emptyView;
    private EditText search;
    private boolean favoritesOnly;
    private View homeClock;
    private View homeDate;
    private View homeHero;
    private LinearLayout pinnedShelf;
    private LinearLayout pinnedShelfItems;
    private boolean compactRows;
    private static final int MAX_SHELF_APPS = 8;

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
        b.setMinWidth(dp(48));
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

        boolean tight = LauncherLayoutPolicy.stackActions(
                getResources().getConfiguration().screenWidthDp,
                getResources().getConfiguration().fontScale);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView phi = text("Φ", 44, CYAN, false);
        header.addView(phi);
        LinearLayout identity = new LinearLayout(this);
        identity.setPadding(dp(10), 0, 0, 0);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.addView(text("PHIOS MOBILE", 19, TEXT, true));
        identity.addView(text("NATIVE LAUNCHER · RUNG 2.5", 10, MUTED, true));
        header.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        Button surfAction = button("✳ SURF", this::openSurf);
        if (!tight) header.addView(surfAction);
        root.addView(header);
        if (tight) {
            // At narrow widths, keep the main navigation button below the title.
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
            p.topMargin = dp(6);
            root.addView(surfAction, p);
        }

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(TEXT);
        clock.setTextSize(44);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-1, -2);
        clockParams.topMargin = dp(22);
        root.addView(clock, clockParams);
        homeClock = clock;

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, MMMM d");
        date.setFormat24Hour("EEEE, MMMM d");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        root.addView(date);
        homeDate = date;

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
        homeHero = hero;

        // A bounded horizontal shelf keeps favorite apps one tap away.
        // It's populated exclusively from real installed launcher activities.
        pinnedShelf = new LinearLayout(this);
        pinnedShelf.setOrientation(LinearLayout.VERTICAL);
        LinearLayout shelfHeading = new LinearLayout(this);
        shelfHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView shelfTitle = text("PINNED SHELF", 11, CYAN, true);
        shelfHeading.addView(shelfTitle, new LinearLayout.LayoutParams(0, -2, 1));
        shelfHeading.addView(button("All pinned", () -> {
            favoritesOnly = true;
            if (search != null) search.setText("");
            renderApps();
        }), new LinearLayout.LayoutParams(dp(102), dp(43)));
        pinnedShelf.addView(shelfHeading);
        HorizontalScrollView shelfScroll = new HorizontalScrollView(this);
        shelfScroll.setHorizontalScrollBarEnabled(false);
        shelfScroll.setFillViewport(true);
        pinnedShelfItems = new LinearLayout(this);
        pinnedShelfItems.setOrientation(LinearLayout.HORIZONTAL);
        pinnedShelfItems.setGravity(Gravity.CENTER_VERTICAL);
        shelfScroll.addView(pinnedShelfItems);
        pinnedShelf.addView(shelfScroll, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams shelfParams = new LinearLayout.LayoutParams(-1, -2);
        shelfParams.topMargin = dp(11);
        root.addView(pinnedShelf, shelfParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(12);
        root.addView(actions, actionParams);
        Button all = button("All apps", () -> {
            favoritesOnly = false;
            renderApps();
        });
        Button favorites = button("Pinned", () -> {
            favoritesOnly = true;
            renderApps();
        });
        Button layout = button("Layout", this::openLayout);
        Button sysSettings = button("System ⚙", this::openSettings);
        Button gpu = button("GPU info", () -> new AlertDialog.Builder(this)
                .setTitle("Φ GPU Observatory")
                .setMessage(GpuCapabilitySummary.describe(this))
                .setPositiveButton("Close", null).show());
        Button sourceShelf = button("Source Shelf", this::openSourceShelf);
        sourceShelf.setContentDescription("Open portable public-source bookmarks, not installed Android apps");
        Button[] controls = {all, favorites, layout, sysSettings, gpu, sourceShelf};
        int columns = tight ? 2 : 4;
        for (int offset = 0; offset < controls.length; offset += columns) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            for (int i = offset; i < Math.min(offset + columns, controls.length); i++) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
                p.setMargins(dp(2), dp(2), dp(2), dp(2));
                line.addView(controls[i], p);
            }
            actions.addView(line, new LinearLayout.LayoutParams(-1, -2));
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
        if (tight) {
            // A whole-page scroll avoids losing lower actions and app rows when
            // system text is enlarged or the display is narrow/short.
            root.addView(appList, new LinearLayout.LayoutParams(-1, -2));
        } else {
            scroller.addView(appList);
            root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        }

        emptyView = text("Finding launcher activities…", 13, MUTED, false);
        emptyView.setPadding(dp(12), dp(16), dp(12), dp(16));
        appList.addView(emptyView);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        Button homeHelp = button("Home help", this::openHomeRecoveryGuide);
        homeHelp.setContentDescription("Home help: restore your original Android launcher");
        Button backup = button("Backup", this::openBackupMenu);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(48), 1);
        left.rightMargin = dp(8);
        footer.addView(homeHelp, left);
        footer.addView(backup, new LinearLayout.LayoutParams(0, dp(48), 1));
        if (tight) {
            // Recovery must be reachable before a long installed-app list.
            root.addView(footer, root.indexOfChild(appList));
            ScrollView fullPage = new ScrollView(this);
            fullPage.setFillViewport(true);
            fullPage.addView(root, new ScrollView.LayoutParams(-1, -2));
            setContentView(fullPage);
        } else {
            root.addView(footer);
            setContentView(root);
        }
        applyHomeLayout();
    }

    private void reloadInstalledApps() {
        if (launcherApps == null) {
            installed = Collections.emptyList();
            renderPinnedShelf();
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
            renderPinnedShelf();
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
        renderPinnedShelf();
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
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1,
                dp(LauncherLayoutPolicy.appRowHeightDp(compactRows,
                        getResources().getConfiguration().fontScale)));
        rowParams.bottomMargin = dp(8);

        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.getIcon(0)); }
        catch (RuntimeException ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
        row.addView(icon, new LinearLayout.LayoutParams(dp(compactRows ? 28 : 36), dp(compactRows ? 28 : 36)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView name = text(String.valueOf(info.getLabel()), 14, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(name);
        titles.addView(text(pinned.contains(componentKey) ? "★ Pinned" : "Tap to open · Hold to pin", 10, pinned.contains(componentKey) ? CYAN : MUTED, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        // Expose a real focusable alternative to long-press pinning for TalkBack.
        boolean isPinned = pinned.contains(componentKey);
        Button pinAction = button(isPinned ? "Unpin" : "Pin", () -> togglePin(info));
        pinAction.setContentDescription((isPinned ? "Unpin " : "Pin ") + info.getLabel());
        row.addView(pinAction, new LinearLayout.LayoutParams(dp(67), dp(48)));

        row.setContentDescription("Open " + info.getLabel()
                + (isPinned ? ", pinned" : ", not pinned"));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> launchApp(info));
        row.setOnLongClickListener(view -> {
            togglePin(info);
            return true;
        });
        appList.addView(row, rowParams);
    }

    private void togglePin(LauncherActivityInfo info) {
        String key = info.getComponentName().flattenToString();
        boolean nowPinned = !pinned.contains(key);
        if (nowPinned) pinned.add(key);
        else pinned.remove(key);
        preferences.edit().putStringSet(PINNED, new HashSet<>(pinned)).apply();
        renderApps();
        Toast.makeText(this, (nowPinned ? "Pinned " : "Unpinned ") + info.getLabel(),
                Toast.LENGTH_SHORT).show();
    }

    private void launchApp(LauncherActivityInfo info) {
        try {
            // Prefer the platform's dedicated current-user launcher API.
            launcherApps.startMainActivity(info.getComponentName(), Process.myUserHandle(), null, null);
            return;
        } catch (RuntimeException launcherFailure) {
            // Some Android builds gate LauncherApps launches behind the HOME role.
            // A normal explicit ACTION_MAIN/LAUNCHER intent is permitted for the
            // current user's exported, discoverable app activity.
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(info.getComponentName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(this, "Android could not launch this app.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderPinnedShelf() {
        if (pinnedShelfItems == null) return;
        pinnedShelfItems.removeAllViews();
        List<String> availableKeys = new ArrayList<>();
        for (LauncherActivityInfo info : installed) {
            availableKeys.add(info.getComponentName().flattenToString());
        }
        List<String> shelfKeys = LauncherLogic.shelfKeys(availableKeys, pinned, MAX_SHELF_APPS);
        if (shelfKeys.isEmpty()) {
            TextView empty = text("Long-press an app below to pin it here.", 11, MUTED, false);
            empty.setPadding(dp(2), dp(8), dp(2), dp(8));
            pinnedShelfItems.addView(empty);
            return;
        }
        for (String key : shelfKeys) {
            LauncherActivityInfo match = null;
            for (LauncherActivityInfo info : installed) {
                if (info.getComponentName().flattenToString().equals(key)) {
                    match = info;
                    break;
                }
            }
            if (match == null) continue;
            final LauncherActivityInfo pinnedInfo = match;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(4), dp(9), dp(4), dp(7));
            tile.setBackground(surface(PANEL, 13, Color.rgb(48, 94, 112)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(82), dp(91));
            params.setMargins(0, dp(4), dp(9), dp(5));

            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(match.getIcon(0)); }
            catch (RuntimeException ignored) {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            tile.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
            TextView label = text(String.valueOf(match.getLabel()), 10, TEXT, true);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            tile.addView(label, new LinearLayout.LayoutParams(-1, dp(27)));
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setContentDescription("Open " + match.getLabel()
                    + " from pinned shelf. Long press to unpin.");
            tile.setOnClickListener(view -> launchApp(pinnedInfo));
            tile.setOnLongClickListener(view -> {
                pinned.remove(key);
                preferences.edit().putStringSet(PINNED, new HashSet<>(pinned)).apply();
                renderApps();
                Toast.makeText(this, "Unpinned " + pinnedInfo.getLabel(), Toast.LENGTH_SHORT).show();
                return true;
            });
            pinnedShelfItems.addView(tile, params);
        }
        if (pinned.size() > shelfKeys.size()) {
            Button more = button("All pinned", () -> {
                favoritesOnly = true;
                if (search != null) search.setText("");
                renderApps();
            });
            pinnedShelfItems.addView(more, new LinearLayout.LayoutParams(dp(95), dp(58)));
        }
    }

    private void applyHomeLayout() {
        if (preferences == null) return;
        if (homeClock != null) homeClock.setVisibility(
                preferences.getBoolean("show_clock", true) ? View.VISIBLE : View.GONE);
        if (homeDate != null) homeDate.setVisibility(
                preferences.getBoolean("show_clock", true) ? View.VISIBLE : View.GONE);
        if (homeHero != null) homeHero.setVisibility(
                preferences.getBoolean("show_hero", true) ? View.VISIBLE : View.GONE);
        if (pinnedShelf != null) pinnedShelf.setVisibility(
                preferences.getBoolean("show_shelf", true) ? View.VISIBLE : View.GONE);
        compactRows = preferences.getBoolean("compact_rows", false);
    }

    private void openLayout() {
        final String[] choices = {
                "Show large clock and date", "Show welcome card",
                "Compact app rows", "Show pinned shelf"
        };
        final boolean[] selections = {
                preferences.getBoolean("show_clock", true),
                preferences.getBoolean("show_hero", true),
                preferences.getBoolean("compact_rows", false),
                preferences.getBoolean("show_shelf", true)
        };
        new AlertDialog.Builder(this)
                .setTitle("Home layout")
                .setMultiChoiceItems(choices, selections,
                        (dialog, index, checked) -> selections[index] = checked)
                .setPositiveButton("Save", (dialog, which) -> {
                    preferences.edit()
                            .putBoolean("show_clock", selections[0])
                            .putBoolean("show_hero", selections[1])
                            .putBoolean("compact_rows", selections[2])
                            .putBoolean("show_shelf", selections[3])
                            .apply();
                    applyHomeLayout();
                    renderApps();
                })
                .setNeutralButton("Reset", (dialog, which) -> {
                    preferences.edit()
                            .remove("show_clock")
                            .remove("show_hero")
                            .remove("compact_rows")
                            .remove("show_shelf")
                            .apply();
                    applyHomeLayout();
                    renderApps();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Map<String, Boolean> currentLayoutSettings() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (int i = 0; i < LauncherProfileCodec.LAYOUT_KEYS.length; i++) {
            result.put(LauncherProfileCodec.LAYOUT_KEYS[i],
                    preferences.getBoolean(LauncherProfileCodec.LAYOUT_KEYS[i],
                            LauncherProfileCodec.LAYOUT_DEFAULTS[i]));
        }
        return result;
    }

    private void openBackupMenu() {
        // A custom dialog shows both the privacy explanation and the actions.
        // AlertDialog.setMessage() with setItems() hides the list on some Android builds.
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), dp(12));
        TextView explanation = text(
                "Back up pins and layout to a file you choose. No account or Internet "
                + "permission is needed. Choose on-device storage if you do not "
                + "want a document provider that may sync to the cloud. "
                + "Imports show a preview before changing settings.", 13, MUTED, false);
        explanation.setPadding(0, dp(4), 0, dp(10));
        content.addView(explanation);
        final AlertDialog[] holder = new AlertDialog[1];
        Button export = button("Export to JSON", () -> {
            holder[0].dismiss();
            beginExportProfile();
        });
        Button restore = button("Import from JSON", () -> {
            holder[0].dismiss();
            beginImportProfile();
        });
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(-1, dp(52));
        first.bottomMargin = dp(8);
        content.addView(export, first);
        content.addView(restore, new LinearLayout.LayoutParams(-1, dp(52)));
        holder[0] = new AlertDialog.Builder(this)
                .setTitle("Local backup & restore")
                .setView(content)
                .setNegativeButton("Close", null)
                .create();
        holder[0].show();
    }

    private void beginExportProfile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "phios-launcher-profile-v1.json");
        try {
            startActivityForResult(intent, EXPORT_PROFILE_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No document provider is available to export settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void beginImportProfile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, IMPORT_PROFILE_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No document provider is available to import settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != EXPORT_PROFILE_REQUEST && requestCode != IMPORT_PROFILE_REQUEST
                && requestCode != EXPORT_SOURCE_SHELF_REQUEST
                && requestCode != IMPORT_SOURCE_SHELF_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri document = data.getData();
        // The system picker grants scoped access to a single selected document.
        // No storage or broad file-access permission is requested or retained.
        if (!"content".equals(document.getScheme())) {
            Toast.makeText(this, "Only document-provider files are supported.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (requestCode == EXPORT_PROFILE_REQUEST) exportProfile(document);
        else if (requestCode == IMPORT_PROFILE_REQUEST) importProfile(document);
        else if (requestCode == EXPORT_SOURCE_SHELF_REQUEST) exportSourceShelf(document);
        else importSourceShelf(document);
    }

    private void exportProfile(Uri document) {
        try {
            String json = LauncherProfileCodec.encode(new HashSet<>(pinned), currentLayoutSettings());
            try (OutputStream out = getContentResolver().openOutputStream(document, "wt")) {
                if (out == null) throw new java.io.IOException("Document provider refused a write stream.");
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            Toast.makeText(this, "Launcher settings exported to your chosen file.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            showBackupError("Export failed", error);
        }
    }

    private byte[] readBoundedDocument(Uri document) throws java.io.IOException {
        try (InputStream stream = getContentResolver().openInputStream(document)) {
            if (stream == null) throw new java.io.IOException("Document provider returned no file.");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > LauncherProfileCodec.MAX_BYTES)
                    throw new java.io.IOException("Profile exceeds the 64 KiB safety limit.");
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        }
    }

    private void importProfile(Uri document) {
        try {
            byte[] file = readBoundedDocument(document);
            Set<String> currentApps = new HashSet<>();
            for (LauncherActivityInfo info : installed)
                currentApps.add(info.getComponentName().flattenToString());
            LauncherProfileCodec.Preview preview = LauncherProfileCodec.decode(file, currentApps);
            String message = "Available pinned apps: " + preview.availablePins.size()
                    + "\nUnavailable apps skipped: " + preview.skippedUnavailable
                    + "\nLayout settings: 4"
                    + "\n\nRestore will REPLACE your existing pins and layout only. "
                    + "It will not install apps, change Android's default Home, or access app data.";
            new AlertDialog.Builder(this)
                    .setTitle("Preview launcher restore")
                    .setMessage(message)
                    .setPositiveButton("Restore", (dialog, which) -> applyImportedProfile(preview))
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception error) {
            showBackupError("Import rejected", error);
        }
    }

    private void applyImportedProfile(LauncherProfileCodec.Preview preview) {
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(PINNED, new HashSet<>(preview.availablePins));
        for (Map.Entry<String, Boolean> setting : preview.layout.entrySet())
            editor.putBoolean(setting.getKey(), setting.getValue());
        if (!editor.commit()) {
            Toast.makeText(this, "Couldn't save restored settings. Existing settings retained.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        pinned = new HashSet<>(preview.availablePins);
        applyHomeLayout();
        renderApps();
        Toast.makeText(this, "Launcher profile restored locally.", Toast.LENGTH_LONG).show();
    }

    /**
     * SOURCE shelf: a separate, explicit bookmark collection. This code NEVER
     * changes PINNED, Android LauncherApps state, app permissions or Home role.
     */
    private AppShelfCodec.Catalog sourceCatalog() throws Exception {
        try (InputStream stream = getAssets().open("public-repo-catalog.v1.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > AppShelfCodec.MAX_CATALOG_BYTES)
                    throw new java.io.IOException("Bundled public catalog exceeds limit.");
                bytes.write(buffer, 0, count);
            }
            return AppShelfCodec.readCatalog(bytes.toByteArray());
        }
    }

    private List<String> sourcePins(AppShelfCodec.Catalog catalog) throws Exception {
        String current = preferences.getString(SOURCE_SHELF_KEY, null);
        if (current == null) return new ArrayList<>();
        return AppShelfCodec.decode(current.getBytes(StandardCharsets.UTF_8), catalog);
    }

    private boolean persistSourcePins(List<String> pins, AppShelfCodec.Catalog catalog) {
        try {
            String portable = AppShelfCodec.encode(pins, catalog, java.time.Instant.now());
            if (!preferences.edit().putString(SOURCE_SHELF_KEY, portable).commit())
                throw new java.io.IOException("Private bookmark storage refused to write.");
            return true;
        } catch (Exception error) {
            showBackupError("Source-only shelf not saved", error);
            return false;
        }
    }

    private void openSourceShelf() {
        try {
            AppShelfCodec.Catalog catalog = sourceCatalog();
            List<String> pins = sourcePins(catalog);
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(8), dp(16), dp(10));
            content.addView(text("PUBLIC SOURCE BOOKMARKS · NOT INSTALLED APPS", 12, CYAN, true));
            content.addView(text(
                    pins.size() + " / 24 pinned. Same portable JSON as PhiOS Web and Mobile PWA. "
                    + "No silent APK installs, Home changes, remote synchronization or permissions. "
                    + "View Source opens an external browser only if you tap it.",
                    12, MUTED, false));
            final AlertDialog[] holder = new AlertDialog[1];
            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.VERTICAL);
            Button add = button("Find and pin public source", () -> {
                holder[0].dismiss();
                searchPublicSource(catalog);
            });
            Button export = button("Export source shelf JSON", () -> {
                holder[0].dismiss();
                beginExportSourceShelf();
            });
            Button restore = button("Import source shelf JSON", () -> {
                holder[0].dismiss();
                beginImportSourceShelf();
            });
            controls.addView(add);
            controls.addView(export);
            controls.addView(restore);
            content.addView(controls);

            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            if (pins.isEmpty()) list.addView(text(
                    "No source bookmarks yet. Use Find and pin public source.", 12, MUTED, false));
            for (int i = 0; i < pins.size(); i++) {
                String id = pins.get(i);
                int index = i;
                AppShelfCodec.Source source = catalog.sources.get(id);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(7), dp(7), dp(7), dp(7));
                row.setBackground(surface(PANEL, 10, Color.rgb(45, 92, 111)));
                row.addView(text(source.name + " · SOURCE ONLY", 12, CYAN, true));
                LinearLayout buttons = new LinearLayout(this);
                buttons.setGravity(Gravity.CENTER_VERTICAL);
                Button up = button("↑", () -> reorderSourcePin(pins, index, -1, catalog, holder[0]));
                up.setContentDescription("Move " + source.name + " up");
                up.setEnabled(i > 0);
                Button down = button("↓", () -> reorderSourcePin(pins, index, 1, catalog, holder[0]));
                down.setContentDescription("Move " + source.name + " down");
                down.setEnabled(i + 1 < pins.size());
                Button remove = button("Unpin", () -> {
                    List<String> updated = new ArrayList<>(pins);
                    updated.remove(id);
                    if (persistSourcePins(updated, catalog)) {
                        holder[0].dismiss();
                        openSourceShelf();
                    }
                });
                remove.setContentDescription("Remove public-source bookmark " + source.name);
                Button view = button("Source ↗", () -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(source.repository)));
                    } catch (ActivityNotFoundException error) {
                        Toast.makeText(this, "No external browser is available.", Toast.LENGTH_LONG).show();
                    }
                });
                view.setContentDescription("Open external browser for GitHub source " + source.name);
                for (Button button : new Button[]{up, down, remove, view})
                    buttons.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
                row.addView(buttons);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.topMargin = dp(8);
                list.addView(row, rp);
            }
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, dp(310)));
            holder[0] = new AlertDialog.Builder(this)
                    .setTitle("Φ Portable Source Shelf")
                    .setView(content)
                    .setNegativeButton("Close", null)
                    .create();
            holder[0].show();
        } catch (Exception error) {
            new AlertDialog.Builder(this)
                    .setTitle("Source shelf unavailable")
                    .setMessage("The catalog or saved bookmark format could not be validated. "
                            + "Existing Android app pins are untouched. "
                            + "Reset only the source bookmarks if needed.\n\n"
                            + String.valueOf(error.getMessage()))
                    .setPositiveButton("Reset source bookmarks", (dialog, which) -> {
                        preferences.edit().remove(SOURCE_SHELF_KEY).apply();
                        openSourceShelf();
                    })
                    .setNegativeButton("Cancel", null).show();
        }
    }

    private void reorderSourcePin(List<String> pins, int index, int offset,
                                  AppShelfCodec.Catalog catalog, AlertDialog dialog) {
        int target = index + offset;
        if (index < 0 || target < 0 || target >= pins.size()) return;
        List<String> reordered = new ArrayList<>(pins);
        Collections.swap(reordered, index, target);
        if (persistSourcePins(reordered, catalog)) {
            dialog.dismiss();
            openSourceShelf();
        }
    }

    private void searchPublicSource(AppShelfCodec.Catalog catalog) {
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Type project name (e.g. PhiOS)");
        new AlertDialog.Builder(this)
                .setTitle("Find public source · 225 snapshot")
                .setMessage("This lists public GitHub projects only, not APKs or launchable apps.")
                .setView(query)
                .setPositiveButton("Find", (dialog, which) -> {
                    String needle = query.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
                    if (needle.isEmpty()) {
                        openSourceShelf();
                        return;
                    }
                    List<AppShelfCodec.Source> matches = new ArrayList<>();
                    for (AppShelfCodec.Source source : catalog.sources.values()) {
                        if (source.name.toLowerCase(java.util.Locale.ROOT).contains(needle)
                                || source.description.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                            matches.add(source);
                            if (matches.size() >= 20) break;
                        }
                    }
                    if (matches.isEmpty()) {
                        Toast.makeText(this, "No matching public source in snapshot.", Toast.LENGTH_LONG).show();
                        openSourceShelf();
                        return;
                    }
                    String[] titles = new String[matches.size()];
                    for (int i = 0; i < matches.size(); i++)
                        titles[i] = matches.get(i).name + " · SOURCE ONLY";
                    new AlertDialog.Builder(this)
                            .setTitle("Choose a source bookmark")
                            .setItems(titles, (choice, selected) -> {
                                AppShelfCodec.Source chosen = matches.get(selected);
                                try {
                                    List<String> pins = sourcePins(catalog);
                                    if (pins.contains(chosen.id)) {
                                        Toast.makeText(this, "Source already pinned.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        pins.add(chosen.id);
                                        if (!persistSourcePins(AppShelfCodec.validate(pins, catalog), catalog)) return;
                                    }
                                    openSourceShelf();
                                } catch (Exception error) {
                                    showBackupError("Source pin refused", error);
                                }
                            })
                            .setNegativeButton("Cancel", (choice, which2) -> openSourceShelf())
                            .show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> openSourceShelf())
                .show();
    }

    private void beginExportSourceShelf() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "phios-app-shelf-r2.json");
        try {
            startActivityForResult(intent, EXPORT_SOURCE_SHELF_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No document provider for source export.", Toast.LENGTH_LONG).show();
        }
    }

    private void beginImportSourceShelf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, IMPORT_SOURCE_SHELF_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No document provider for source import.", Toast.LENGTH_LONG).show();
        }
    }

    private void exportSourceShelf(Uri document) {
        try {
            AppShelfCodec.Catalog catalog = sourceCatalog();
            String json = AppShelfCodec.encode(sourcePins(catalog), catalog, java.time.Instant.now());
            try (OutputStream out = getContentResolver().openOutputStream(document, "wt")) {
                if (out == null) throw new java.io.IOException("Document provider refused write.");
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            Toast.makeText(this, "Source-only shelf exported. Installed apps unchanged.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            showBackupError("Source shelf export failed", error);
        }
    }

    private void importSourceShelf(Uri document) {
        try {
            AppShelfCodec.Catalog catalog = sourceCatalog();
            byte[] data = readBoundedDocument(document);
            List<String> proposed = AppShelfCodec.decode(data, catalog);
            StringBuilder preview = new StringBuilder("Replace ONLY source bookmarks with "
                    + proposed.size() + " public GitHub IDs?\n\n");
            for (String id : proposed)
                preview.append("• ").append(catalog.sources.get(id).name).append("\n");
            preview.append("\nInstalled Android app pins, layout, Home and permissions stay unchanged.");
            new AlertDialog.Builder(this)
                    .setTitle("Review portable source shelf")
                    .setMessage(preview.toString())
                    .setPositiveButton("Restore source pins", (dialog, which) -> {
                        if (persistSourcePins(proposed, catalog)) openSourceShelf();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception error) {
            showBackupError("Source shelf import refused", error);
        }
    }

    private void showBackupError(String message, Exception error) {
        String detail = error.getMessage() == null ? "Unknown document error" : error.getMessage();
        if (detail.length() > 150) detail = detail.substring(0, 150);
        Toast.makeText(this, message + ": " + detail, Toast.LENGTH_LONG).show();
    }

    private void openHomeRecoveryGuide() {
        new AlertDialog.Builder(this)
                .setTitle("Restore your original Home")
                .setMessage("PhiOS is only a user-selectable Android launcher. It cannot "
                        + "silently change the system Home app.\n\n"
                        + "To return to your original launcher:\n"
                        + "1. Open Android Settings → Apps → Default apps → Home app.\n"
                        + "2. Select the stock Home app that was installed before PhiOS.\n"
                        + "3. Press Home and confirm your original launcher opens.\n"
                        + "4. Only then uninstall PhiOS Mobile if you want to.\n\n"
                        + "If your device uses different menus, follow its manufacturer "
                        + "instructions. Save a local JSON backup of your pins and layout "
                        + "before uninstalling: uninstalling removes those app preferences. "
                        + "No firmware flashing, rooting or reset is required.")
                .setPositiveButton("Open Home settings", (dialog, which) -> chooseDefaultHome())
                .setNegativeButton("Close", null)
                .show();
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
        container.addView(dial, new LinearLayout.LayoutParams(-1,
                dp(LauncherLayoutPolicy.surfDialHeightDp(
                        getResources().getConfiguration().screenHeightDp,
                        getResources().getConfiguration().fontScale))));

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
