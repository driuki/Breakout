package com.driuki.breakout;

import android.graphics.RectF;

public class Brick {

    private RectF rect;
    private boolean isVisable;

    Brick(int row, int column, int width, int height) {

        isVisable = true;

        // Padding is 1px
        int padding = 1;

        rect = new RectF(column * width + padding,
                row * height + padding,
                column * width + width - padding,
                row * height + height - padding);

    }

    RectF getRect() {
        return this.rect;
    }

    // When a brick get's hit - make it invisible
    void setInvisible() {
        isVisable = false;
    }

    // To know visibility of a brick when we need to do something with the brick
    boolean getVisibility() {
        return isVisable;
    }

}
