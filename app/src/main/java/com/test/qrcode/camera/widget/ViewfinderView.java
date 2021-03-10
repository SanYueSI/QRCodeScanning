/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.test.qrcode.camera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.test.qrcode.R;
import com.test.qrcode.camera.mangger.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final long ANIMATION_DELAY = 10;
    private static final int MAX_RESULT_POINTS = 20;

    private CameraManager cameraManager;
    private final Paint paint;
    private final int maskColor;

    /**
     * 直角绘制画笔
     */
    private Paint rightAnglePaint;
    private Paint linPaint;
    private Paint textPaint;
    /**
     * 4个直角路径
     */
    private Path leftTop, rigTop, leftBottom, rigBottom;
    /**
     * 直角长度
     */
    private int rightAngleLength = 50;

    private int linY = 0;
    private Rect previewFrame;
    private Rect frame;
    /**
     * 扫描线上下移动
     */
    private RectF linRectF;
    /**
     * 扫描线高度
     */
    private int linHeight = 10;
    private int identificationAreaWidth;
    private int identificationAreaHeight;


    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rightAnglePaint = new Paint();
        rightAnglePaint.setColor(Color.RED);
        rightAnglePaint.setStrokeWidth(10);
        rightAnglePaint.setStyle(Paint.Style.STROKE);

        maskColor = ContextCompat.getColor(getContext(), R.color.viewfinder_mask);
        textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(50f);
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        linPaint = new Paint();
        linPaint.setStrokeWidth(1);
        linPaint.setAntiAlias(true);
        linPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linPaint.setColor(Color.RED);
        linPaint.setAlpha(255 / 2);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }


    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {


        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        if (frame == null) {
            frame = cameraManager.getFramingRect();
            identificationAreaWidth = frame.right - frame.left;
            identificationAreaHeight = frame.bottom - frame.top;
        }

        if (previewFrame == null) {
            previewFrame = cameraManager.getFramingRectInPreview();
        }
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (leftTop == null) {
            leftTop = new Path();
            leftTop.moveTo(frame.left - rightAnglePaint.getStrokeWidth() / 2, frame.top + rightAngleLength - rightAnglePaint.getStrokeWidth() / 2);
            leftTop.lineTo(frame.left - rightAnglePaint.getStrokeWidth() / 2, frame.top - rightAnglePaint.getStrokeWidth() / 2);
            leftTop.lineTo(frame.left + rightAngleLength - rightAnglePaint.getStrokeWidth() / 2, frame.top - rightAnglePaint.getStrokeWidth() / 2);
        }
        canvas.drawPath(leftTop, rightAnglePaint);
        if (leftBottom == null) {
            leftBottom = new Path();
            leftBottom.moveTo(frame.left + rightAngleLength - rightAnglePaint.getStrokeWidth() / 2, frame.bottom + rightAnglePaint.getStrokeWidth() / 2);
            leftBottom.lineTo(frame.left - rightAnglePaint.getStrokeWidth() / 2, frame.bottom + rightAnglePaint.getStrokeWidth() / 2);
            leftBottom.lineTo(frame.left - rightAnglePaint.getStrokeWidth() / 2, frame.bottom - rightAngleLength + rightAnglePaint.getStrokeWidth() / 2);
        }
        canvas.drawPath(leftBottom, rightAnglePaint);
        if (rigTop == null) {
            rigTop = new Path();
            rigTop.moveTo(frame.right + rightAnglePaint.getStrokeWidth() / 2, frame.top + rightAngleLength - rightAnglePaint.getStrokeWidth() / 2);
            rigTop.lineTo(frame.right + rightAnglePaint.getStrokeWidth() / 2, frame.top - rightAnglePaint.getStrokeWidth() / 2);
            rigTop.lineTo(frame.right - rightAngleLength + rightAnglePaint.getStrokeWidth() / 2, frame.top - rightAnglePaint.getStrokeWidth() / 2);
        }
        canvas.drawPath(rigTop, rightAnglePaint);
        if (rigBottom == null) {
            rigBottom = new Path();
            rigBottom.moveTo(frame.right - rightAngleLength + rightAnglePaint.getStrokeWidth() / 2, frame.bottom + rightAnglePaint.getStrokeWidth() / 2);
            rigBottom.lineTo(frame.right + rightAnglePaint.getStrokeWidth() / 2, frame.bottom + rightAnglePaint.getStrokeWidth() / 2);
            rigBottom.lineTo(frame.right + rightAnglePaint.getStrokeWidth() / 2, frame.bottom - rightAngleLength + rightAnglePaint.getStrokeWidth() / 2);
        }
        canvas.drawPath(rigBottom, rightAnglePaint);

        if (linY == 0 || linY >= frame.bottom - (linPaint.getStrokeWidth() + linHeight)) {
            linY = frame.top;
        } else {
            linY += 8;
        }
        canvas.drawText("将二维码放置框内，即可开始扫描", getWidth() / 2, frame.bottom + (textPaint.getTextSize() * 2), textPaint);
        if (linRectF == null) {
            linRectF = new RectF(frame.left + rightAngleLength * 2, linY, frame.right - rightAngleLength * 2, linY + linHeight);
        } else {
            linRectF.left = frame.left + rightAngleLength * 2;
            linRectF.top = linY;
            linRectF.right = frame.right - rightAngleLength * 2;
            linRectF.bottom = linY + linHeight;

        }
        canvas.drawOval(linRectF, linPaint);
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left,
                frame.top,
                frame.right,
                frame.bottom);
        Paint a = new Paint();
        a.setStyle(Paint.Style.STROKE);
        a.setColor(Color.WHITE);
        a.setStrokeWidth(1);
//        canvas.drawRect(new RectF(   frame),a);
        canvas.drawCircle(623,392,10,a);
        canvas.drawCircle(192,501,10,a);

    }

    private int px2dip(float pxValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    private float oldDist = 1f;
    private float clickTime = 300;
    private static long lastClickTime;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    long time = System.currentTimeMillis();
                    long timeD = time - lastClickTime;
                    if (0 < timeD && timeD < clickTime) {
                        cameraManager.setMaxOrMinZoom();
                    }
                    lastClickTime = time;
                    break;
                default:
                    break;
            }
        } else {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        cameraManager.setZoom(true);
                    } else if (newDist < oldDist) {
                        cameraManager.setZoom(false);
                    }
                    oldDist = newDist;
                    break;
                default:
                    break;
            }
        }
        return true;

    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        double sqrt = Math.sqrt(x * x + y * y);
        return (float) sqrt;
    }

    public int getIdentificationAreaWidth() {
        return identificationAreaWidth;
    }

    public int getIdentificationAreaHeight() {
        return identificationAreaHeight;
    }
}
