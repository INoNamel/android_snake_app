package com.example.snake_app;

public class Snake extends GameObject {

    private int headDirection = 0, snakeLength = 1;

    public Snake() {}

    public int getHeadDirection() {
        return headDirection;
    }

    public void setHeadDirection(int headDirection) {
        this.headDirection = headDirection;
    }

    public int getSnakeLength() {
        return snakeLength;
    }

    public void setSnakeLength(int snakeLength) {
        this.snakeLength = snakeLength;
    }


}
