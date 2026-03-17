package com.hcmute.edu.vn.util;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hcmute.edu.vn.activity.ChatbotActivity;

public class ChatbotHelper {

    private static final int CLICK_DRAG_TOLERANCE = 10;

    public static void setupChatbotFAB(Context context, FloatingActionButton fabChatbot) {
        if (fabChatbot == null) return;

        // Xử lý Click mở Chatbot Activity
        fabChatbot.setOnClickListener(v -> {
            Intent chatbotIntent = new Intent(context, ChatbotActivity.class);
            context.startActivity(chatbotIntent);
        });

        // Xử lý kéo thả FAB Chatbot
        fabChatbot.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private boolean isDragging;
            private float startX, startY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > CLICK_DRAG_TOLERANCE || 
                            Math.abs(event.getRawY() - startY) > CLICK_DRAG_TOLERANCE) {
                            isDragging = true;
                        }
                        if (isDragging) {
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;
                            
                            View parent = (View) view.getParent();
                            if (parent != null) {
                                float maxX = parent.getWidth() - view.getWidth();
                                float maxY = parent.getHeight() - view.getHeight();
                                newX = Math.max(0, Math.min(newX, maxX));
                                newY = Math.max(0, Math.min(newY, maxY));
                            }
                            
                            view.animate()
                                .x(newX)
                                .y(newY)
                                .setDuration(0)
                                .start();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            view.performClick();
                        }
                        break;
                }
                return true; 
            }
        });
    }
}
