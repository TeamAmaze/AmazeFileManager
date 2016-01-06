package com.futuremind.recyclerviewfastscroll;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by mklimczak on 28/07/15.
 */
public class FastScroller extends LinearLayout {

    private FastScrollBubble bubble;
    private ImageView handle;
    private int bubbleOffset;

    private int scrollerOrientation;

    private RecyclerView recyclerView;

    private final ScrollListener scrollListener = new ScrollListener();

    private boolean manuallyChangingPosition;

    private SectionTitleProvider titleProvider;


    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(isVertical() ? R.layout.fastscroller_vertical : R.layout.fastscroller_horizontal, this);
    }

    @Override //TODO should probably use some custom orientation instead of linear layout one
    public void setOrientation(int orientation) {
        scrollerOrientation = orientation;
        //switching orientation, because orientation in linear layout
        //is something different than orientation of fast scroller
        super.setOrientation(orientation == HORIZONTAL ? VERTICAL : HORIZONTAL);
    }

    /**
     * Attach the FastScroller to RecyclerView. Should be used after the Adapter is set
     * to the RecyclerView. If the adapter implements SectionTitleProvider, the FastScroller
     * will show a bubble with title.
     * @param recyclerView A RecyclerView to attach the FastScroller to
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        if(recyclerView.getAdapter() instanceof SectionTitleProvider) titleProvider = (SectionTitleProvider) recyclerView.getAdapter();
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
        bubble = (FastScrollBubble) findViewById(R.id.fastscroller_bubble);
        handle = (ImageView) findViewById(R.id.fastscroller_handle);
        bubbleOffset = (int) (isVertical() ? ((float)handle.getHeight()/2f)-bubble.getHeight() : ((float)handle.getWidth()/2f)-bubble.getWidth());
        initHandleMovement();
    }
    public void setColor(int color){
        if(handle==null)handle = (ImageView) findViewById(R.id.fastscroller_handle);
        handle.setColorFilter(color);
    }
    private void initHandleMovement() {
        handle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

                    if(titleProvider!=null) bubble.show();
                    manuallyChangingPosition = true;

                    float relativePos = getRelativeTouchPosition(event);
                    setHandlePosition(relativePos);
                    setRecyclerViewPosition(relativePos);

                    return true;

                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    manuallyChangingPosition = false;
                    if(titleProvider!=null) bubble.hide();
                    return true;

                }

                return false;

            }
        });
    }

    private float getRelativeTouchPosition(MotionEvent event){
        if(isVertical()){
            float yInParent = event.getRawY() - Utils.getViewRawY(handle);
            return yInParent / (getHeight() - handle.getHeight());
        } else {
            float xInParent = event.getRawX() - Utils.getViewRawX(handle);
            return xInParent / (getWidth() - handle.getWidth());
        }
    }

    private void invalidateVisibility() {
        if(
                recyclerView.getAdapter()==null ||
                recyclerView.getAdapter().getItemCount()==0 ||
                recyclerView.getChildAt(0)==null ||
                isRecyclerViewScrollable() //TODO make it dependent on the orientation
                ){
            setVisibility(INVISIBLE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private boolean isRecyclerViewScrollable() {
        if(isVertical()) {
            return recyclerView.getChildAt(0).getHeight() * recyclerView.getAdapter().getItemCount() <= getHeight();
        } else {
            return recyclerView.getChildAt(0).getWidth() * recyclerView.getAdapter().getItemCount()<=getWidth();
        }
    }

    private void setRecyclerViewPosition(float relativePos) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            int targetPos = (int) Utils.getValueInRange(0, itemCount - 1, (int) (relativePos * (float) itemCount));
            recyclerView.scrollToPosition(targetPos);
            if(titleProvider!=null) bubble.setText(titleProvider.getSectionTitle(targetPos));
        }
    }

    private void setHandlePosition(float relativePos) {
        if(isVertical()) {
            if(bubble!=null)
            bubble.setY(Utils.getValueInRange(
                    0,
                    getHeight() - bubble.getHeight(),
                    relativePos * (getHeight() - handle.getHeight()) + bubbleOffset)
            );
            handle.setY(Utils.getValueInRange(
                    0,
                    getHeight() - handle.getHeight(),
                    relativePos * (getHeight() - handle.getHeight()))
            );
        } else {
            bubble.setX(Utils.getValueInRange(
                    0,
                    getWidth() - bubble.getWidth(),
                    relativePos * (getWidth() - handle.getWidth()) + bubbleOffset)
            );
            handle.setX(Utils.getValueInRange(
                    0,
                    getWidth() - handle.getWidth(),
                    relativePos * (getWidth() - handle.getWidth()))
            );
        }
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
            if(handle!=null && !manuallyChangingPosition) {
                View firstVisibleView = recyclerView.getChildAt(0);
                float recyclerViewOversize; //how much is recyclerView bigger than fastScroller
                int recyclerViewAbsoluteScroll;
                if(firstVisibleView==null || recyclerView==null || rv==null)return;
                if(isVertical()) {
                    recyclerViewOversize = firstVisibleView.getHeight() * rv.getAdapter().getItemCount() - getHeight();
                    recyclerViewAbsoluteScroll = recyclerView.getChildLayoutPosition(firstVisibleView) * firstVisibleView.getHeight() - firstVisibleView.getTop();
                } else {
                    recyclerViewOversize = firstVisibleView.getWidth() * rv.getAdapter().getItemCount() - getWidth();
                    recyclerViewAbsoluteScroll = recyclerView.getChildLayoutPosition(firstVisibleView) * firstVisibleView.getWidth() - firstVisibleView.getLeft();
                }
                setHandlePosition(recyclerViewAbsoluteScroll / recyclerViewOversize);
            }
        }
    }

    private boolean isVertical(){
        return scrollerOrientation == VERTICAL;
    }

}
