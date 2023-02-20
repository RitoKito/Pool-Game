package PoolGame.strategy;

public class BlackBrownStrategy extends PocketStrategy{

    public BlackBrownStrategy() {
        this.lives = 3;
    }
    @Override
    public void reset() { this.lives = 3; }
}
