package PoolGame.objects;

import PoolGame.strategy.BallStrategy;
import PoolGame.strategy.PocketStrategy;

public class BallMemento {
    private double xPos;
    public double getxPos() { return xPos; }

    private double yPos;
    public double getyPos() { return yPos; }

    private boolean isActive;
    public boolean getIsActive() { return isActive; }

    private PocketStrategy ballStrategy;
    public PocketStrategy getBallStrategy() { return ballStrategy; }

    public BallMemento(double xPos, double yPos, boolean isActive, PocketStrategy ballStrategy){
        this.xPos = xPos;
        this.yPos = yPos;
        this.isActive = isActive;
        this.ballStrategy = ballStrategy;
    }
}
