package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;

public class FastScroller extends FrameLayout {
    private View bar;
    private ImageView handle;
    private RecyclerView recyclerView;
    private final ScrollListener scrollListener;
    boolean manuallyChangingPosition = false;
    int columns = 1;

    private class ScrollListener extends OnScrollListener {
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            if (handle != null && !manuallyChangingPosition) {
                updateHandlePosition();
            }
        }
    }

    public FastScroller(@NonNull Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.scrollListener = new ScrollListener();
        initialise(context);
    }

    public FastScroller(@NonNull Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.scrollListener = new ScrollListener();
        initialise(context);
    }

    private float computeHandlePosition() {
        View firstVisibleView = recyclerView.getChildAt(0);
        handle.setVisibility(VISIBLE);
        float recyclerViewOversize; //how much is recyclerView bigger than fastScroller
        int recyclerViewAbsoluteScroll;
        if (firstVisibleView == null || recyclerView == null) return -1;
        recyclerViewOversize = firstVisibleView.getHeight() / columns * recyclerView.getAdapter().getItemCount() - getHeightMinusPadding();
        recyclerViewAbsoluteScroll = recyclerView.getChildLayoutPosition(firstVisibleView) / columns * firstVisibleView.getHeight() - firstVisibleView.getTop();
        return recyclerViewAbsoluteScroll / recyclerViewOversize;
    }

    private int getHeightMinusPadding() {
        return (getHeight() - getPaddingBottom()) - getPaddingTop();
    }


    private void initialise(@NonNull Context context) {
        setClipChildren(false);
        inflate(context, R.layout.fastscroller, this);
        this.handle = (ImageView) findViewById(R.id.scroll_handle);
        this.bar = findViewById(R.id.scroll_bar);
        this.handle.setEnabled(true);
        setPressedHandleColor(getResources().getColor(R.color.accent_blue));
        setUpBarBackground();
        setVisibility(VISIBLE);
    }

    private void setHandlePosition1(float relativePos) {
        handle.setY(Futils.getValueInRange(
                0, getHeightMinusPadding() - handle.getHeight(), relativePos * (getHeightMinusPadding() - handle.getHeight()))
        );

    }

    private void setUpBarBackground() {
        InsetDrawable insetDrawable;
        int resolveColor = resolveColor(getContext(), R.attr.colorControlNormal);
        insetDrawable = new InsetDrawable(new ColorDrawable(resolveColor), getResources().getDimensionPixelSize(R.dimen.fastscroller_track_padding), 0, 0, 0);
        this.bar.setBackgroundDrawable(insetDrawable);
    }

    int resolveColor(@NonNull Context context, @AttrRes int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = obtainStyledAttributes.getColor(0, 0);
        obtainStyledAttributes.recycle();
        return color;
    }

    onTouchListener a;

    public boolean onTouchEvent(@NonNull MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 || motionEvent.getAction() == 2) {
            this.handle.setPressed(true);
            bar.setVisibility(VISIBLE);
            float relativePos = getRelativeTouchPosition(motionEvent);
            setHandlePosition1(relativePos);
            manuallyChangingPosition = true;
            setRecyclerViewPosition(relativePos);
            // showIfHidden();
            if (a != null) a.onTouch();
            return true;
        } else if (motionEvent.getAction() != 1) {
            return super.onTouchEvent(motionEvent);
        } else {
            bar.setVisibility(INVISIBLE);
            manuallyChangingPosition = false;
            this.handle.setPressed(false);
            // scheduleHide();
            return true;
        }
    }

    private void invalidateVisibility() {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0 || recyclerView.getChildAt(0) == null ||
                isRecyclerViewScrollable()
                ) {
            setVisibility(INVISIBLE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private boolean isRecyclerViewScrollable() {
        return recyclerView.getChildAt(0).getHeight() * recyclerView.getAdapter().getItemCount() / columns <= getHeightMinusPadding() || recyclerView.getAdapter().getItemCount() / columns < 25;

    }

    private void setRecyclerViewPosition(float relativePos) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            int targetPos = (int) Futils.getValueInRange(0, itemCount - 1, (int) (relativePos * (float) itemCount));
            recyclerView.smoothScrollToPosition(targetPos);
        }
    }

    private float getRelativeTouchPosition(MotionEvent event) {
        float yInParent = event.getRawY() - Futils.getViewRawY(handle);
        return yInParent / (getHeightMinusPadding() - handle.getHeight());

    }

    public interface onTouchListener {
        void onTouch();
    }

    public void registerOnTouchListener(onTouchListener onTouchListener) {
        a = onTouchListener;
    }

    public void setPressedHandleColor(int i) {
        handle.setColorFilter(i);
        StateListDrawable stateListDrawable = new StateListDrawable();
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fastscroller_handle_normal);
        Drawable drawable1 = ContextCompat.getDrawable(getContext(), R.drawable.fastscroller_handle_pressed);
        stateListDrawable.addState(View.PRESSED_ENABLED_STATE_SET, new InsetDrawable(drawable1, getResources().getDimensionPixelSize(R.dimen.fastscroller_track_padding), 0, 0, 0));
        stateListDrawable.addState(View.EMPTY_STATE_SET, new InsetDrawable(drawable, getResources().getDimensionPixelSize(R.dimen.fastscroller_track_padding), 0, 0, 0));
        this.handle.setImageDrawable(stateListDrawable);
    }

    public void setRecyclerView(@NonNull RecyclerView recyclerView, int columns) {
        this.recyclerView = recyclerView;
        this.columns = columns;
        bar.setVisibility(INVISIBLE);
        recyclerView.addOnScrollListener(this.scrollListener);
        invalidateVisibility();
        recyclerView.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                invalidateVisibility();
            }


            @Override
            public void onChildViewRemoved(View parent, View child) {
                invalidateVisibility();
            }
        });
    }

    void updateHandlePosition() {
        setHandlePosition1(computeHandlePosition());
    }

    int vx1 = -1;

    public void updateHandlePosition(int vx, int l) {
        if (vx != vx1) {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), l + vx);
            setHandlePosition1(computeHandlePosition());
            vx1 = vx;
        }
    }
}