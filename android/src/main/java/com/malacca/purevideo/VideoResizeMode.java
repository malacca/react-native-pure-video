package com.malacca.purevideo;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;

class VideoResizeMode {

    // 保持比例, 填满容器; 可能显示不完全
    static final int RESIZE_MODE_COVER = 0;

    // 保持比例,填满高度或宽度; 可能有留白
    static final int RESIZE_MODE_CONTAIN = 1;

    // 不保持比例, 拉伸至填满; 可能会变形
    static final int RESIZE_MODE_STRETCH = 2;

    // 居中, 视频大于容器,效果等同于 CONTAIN; 小于容器则居中显示
    static final int RESIZE_MODE_CENTER = 3;

    @Retention(SOURCE)
    @IntDef({
            RESIZE_MODE_COVER,
            RESIZE_MODE_CONTAIN,
            RESIZE_MODE_STRETCH,
            RESIZE_MODE_CENTER
    })
    @interface Mode {
    }

    @VideoResizeMode.Mode
    static int toResizeMode(int ordinal) {
        switch (ordinal) {
            case VideoResizeMode.RESIZE_MODE_CONTAIN:
                return VideoResizeMode.RESIZE_MODE_CONTAIN;

            case VideoResizeMode.RESIZE_MODE_STRETCH:
                return VideoResizeMode.RESIZE_MODE_STRETCH;

            case VideoResizeMode.RESIZE_MODE_CENTER:
                return VideoResizeMode.RESIZE_MODE_CENTER;

            case VideoResizeMode.RESIZE_MODE_COVER:
            default:
                return VideoResizeMode.RESIZE_MODE_COVER;
        }
    }

}