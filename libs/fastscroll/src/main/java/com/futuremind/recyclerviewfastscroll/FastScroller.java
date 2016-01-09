package com.futuremind.recyclerviewfastscroll;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Created by mklimczak on 28/07/15.
 */
public class FastScroller extends LinearLayout {

    private ImageButton handle;

    private RecyclerView recyclerView;

    private final ScrollListener scrollListener = new ScrollListener();

    private boolean manuallyChangingPosition;

    private SectionTitleProvider titleProvider;


    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.fastscroller_vertical, this);
    }


    /**
     * Attach the FastScroller to RecyclerView. Should be used after the Adapter is set
     * to the RecyclerView. If the adapter implements SectionTitleProvider, the FastScroller
     * will show a bubble with title.
     *
     * @param recyclerView A RecyclerView to attach the FastScroller to
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        if (recyclerView.getAdapter() instanceof SectionTitleProvider)
            titleProvider = (SectionTitleProvider) recyclerView.getAdapter();
        recyclerView.addOnScrollListener(scrollListener);
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        handle = (ImageButton) findViewById(R.id.fastscroller_handle);
        initHandleMovement();
    }

    public void setColor(int color) {
        if (handle == null) handle = (ImageButton) findViewById(R.id.fastscroller_handle);
        handle.setColorFilter(color);
    }

    public interface AppBarListner {
        void onChange(int i);
    }

    public void setAppBarListner(AppBarListner appBarListner) {
        this.appBarListner = appBarListner;
    }

    AppBarListner appBarListner;
    float r = -1;
    int i = -1;

    private void initHandleMovement() {
        handle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

                    manuallyChangingPosition = true;

                    float relativePos = getRelativeTouchPosition(event);
                    setHandlePosition(relativePos);
                    setRecyclerViewPosition(relativePos);
                    if (appBarListner != null)
                        if (r != -1 && relativePos > r && (i == -1 || i == 1)) {
                            {
                                appBarListner.onChange(0);
                                i = 0;
                            }
                        } else if (r != -1 && relativePos < r && (i == -1 || i == 0)) {
                            appBarListner.onChange(1);
                            i = 1;
                        }
                    r = relativePos;
                    return true;

                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    manuallyChangingPosition = false;
                    return true;

                }

                return false;

            }
        });
    }

    private float getRelativeTouchPosition(MotionEvent event) {
        float yInParent = event.getRawY() - Utils.getViewRawY(handle);
        return yInParent / (getHeight() - handle.getHeight());

    }

    private void invalidateVisibility() {
        if (
                recyclerView.getAdapter() == null ||
                        recyclerView.getAdapter().getItemCount() == 0 ||
                        recyclerView.getChildAt(0) == null ||
                        isRecyclerViewScrollable() //TODO make it dependent on the orientation
                ) {
            setVisibility(INVISIBLE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private boolean isRecyclerViewScrollable() {
        return recyclerView.getChildAt(0).getHeight() * recyclerView.getAdapter().getItemCount() <= getHeight();

    }

    private void setRecyclerViewPosition(float relativePos) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            int targetPos = (int) Utils.getValueInRange(0, itemCount - 1, (int) (relativePos * (float) itemCount));
            recyclerView.scrollToPosition(targetPos);
        }
    }

    private void setHandlePosition(float relativePos) {
        handle.setY(Utils.getValueInRange(
                0, getHeight() - handle.getHeight(), relativePos * (getHeight() - handle.getHeight()))
        );

    }

    private class ScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
            if (handle != null && !manuallyChangingPosition) {
                View firstVisibleView = recyclerView.getChildAt(0);
                handle.setVisibility(VISIBLE);
                float recyclerViewOversize; //how much is recyclerView bigger than fastScroller
                int recyclerViewAbsoluteScroll;
                if (firstVisibleView == null || recyclerView == null || rv == null) return;
                recyclerViewOversize = firstVisibleView.getHeight() * rv.getAdapter().getItemCount() - getHeight();
                recyclerViewAbsoluteScroll = recyclerView.getChildLayoutPosition(firstVisibleView) * firstVisibleView.getHeight() - firstVisibleView.getTop();
                setHandlePosition(recyclerViewAbsoluteScroll / recyclerViewOversize);
            }
        }
    }


}
