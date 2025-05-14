package com.example.lumew.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public class SwipeHelper implements View.OnTouchListener {
    private static final float SWIPE_THRESHOLD = 100f;
    private static final float ROTATION_DEGREES = 15f;
    private float initialX, initialY;
    private final View view;
    private final SwipeCallback callback;
    private final TextView likeIndicator;
    private final TextView dislikeIndicator;

    public interface SwipeCallback {
        void onSwipedRight();
        void onSwipedLeft();
        void onCardReset();
    }

    public SwipeHelper(View view, TextView likeIndicator, TextView dislikeIndicator,
                       SwipeCallback callback) {
        this.view = view;
        this.callback = callback;
        this.likeIndicator = likeIndicator;
        this.dislikeIndicator = dislikeIndicator;
        view.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = view.getX() - event.getRawX();
                initialY = view.getY() - event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() + initialX;
                float dy = event.getRawY() + initialY;

                // Move card with finger
                view.setX(dx);
                view.setY(dy);

                // Apply rotation based on horizontal movement
                float rotation = ROTATION_DEGREES * (dx / (view.getWidth()/2f));
                view.setRotation(rotation);

                // Update indicators
                updateSwipeIndicators(dx);
                return true;

            case MotionEvent.ACTION_UP:
                handleRelease();
                return true;
        }
        return false;
    }

    private void updateSwipeIndicators(float dX) {
        float alpha = Math.min(1f, Math.abs(dX) / 500f);
        if (dX > view.getWidth() * 0.1f) {
            likeIndicator.setVisibility(View.VISIBLE);
            dislikeIndicator.setVisibility(View.INVISIBLE);
            likeIndicator.setAlpha(alpha);
        } else if (dX < -view.getWidth() * 0.1f) {
            dislikeIndicator.setVisibility(View.VISIBLE);
            likeIndicator.setVisibility(View.INVISIBLE);
            dislikeIndicator.setAlpha(alpha);
        } else {
            likeIndicator.setVisibility(View.INVISIBLE);
            dislikeIndicator.setVisibility(View.INVISIBLE);
        }
    }

    private void handleRelease() {
        float centerX = view.getWidth() / 2f;
        float currentX = view.getX() + centerX;
        float threshold = view.getWidth() * 0.25f;

        if (currentX > centerX + threshold) {
            animateExit(true);
        } else if (currentX < centerX - threshold) {
            animateExit(false);
        } else {
            resetCard();
        }
    }

    private void animateExit(boolean isRight) {
        float endX = isRight ? view.getWidth() * 2 : -view.getWidth() * 2;
        view.animate()
                .x(endX)
                .rotation(isRight ? ROTATION_DEGREES : -ROTATION_DEGREES)
                .alpha(0.7f)
                .withEndAction(() -> {
                    if (isRight) callback.onSwipedRight();
                    else callback.onSwipedLeft();
                })
                .start();
    }

    private void resetCard() {
        view.animate()
                .x(0)
                .y(0)
                .rotation(0)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .withEndAction(callback::onCardReset)
                .start();

        likeIndicator.setVisibility(View.INVISIBLE);
        dislikeIndicator.setVisibility(View.INVISIBLE);
    }
}