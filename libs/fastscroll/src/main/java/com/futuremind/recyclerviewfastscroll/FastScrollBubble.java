package com.futuremind.recyclerviewfastscroll;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by mklimczak on 31/07/15.
 */
public class FastScrollBubble extends FrameLayout {

    private static final int BUBBLE_HIDE_DELAY = 1000;
    private static final int BUBBLE_ANIMATION_DURATION = 200;
    private static final String SCALE_X = "scaleX";
    private static final String SCALE_Y = "scaleY";
    private static final String ALPHA = "alpha";

    private final BubbleHider bubbleHider = new BubbleHider();
    private AnimatorSet bubbleHideAnimator = null;

    private TextView textView;

    public FastScrollBubble(Context context) {
        super(context);
        init();
    }

    public FastScrollBubble(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastScrollBubble(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.default_bubble, null);
        addView(textView);
    }

    public void show(){
        if (bubbleHideAnimator != null) {
            bubbleHideAnimator.cancel();
        }
        getHandler().removeCallbacks(bubbleHider);
        if (getVisibility() == INVISIBLE) {
            animateShow();
        }
    }

    public void hide(){
        getHandler().postDelayed(bubbleHider, BUBBLE_HIDE_DELAY);
    }

    private void animateShow() {

        AnimatorSet animatorSet = new AnimatorSet();
        this.setPivotX(this.getWidth());
        this.setPivotY(this.getHeight());
        this.setVisibility(VISIBLE);
        Animator growerX = ObjectAnimator.ofFloat(this, SCALE_X, 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION);
        Animator growerY = ObjectAnimator.ofFloat(this, SCALE_Y, 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION);
        Animator alpha = ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.playTogether(growerX, growerY, alpha);
        animatorSet.start();
    }

    private void animateHide() {
        bubbleHideAnimator = new AnimatorSet();
        this.setPivotX(this.getWidth());
        this.setPivotY(this.getHeight());
        Animator shrinkerX = ObjectAnimator.ofFloat(this, SCALE_X, 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
        Animator shrinkerY = ObjectAnimator.ofFloat(this, SCALE_Y, 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
        Animator alpha = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
        bubbleHideAnimator.setInterpolator(new AccelerateInterpolator());
        bubbleHideAnimator.playTogether(shrinkerX, shrinkerY, alpha);
        bubbleHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                FastScrollBubble.this.setVisibility(INVISIBLE);
                bubbleHideAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                FastScrollBubble.this.setVisibility(INVISIBLE);
                bubbleHideAnimator = null;
            }
        });
        bubbleHideAnimator.start();
    }

    public void setText(String sectionTitle) {
        textView.setText(sectionTitle);
    }


    private class BubbleHider implements Runnable {
        @Override
        public void run() {
            animateHide();
        }
    }

}
