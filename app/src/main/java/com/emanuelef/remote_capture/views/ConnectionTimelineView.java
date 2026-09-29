/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 - Wk & PCAPdroid Contributors
 */

package com.emanuelef.remote_capture.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

public class ConnectionTimelineView extends View {
    private int mTcpConnectMs = -1;
    private int mTlsSetupMs = -1;
    private int mServerWaitMs = -1;

    private final Paint mPaintBlock = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaintSubtext = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();

    // Distinct theme colors
    private static final int COLOR_TCP = 0xFF1E88E5;     // Vibrant Blue
    private static final int COLOR_TLS = 0xFF8E24AA;     // Vibrant Purple
    private static final int COLOR_WAIT = 0xFFFB8C00;    // Vibrant Amber/Orange
    private static final int COLOR_EMPTY = 0xFFB0BEC5;   // Soft Grey
    private static final int COLOR_TEXT_DARK = 0xFF212121;
    private static final int COLOR_TEXT_LIGHT = 0xFFFFFFFF;

    public ConnectionTimelineView(Context context) {
        super(context);
        init();
    }

    public ConnectionTimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConnectionTimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaintText.setTextSize(sp2px(11f));
        mPaintText.setTextAlign(Paint.Align.CENTER);
        mPaintText.setFakeBoldText(true);

        mPaintSubtext.setTextSize(sp2px(10f));
        mPaintSubtext.setTextAlign(Paint.Align.CENTER);
        mPaintSubtext.setColor(0xFF757575);

        mPaintLine.setStrokeWidth(dp2px(1.5f));
        mPaintLine.setColor(0xFFBDBDBD);
    }

    public void setTimingData(int tcpConnectMs, int tlsSetupMs, int serverWaitMs) {
        this.mTcpConnectMs = tcpConnectMs;
        this.mTlsSetupMs = tlsSetupMs;
        this.mServerWaitMs = serverWaitMs;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp2px(86f);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0)
            return;

        float paddingLeft = dp2px(12f);
        float paddingRight = dp2px(12f);
        float contentWidth = width - paddingLeft - paddingRight;

        int tcp = Math.max(0, mTcpConnectMs);
        int tls = Math.max(0, mTlsSetupMs);
        int wait = Math.max(0, mServerWaitMs);
        int totalRaw = tcp + tls + wait;

        if (totalRaw <= 0) {
            // Draw empty / placeholder bar
            float barY = height * 0.35f;
            float barHeight = dp2px(26f);
            mRect.set(paddingLeft, barY, width - paddingRight, barY + barHeight);
            mPaintBlock.setColor(0xFFECEFF1);
            canvas.drawRoundRect(mRect, dp2px(6f), dp2px(6f), mPaintBlock);

            mPaintSubtext.setColor(COLOR_EMPTY);
            canvas.drawText("等待抓取到有效 TCP / TLS / HTTP 报文交互以生成时序...",
                    width / 2f, barY + barHeight / 2f + dp2px(4f), mPaintSubtext);
            return;
        }

        // Calculate phase weights with minimum visible width enforcement
        int phasesCount = (tcp > 0 ? 1 : 0) + (tls > 0 ? 1 : 0) + (wait > 0 ? 1 : 0);
        float minWidthPerPhase = dp2px(54f);
        float availableWidth = contentWidth;

        float tcpW = 0, tlsW = 0, waitW = 0;
        if (phasesCount * minWidthPerPhase >= availableWidth) {
            // Equal division if space is constrained
            float perW = availableWidth / phasesCount;
            if (tcp > 0) tcpW = perW;
            if (tls > 0) tlsW = perW;
            if (wait > 0) waitW = perW;
        } else {
            // Proportional allocation with minimum width guard
            float remainingWidth = availableWidth - (phasesCount * minWidthPerPhase);
            float tcpPortion = (float) tcp / totalRaw;
            float tlsPortion = (float) tls / totalRaw;
            float waitPortion = (float) wait / totalRaw;

            if (tcp > 0) tcpW = minWidthPerPhase + (remainingWidth * tcpPortion);
            if (tls > 0) tlsW = minWidthPerPhase + (remainingWidth * tlsPortion);
            if (wait > 0) waitW = minWidthPerPhase + (remainingWidth * waitPortion);
        }

        float barY = dp2px(18f);
        float barHeight = dp2px(28f);
        float curX = paddingLeft;
        float cornerRadius = dp2px(6f);

        // Draw Phase 1: TCP Connect
        if (tcp > 0) {
            mRect.set(curX, barY, curX + tcpW, barY + barHeight);
            mPaintBlock.setColor(COLOR_TCP);
            canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, mPaintBlock);

            mPaintText.setColor(COLOR_TEXT_LIGHT);
            canvas.drawText("TCP 握手", curX + tcpW / 2f, barY + dp2px(12f), mPaintText);
            mPaintText.setColor(0xCCFFFFFF);
            canvas.drawText(tcp + " ms", curX + tcpW / 2f, barY + dp2px(23f), mPaintText);

            // Time annotation below
            drawTimeMarker(canvas, curX, curX + tcpW, barY + barHeight, "0", "+" + tcp + "ms");
            curX += tcpW + dp2px(3f);
        }

        // Draw Phase 2: TLS Setup
        if (tls > 0) {
            mRect.set(curX, barY, curX + tlsW, barY + barHeight);
            mPaintBlock.setColor(COLOR_TLS);
            canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, mPaintBlock);

            mPaintText.setColor(COLOR_TEXT_LIGHT);
            canvas.drawText("TLS 协商", curX + tlsW / 2f, barY + dp2px(12f), mPaintText);
            mPaintText.setColor(0xCCFFFFFF);
            canvas.drawText(tls + " ms", curX + tlsW / 2f, barY + dp2px(23f), mPaintText);

            drawTimeMarker(canvas, curX, curX + tlsW, barY + barHeight, "", "+" + (tcp + tls) + "ms");
            curX += tlsW + dp2px(3f);
        }

        // Draw Phase 3: Server Wait (TTFB)
        if (wait > 0) {
            mRect.set(curX, barY, curX + waitW, barY + barHeight);
            mPaintBlock.setColor(COLOR_WAIT);
            canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, mPaintBlock);

            mPaintText.setColor(COLOR_TEXT_LIGHT);
            canvas.drawText("服务响应", curX + waitW / 2f, barY + dp2px(12f), mPaintText);
            mPaintText.setColor(0xCCFFFFFF);
            String waitStr = (wait >= 1000) ? String.format(Locale.US, "%.2fs", wait / 1000.0f) : (wait + " ms");
            canvas.drawText(waitStr, curX + waitW / 2f, barY + dp2px(23f), mPaintText);

            String endStr = (totalRaw >= 1000) ? String.format(Locale.US, "%.2fs", totalRaw / 1000.0f) : (totalRaw + "ms");
            drawTimeMarker(canvas, curX, curX + waitW, barY + barHeight, "", endStr);
        }
    }

    private void drawTimeMarker(Canvas canvas, float startX, float endX, float bottomY, String startLabel, String endLabel) {
        float markerLineY = bottomY + dp2px(8f);
        canvas.drawLine(startX, markerLineY, endX, markerLineY, mPaintLine);

        // Tick marks
        canvas.drawLine(startX, markerLineY - dp2px(3f), startX, markerLineY + dp2px(3f), mPaintLine);
        canvas.drawLine(endX, markerLineY - dp2px(3f), endX, markerLineY + dp2px(3f), mPaintLine);

        if (startLabel != null && !startLabel.isEmpty()) {
            mPaintSubtext.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(startLabel, startX, markerLineY + dp2px(12f), mPaintSubtext);
        }

        if (endLabel != null && !endLabel.isEmpty()) {
            mPaintSubtext.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(endLabel, endX, markerLineY + dp2px(12f), mPaintSubtext);
        }
    }

    private float dp2px(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float sp2px(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
