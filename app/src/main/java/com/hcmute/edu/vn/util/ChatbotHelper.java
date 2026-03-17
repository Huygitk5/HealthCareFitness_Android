package com.hcmute.edu.vn.util;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hcmute.edu.vn.activity.ChatbotActivity;

public class ChatbotHelper {

    private static final int CLICK_DRAG_TOLERANCE = 10;

    /**
     * Cài đặt FAB Chatbot với giới hạn vùng kéo thả tùy chỉnh.
     *
     * @param topMarginDp       Khoảng cách an toàn từ đỉnh cha (dp), ví dụ 80dp để tránh header
     * @param bottomMarginDp    Khoảng cách an toàn từ đáy cha (dp), ví dụ 72dp để tránh bottom nav
     * @param horizontalMarginDp Khoảng cách an toàn từ trái/phải (dp), ví dụ 16dp
     */
    public static void setupChatbotFAB(Context context, FloatingActionButton fabChatbot,
                                       int topMarginDp, int bottomMarginDp, int horizontalMarginDp) {
        if (fabChatbot == null) return;

        float density = context.getResources().getDisplayMetrics().density;
        final float topPx    = topMarginDp * density;
        final float bottomPx = bottomMarginDp * density;
        final float horizPx  = horizontalMarginDp * density;

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
                                // Giới hạn X: giữ khoảng cách ngang
                                float minX = horizPx;
                                float maxX = parent.getWidth() - view.getWidth() - horizPx;
                                // Giới hạn Y: giữ khoảng cách trên (header) và dưới (bottom nav)
                                float minY = topPx;
                                float maxY = parent.getHeight() - view.getHeight() - bottomPx;

                                newX = Math.max(minX, Math.min(newX, maxX));
                                newY = Math.max(minY, Math.min(newY, maxY));
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
                        } else {
                            // Snap về cạnh trái hoặc phải (giữ safe margin ngang)
                            View parent = (View) view.getParent();
                            if (parent != null) {
                                float currentX  = view.getX();
                                float viewCenter = currentX + view.getWidth() / 2f;
                                float parentCenter = parent.getWidth() / 2f;

                                float targetX = (viewCenter < parentCenter)
                                        ? horizPx
                                        : (parent.getWidth() - view.getWidth() - horizPx);

                                view.animate()
                                        .x(targetX)
                                        .setDuration(250)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .start();
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    /**
     * Overload mặc định với safe margins tiêu chuẩn:
     * Trên: 80dp (bên dưới header/profile), Dưới: 72dp (bên trên bottom nav), Ngang: 16dp
     */
    public static void setupChatbotFAB(Context context, FloatingActionButton fabChatbot) {
        setupChatbotFAB(context, fabChatbot, 80, 80, 12);
    }
}
