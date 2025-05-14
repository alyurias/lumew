package com.example.lumew.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeTouchListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private float startX, startY;
    private boolean isSwiping = false;

    public interface SwipeCallback {
        void onSwipeLeft();
        void onSwipeRight();
        void onMove(float deltaX);
    }

    public SwipeTouchListener(Context ctx) {
        gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                isSwiping = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - startX;
                float deltaY = event.getY() - startY;

                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
                    isSwiping = true;
                    if (this instanceof SwipeCallback) {
                        ((SwipeCallback) this).onMove(deltaX);
                    }
                    v.setTranslationX(deltaX);
                    v.setRotation(deltaX / 25f);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isSwiping) {
                    v.performClick();
                } else {
                    v.setTranslationX(0);
                    v.setRotation(0);
                    if (this instanceof SwipeCallback) {
                        ((SwipeCallback) this).onMove(0);
                    }
                }
                break;
        }
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    if (SwipeTouchListener.this instanceof SwipeCallback) {
                        ((SwipeCallback) SwipeTouchListener.this).onSwipeRight();
                    }
                } else {
                    if (SwipeTouchListener.this instanceof SwipeCallback) {
                        ((SwipeCallback) SwipeTouchListener.this).onSwipeLeft();
                    }
                }
                return true;
            }
            return false;
        }
    }

    public void onSwipeLeft() {}
    public void onSwipeRight() {}
    public void onMove(float deltaX) {}
}