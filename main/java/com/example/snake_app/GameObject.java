package com.example.snake_app;

import android.graphics.Bitmap;

public abstract class GameObject {
    private int [] posX, posY;
    private Bitmap bitmap;

    public int[] getPosX() {
        return posX;
    }

    public void setPosX(int[] posX) {
        this.posX = posX;
    }

    public void setPosX(int posX, int value) {
        this.posX[posX] = value;
    }

    public int[] getPosY() {
        return posY;
    }

    public void setPosY(int[] posY) {
        this.posY = posY;
    }

    public void setPosY(int posY, int value) {
        this.posY[posY] = value;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

}
