package PoolGame.objects;

import java.util.ArrayList;

public class SystemMemento {
    private ArrayList<BallMemento> ballMementos = new ArrayList<>();
    public ArrayList<BallMemento> getBallMementos() { return  ballMementos; }

    private boolean winFlag;
    public boolean getWinFlag() { return winFlag; }

    private int score;
    public int getScore() { return score; }

    private double elapsedSeconds;
    public double getElapsedSeconds() { return elapsedSeconds; }

    private double elapsedMinutes;
    public double getElapsedMinutes() { return elapsedMinutes; }

    public SystemMemento(Builder builder){
        this.ballMementos = builder.ballMementos;
        this.winFlag = builder.winFlag;
        this.score = builder.score;
        this.elapsedSeconds = builder.elapsedTime;
        this.elapsedMinutes = builder.elapsedMinutes;
    }

    public static class Builder {
        protected ArrayList<BallMemento> ballMementos = new ArrayList<>();
        protected boolean winFlag;
        protected int score;
        protected double elapsedTime;
        protected double elapsedMinutes;

        public Builder setBalls(ArrayList<BallMemento> ballMementos){
            this.ballMementos = ballMementos;
            return this;
        }

        public Builder setWinFlag(boolean winFlag){
            this.winFlag = winFlag;
            return this;
        }

        public Builder setScore(int score){
            this.score = score;
            return this;
        }

        public Builder setElapsedSeconds(double elapsedSeconds){
            this.elapsedTime = elapsedSeconds;
            return this;
        }

        public Builder setElapsedMinutes(double elapsedMinutes){
            this.elapsedMinutes = elapsedMinutes;
            return this;
        }

        public SystemMemento buildObject(){
            SystemMemento momento = new SystemMemento(this);
            return momento;
        }
    }
}
