/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.malacca.purevideo;

import android.content.Context;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

/**
 * A {@link FrameLayout} that resizes itself to match a specified aspect ratio.
 */
final class VideoAspectFrameLayout extends FrameLayout {
    /**
     * The {@link FrameLayout} will not resize itself if the fractional difference between its natural
     * aspect ratio and the requested aspect ratio falls below this threshold.
     * <p>
     * This tolerance allows the view to occupy the whole of the screen when the requested aspect
     * ratio is very close, but not exactly equal to, the aspect ratio of the screen. This may reduce
     * the number of view layers that need to be composited by the underlying system, which can help
     * to reduce power consumption.
     */
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;

    private float videoAspectRatio;
    private int videoNativeWidth;
    private int videoNativeHeight;
    private @VideoResizeMode.Mode int resizeMode = VideoResizeMode.RESIZE_MODE_COVER;

    public VideoAspectFrameLayout(@NonNull Context context) {
        super(context);
    }

    /**
     * Set the aspect ratio that this view should satisfy.
     */
    public void setAspectRatio(int width, int height, float pixelWidthHeightRatio) {
        float widthHeightRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
        if (width == videoNativeWidth && height == videoNativeHeight && widthHeightRatio == videoAspectRatio) {
            return;
        }
        videoNativeWidth = Math.round(width * pixelWidthHeightRatio);
        videoNativeHeight = height;
        videoAspectRatio = widthHeightRatio;
        requestLayout();
    }

    /**
     * Get the aspect ratio that this view should satisfy.
     * @return widthHeightRatio The width to height ratio.
     */
    public float getAspectRatio() {
        return videoAspectRatio;
    }

    /**
     * Sets the resize mode which can be of value {@link VideoResizeMode.Mode}
     *
     * @param resizeMode The resize mode.
     */
    public void setResizeMode(@VideoResizeMode.Mode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    /**
     * Gets the resize mode which can be of value {@link VideoResizeMode.Mode}
     *
     * @return resizeMode The resize mode.
     */
    public @VideoResizeMode.Mode int getResizeMode() {
        return resizeMode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio == 0) {
            // Aspect ratio not set.
            return;
        }

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        float viewAspectRatio = (float) measuredWidth / measuredHeight;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance.
            return;
        }

        int width = measuredWidth;
        int height = measuredHeight;
        switch (resizeMode) {
            case VideoResizeMode.RESIZE_MODE_CONTAIN:
                if (aspectDeformation > 0) {
                    height = (int) (measuredWidth / videoAspectRatio);
                } else {
                    width = (int) (measuredHeight * videoAspectRatio);
                }
                break;
            case VideoResizeMode.RESIZE_MODE_STRETCH:
                // do nothing
                break;
            case VideoResizeMode.RESIZE_MODE_CENTER:
                if (videoNativeWidth <= measuredWidth && videoNativeHeight <= measuredHeight) {
                    width = videoNativeWidth;
                    height = videoNativeHeight;
                } else if (aspectDeformation > 0) {
                    height = (int) (measuredWidth / videoAspectRatio);
                } else {
                    width = (int) (measuredHeight * videoAspectRatio);
                }
                break;
            case VideoResizeMode.RESIZE_MODE_COVER:
            default:
                width = (int) (measuredHeight * videoAspectRatio);
                if (width < measuredWidth) {
                    float scaleFactor = (float) measuredWidth / width;
                    width = (int) (width * scaleFactor);
                    height = (int) (measuredHeight * scaleFactor);
                }
                break;
        }
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
}
