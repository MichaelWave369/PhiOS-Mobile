package org.enterthefield.phiosmobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * A native, touch-driven PhiSurf selector.
 * The adjacent ordinary buttons in the hosting dialog provide a keyboard and
 * screen-reader alternative to the radial touch affordance.
 */
public final class PhiSurfDial extends View {
    public interface SelectionListener { void onSelect(int sector); }

    private static final int CYAN = Color.rgb(121, 232, 235);
    private static final int PANEL = Color.rgb(17, 30, 48);
    private static final String[] LABELS =
            { "ALL", "PINNED", "SEARCH", "SETTINGS", "STATUS", "CLOSE" };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SelectionListener listener;
    private int selected = -1;

    public PhiSurfDial(Context context, SelectionListener listener) {
        super(context);
        this.listener = listener;
        setContentDescription("PhiSurf radial selector. Alternative navigation buttons follow below.");
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = (int) dp(292);
        setMeasuredDimension(resolveSize(desired, widthMeasureSpec),
                resolveSize(desired, heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.68f;

        paint.setColor(Color.rgb(48, 76, 98));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(Color.rgb(28, 53, 70));
        canvas.drawCircle(cx, cy, radius * 0.60f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CYAN);
        paint.setTextSize(dp(46));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Φ", cx, cy + dp(16), paint);

        for (int i = 0; i < LABELS.length; i++) {
            double angle = i * Math.PI / 3d;
            float x = cx + (float) Math.sin(angle) * radius;
            float y = cy - (float) Math.cos(angle) * radius;

            paint.setColor(i == selected ? Color.rgb(42, 92, 110) : PANEL);
            canvas.drawCircle(x, y, dp(i == selected ? 40 : 36), paint);
            paint.setColor(CYAN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(i == selected ? 3 : 1));
            canvas.drawCircle(x, y, dp(i == selected ? 40 : 36), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(10));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(LABELS[i], x, y + dp(4), paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - getWidth() / 2f;
        float dy = event.getY() - getHeight() / 2f;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                selected = LauncherLogic.dialSector(dx, dy, dp(40));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                int finalSelection = LauncherLogic.dialSector(dx, dy, dp(40));
                selected = -1;
                invalidate();
                performClick();
                if (finalSelection >= 0) listener.onSelect(finalSelection);
                return true;
            case MotionEvent.ACTION_CANCEL:
                selected = -1;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
