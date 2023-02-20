package PoolGame;

import PoolGame.objects.*;

import java.util.ArrayList;

import PoolGame.strategy.PocketStrategy;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.Scene;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.paint.Paint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.Pair;

import static java.lang.Math.atan2;

/**
 * Controls the game interface; drawing objects, handling logic and collisions.
 */
public class GameManager {
    private Table table;
    private ArrayList<Ball> balls = new ArrayList<Ball>();
    private Line cueLine;
    private double cueSizeX = 15;
    private double cueSizeY = 300;
    private Rectangle cue;
    private Pane cuePivot;
    private Ball cueBall;
    private void setCueBall() { for (Ball ball: balls) { if(ball.isCue()) { cueBall = ball;}}}
    private Circle cueBallDraw;
    private double hitStrength;
    private double hitStrengthX;
    private double hitStrengthY;

    private boolean cueSet = false;
    private boolean cueActive = false;
    private boolean winFlag = false;

    private int score = 0;
    private Label scoreLabelTxt = new Label("Score:");
    private Label scoreLabel = new Label();

    private int difficulty = -1;

    private long startTime;
    private long elapsedTime;
    private double seconds = 0;
    private double elapsedMinutes = 0;
    private Label elapsedTimeLabel = new Label();


    private final double TABLEBUFFER = Config.getTableBuffer();
    private final double TABLEEDGE = Config.getTableEdge();
    private final double FORCEFACTOR = 0.1;

    private Scene scene;
    private GraphicsContext gc;

    private static GameManager instance = null;

    private Button undoButton = new Button("Undo Last Action");
    private MementoCaretaker mementoCaretaker;


    public static GameManager getInstance(){
        if(instance == null)
            instance = new GameManager();

        return instance;
    }

    /**
     * Initialises timeline and cycle count.
     */

    Timeline mainTimeline = null;
    public void run() {
        if(mainTimeline == null) {
            mainTimeline = new Timeline(new KeyFrame(Duration.millis(17),
                    t -> this.draw()));
        }
        mainTimeline.setCycleCount(Timeline.INDEFINITE);
        mainTimeline.play();
        startTime = System.nanoTime();

        scoreLabel.setText(String.valueOf(score));
    }

    /**
     * Builds GameManager properties such as initialising pane, canvas,
     * graphicscontext, and setting events related to clicks.
     */
    public void buildManager() {
        mementoCaretaker = new MementoCaretaker();

        Pane pane = new Pane();
        setClickEvents(pane);
        setCueBall();

        cue = new Rectangle(0, 0, cueSizeX, cueSizeY);
        cue.setFill(Color.TRANSPARENT);

        cuePivot = new StackPane();
        cuePivot.getChildren().add(cue);
        cuePivot.translateXProperty().set(cueBall.getxPos());
        cuePivot.translateYProperty().set(cueBall.getyPos());

        cueBallDraw = new Circle();

        moveCue();
        hitBall();

        this.scene = new Scene(pane, table.getxLength() + TABLEBUFFER * 2, table.getyLength() + TABLEBUFFER * 2);
        Canvas canvas = new Canvas(table.getxLength() + TABLEBUFFER * 2, table.getyLength() + TABLEBUFFER * 2);
        gc = canvas.getGraphicsContext2D();
        pane.getChildren().add(canvas);
        pane.getChildren().add(cuePivot);
        pane.getChildren().add(cueBallDraw);


        Button menu = new Button("Game Settings");
        menu.setMinSize(100, 35);
        menu.setTranslateX(table.getxLength() * 0.935d);
        menu.setTranslateY(table.getyLength() + TABLEBUFFER * 1.25d);

        menu.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                GridPane menuGrid = new GridPane();
                menuGrid.setVgap(10);
                menuGrid.setHgap(10);
                menuGrid.setPadding(new Insets(10, 10, 10, 10));

                Button changeDifficultyBtn = new Button("Change Difficulty");
                GridPane.setHalignment(changeDifficultyBtn, HPos.CENTER);

                Button cheatBtn = new Button("Cheat: Remove Balls");
                GridPane.setHalignment(cheatBtn, HPos.CENTER);
                cheatBtn.setMaxWidth(Double.MAX_VALUE);
                cheatBtn.setMinWidth(changeDifficultyBtn.getWidth());

                Button closeMenuBtn = new Button("Close Menu");
                GridPane.setHalignment(closeMenuBtn, HPos.CENTER);
                closeMenuBtn.setMinWidth(Double.MAX_VALUE);
                closeMenuBtn.setMinWidth(changeDifficultyBtn.getWidth());

                menuGrid.add(changeDifficultyBtn, 0, 0);
                menuGrid.add(cheatBtn, 0, 1);
                menuGrid.add(closeMenuBtn, 0, 27);

                Stage menuDialogue = new Stage();
                menuDialogue.initModality(Modality.APPLICATION_MODAL);
                menuDialogue.initStyle(StageStyle.UTILITY);
                menuDialogue.setAlwaysOnTop(true);

                menuDialogue.setTitle("Game Settings");
                Scene menuScene = new Scene(menuGrid, 145, 370, Color.GRAY);
                menuDialogue.setScene(menuScene);
                menuDialogue.show();

                closeMenuBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        menuDialogue.hide();
                    }
                });

                handleDifficultySelection(menuGrid, changeDifficultyBtn, menuDialogue, menuScene);
                handleCheatMenu(menuGrid, cheatBtn, menuScene);
            }
        });

        undoButton.setDisable(true);
        undoButton.setMinSize(100, 35);
        undoButton.setTranslateX(table.getxLength() * 0.75d);
        undoButton.setTranslateY(table.getyLength() + TABLEBUFFER * 1.25d);
        undoButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(!mementoCaretaker.isEmpty()){
                    loadMemento();
                }

                if(mementoCaretaker.isEmpty()) { undoButton.setDisable(true); }
            }
        });

        pane.getChildren().add(menu);
        pane.getChildren().add(undoButton);

        Label elapsedTimeLabelTxt = new Label("Elapsed Time: ");
        elapsedTimeLabelTxt.setTranslateY(table.getyLength() + TABLEBUFFER * 1.25d);
        elapsedTimeLabelTxt.setFont(new Font(25));

        elapsedTimeLabel.setTranslateY(table.getyLength() + TABLEBUFFER * 1.3d);
        elapsedTimeLabel.setTranslateX(elapsedTimeLabelTxt.getTranslateX() + 160);
        elapsedTimeLabel.setFont(new Font(24));

        pane.getChildren().add(elapsedTimeLabelTxt);
        pane.getChildren().add(elapsedTimeLabel);


        scoreLabelTxt.setTranslateY(table.getyLength() + TABLEBUFFER * 1.3d);
        scoreLabelTxt.setTranslateX(elapsedTimeLabel.getTranslateX() + 100);
        scoreLabelTxt.setFont(new Font(25));

        scoreLabel.setTranslateY(table.getyLength() + TABLEBUFFER * 1.31d);
        scoreLabel.setTranslateX(scoreLabelTxt.getTranslateX() + 75);
        scoreLabel.setFont(new Font(24));

        pane.getChildren().add(scoreLabelTxt);
        pane.getChildren().add(scoreLabel);
    }

    /**
     * Draws all relevant items - table, cue, balls, pockets - onto Canvas.
     * Used Exercise 6 as reference.
     */
    private void draw() {
        tick();

        // Fill in background
        gc.setFill(Paint.valueOf("white"));
        gc.fillRect(0, 0, table.getxLength() + TABLEBUFFER * 2, table.getyLength() + TABLEBUFFER * 2);

        // Fill in edges
        gc.setFill(Paint.valueOf("brown"));
        gc.fillRect(TABLEBUFFER - TABLEEDGE, TABLEBUFFER - TABLEEDGE, table.getxLength() + TABLEEDGE * 2,
                table.getyLength() + TABLEEDGE * 2);

        // Fill in Table
        gc.setFill(table.getColour());
        gc.fillRect(TABLEBUFFER, TABLEBUFFER, table.getxLength(), table.getyLength());

        // Fill in Pockets
        for (Pocket pocket : table.getPockets()) {
            gc.setFill(Paint.valueOf("black"));
            gc.fillOval(pocket.getxPos() - pocket.getRadius(), pocket.getyPos() - pocket.getRadius(),
                    pocket.getRadius() * 2, pocket.getRadius() * 2);
        }

//        // Cue
//        if (this.cue != null && cueActive) {
//            gc.strokeLine(cue.getStartX(), cue.getStartY(), cue.getEndX(), cue.getEndY());
//        }

        for (Ball ball : balls) {
            if (ball.isActive() && !ball.isCue()) {
                gc.setFill(ball.getColour());
                gc.fillOval(ball.getxPos() - ball.getRadius(),
                        ball.getyPos() - ball.getRadius(),
                        ball.getRadius() * 2,
                        ball.getRadius() * 2);
            }

        }

        cueBallDraw.setRadius(cueBall.getRadius());
        cueBallDraw.setFill(cueBall.getColour());
        cueBallDraw.setCenterX(cueBall.getxPos());
        cueBallDraw.setCenterY(cueBall.getyPos());

        if(!ballsStationary())
            cue.setFill(Color.TRANSPARENT);
        else{
            cue.setFill(Color.SANDYBROWN);
        }

        cue.setTranslateY(cueSizeY/2 + cueBall.getRadius() + hitStrength/2);
        cuePivot.translateXProperty().set(cueBall.getxPos() - cueSizeX/2);
        cuePivot.translateYProperty().set(cueBall.getyPos() - cueSizeY/2);


        // Win
        if (winFlag) {
            gc.setStroke(Paint.valueOf("white"));
            gc.setFont(new Font("Impact", 80));
            gc.strokeText("Win and bye", table.getxLength() / 2 + TABLEBUFFER - 180,
                    table.getyLength() / 2 + TABLEBUFFER);
        }
    }

    // As this feature was already implemented by me previously in assignment 2
    // the code for this feature is re-used from assignment 2 codebase
    // substituting original cue implementation
    // Start of re-used code here
    private void moveCue(){
        cueBallDraw.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if(ballsStationary()) {

                    double xDir = cueBall.getxPos() - t.getX();
                    double yDir = cueBall.getyPos() - t.getY();

                    // Due to cue being instantiated vertically
                    // and how atan2 functions, the angle is off by 90 degrees
                    // thus 90 degrees added to calculated angle
                    cuePivot.setRotate(Math.toDegrees(atan2(yDir, xDir)) + 90);

                    double xDistance = Math.pow((cueBall.getxPos() - t.getX()), 2);
                    double yDistance = Math.pow((cueBall.getyPos() - t.getY()), 2);
                    double distance = Math.sqrt(xDistance + yDistance);


                    if(distance > 160 || distance < -160){
                        hitStrength = 160;
                    }
                    else
                        hitStrength = distance;

                    double deltaX = cueBall.getxPos() - cueLine.getStartX();
                    double deltaY = cueBall.getyPos() - cueLine.getStartY();
                    double distanceCueLine = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                    // Check that start of cue is within cue ball
                    if (distanceCueLine < cueBall.getRadius()) {
                        // Collide ball with cue
                        double hitxVel = (cueLine.getStartX() - cueLine.getEndX()) * FORCEFACTOR;
                        double hityVel = (cueLine.getStartY() - cueLine.getEndY()) * FORCEFACTOR;


                        hitStrengthX = hitxVel;
                        hitStrengthY = hityVel;
                    }
                }
            }
        });
    }
    // End of re-used code

    /**
     * Hits the ball with the cue, distance of the cue indicates the strength of the
     * strike.
     *
     * ball
     */
    private void hitBall() {
        cueBallDraw.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
                if(ballsStationary()) {
                    cueBall.setxVel(hitStrengthX);
                    cueBall.setyVel(hitStrengthY);

                    hitStrengthX = 0;
                    hitStrengthY = 0;
                    hitStrength = 0;
                }
            }
        });

        cueBallDraw.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveMemento();
            }
        });
    }

    /**
     * Updates positions of all balls, handles logic related to collisions.
     * Used Exercise 6 as reference.
     */
    public void tick() {
        if(!winFlag) {
            elapsedTime = System.nanoTime() - startTime;
            double elapsedTimeSeconds = (double) elapsedTime / 1000000000;

            if(elapsedTimeSeconds >= 1) { seconds++; startTime = System.nanoTime(); }

            if (seconds >= 60) {
                seconds = 0;
                elapsedMinutes++;
            }
            elapsedTimeLabel.setText(String.format("%01.0f:%02.0f", elapsedMinutes, seconds));
        }

//        if (score == balls.size() - 1) {
//            winFlag = true;
//        }

        for (Ball ball : balls) {
            ball.tick();

//            if (ball.isCue() && cueSet) {
////                hitBall(ball);
//            }

            double width = table.getxLength();
            double height = table.getyLength();

            // Check if ball landed in pocket
            for (Pocket pocket : table.getPockets()) {
                if (pocket.isInPocket(ball)) {
                    if (ball.isCue()) {
                        this.reset();
                        winFlag = false;
                    } else {
                        if (ball.remove()) {
                            addScore(ball);
                            scoreLabel.setText(String.valueOf(score));
                        } else {
                            // Check if when ball is removed, any ot
                            // her balls are present in its space. (If
                            // another ball is present, blue ball is removed)
                            for (Ball otherBall : balls) {
                                double deltaX = ball.getxPos() - otherBall.getxPos();
                                double deltaY = ball.getyPos() - otherBall.getyPos();
                                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                                if (otherBall != ball && otherBall.isActive() && distance < 10) {
                                    ball.remove();
                                }
                            }
                        }
                    }
                    break;
                }
            }

            // Handle the edges (balls don't get a choice here)
            if (ball.getxPos() + ball.getRadius() > width + TABLEBUFFER) {
                ball.setxPos(width - ball.getRadius());
                ball.setxVel(ball.getxVel() * -1);
            }
            if (ball.getxPos() - ball.getRadius() < TABLEBUFFER) {
                ball.setxPos(ball.getRadius());
                ball.setxVel(ball.getxVel() * -1);
            }
            if (ball.getyPos() + ball.getRadius() > height + TABLEBUFFER) {
                ball.setyPos(height - ball.getRadius());
                ball.setyVel(ball.getyVel() * -1);
            }
            if (ball.getyPos() - ball.getRadius() < TABLEBUFFER) {
                ball.setyPos(ball.getRadius());
                ball.setyVel(ball.getyVel() * -1);
            }

            // Apply table friction
            double friction = table.getFriction();
            ball.setxVel(ball.getxVel() * friction);
            ball.setyVel(ball.getyVel() * friction);

            // Check ball collisions
            for (Ball ballB : balls) {
                if (checkCollision(ball, ballB)) {
                    Point2D ballPos = new Point2D(ball.getxPos(), ball.getyPos());
                    Point2D ballBPos = new Point2D(ballB.getxPos(), ballB.getyPos());
                    Point2D ballVel = new Point2D(ball.getxVel(), ball.getyVel());
                    Point2D ballBVel = new Point2D(ballB.getxVel(), ballB.getyVel());
                    Pair<Point2D, Point2D> changes = calculateCollision(ballPos, ballVel, ball.getMass(), ballBPos,
                            ballBVel, ballB.getMass(), false);
                    calculateChanges(changes, ball, ballB);
                }
            }
        }
    }

    private void addScore(Ball ball) {
        if(ball.getColour().equals(Paint.valueOf("red"))){
            score += 1;
        }
        else if(ball.getColour().equals(Paint.valueOf("yellow"))){
            score += 2;
        }
        else if(ball.getColour().equals(Paint.valueOf("green"))
        || ball.getColour().equals(Paint.valueOf("limegreen"))){
            score += 3;
        }
        if(ball.getColour().equals(Paint.valueOf("brown"))){
            score += 4;
        }
        if(ball.getColour().equals(Paint.valueOf("blue"))){
            score += 5;
        }
        if(ball.getColour().equals(Paint.valueOf("purple"))){
            score += 6;
        }
        if(ball.getColour().equals(Paint.valueOf("black"))){
            score += 7;
        }
        if(ball.getColour().equals(Paint.valueOf("orange"))){
            score += 8;
        }

        for(Ball b: balls){
            if(!b.isCue() && b.isActive()){
                return;
            }
        }

        winFlag = true;
    }

    /**
     * Resets the game.
     */
    public void reset() {
        for (Ball ball : balls) {
            ball.reset();
        }

        this.score = 0;
        this.scoreLabel.setText(String.valueOf(score));
        this.seconds = 0;
        this.elapsedMinutes = 0;
        this.startTime = System.nanoTime();
    }

    /**
     * @return scene.
     */
    public Scene getScene() {
        return this.scene;
    }

    /**
     * Sets the table of the game.
     *
     * @param table
     */
    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * @return table
     */
    public Table getTable() {
        return this.table;
    }

    /**
     * Sets the balls of the game.
     *
     * @param balls
     */
    public void setBalls(ArrayList<Ball> balls) {
        this.balls = balls;
    }

    /**
     * Changes values of balls based on collision (if ball is null ignore it)
     *
     * @param changes
     * @param ballA
     * @param ballB
     */
    private void calculateChanges(Pair<Point2D, Point2D> changes, Ball ballA, Ball ballB) {
        ballA.setxVel(changes.getKey().getX());
        ballA.setyVel(changes.getKey().getY());
        if (ballB != null) {
            ballB.setxVel(changes.getValue().getX());
            ballB.setyVel(changes.getValue().getY());
        }
    }

    /**
     * Sets the cue to be drawn on click, and manages cue actions
     *
     * @param pane
     */
    private void setClickEvents(Pane pane) {
        pane.setOnMousePressed(event -> {
            cueLine = new Line(event.getX(), event.getY(), event.getX(), event.getY());
            cueSet = false;
            cueActive = true;
        });

        pane.setOnMouseDragged(event -> {
            cueLine.setEndX(event.getX());
            cueLine.setEndY(event.getY());
        });

        pane.setOnMouseReleased(event -> {
            cueSet = true;
            cueActive = false;
        });
    }

    /**
     * Checks if two balls are colliding.
     * Used Exercise 6 as reference.
     *
     * @param ballA
     * @param ballB
     * @return true if colliding, false otherwise
     */
    private boolean checkCollision(Ball ballA, Ball ballB) {
        if (ballA == ballB) {
            return false;
        }

        return Math.abs(ballA.getxPos() - ballB.getxPos()) < ballA.getRadius() + ballB.getRadius() &&
                Math.abs(ballA.getyPos() - ballB.getyPos()) < ballA.getRadius() + ballB.getRadius();
    }

    /**
     * Collision function adapted from assignment, using physics algorithm:
     * http://www.gamasutra.com/view/feature/3015/pool_hall_lessons_fast_accurate_.php?page=3
     *
     * @param positionA The coordinates of the centre of ball A
     * @param velocityA The delta x,y vector of ball A (how much it moves per tick)
     * @param massA     The mass of ball A (for the moment this should always be the
     *                  same as ball B)
     * @param positionB The coordinates of the centre of ball B
     * @param velocityB The delta x,y vector of ball B (how much it moves per tick)
     * @param massB     The mass of ball B (for the moment this should always be the
     *                  same as ball A)
     *
     * @return A Pair in which the first (key) Point2D is the new
     *         delta x,y vector for ball A, and the second (value) Point2D is the
     *         new delta x,y vector for ball B.
     */
    public static Pair<Point2D, Point2D> calculateCollision(Point2D positionA, Point2D velocityA, double massA,
            Point2D positionB, Point2D velocityB, double massB, boolean isCue) {

        // Find the angle of the collision - basically where is ball B relative to ball
        // A. We aren't concerned with
        // distance here, so we reduce it to unit (1) size with normalize() - this
        // allows for arbitrary radii
        Point2D collisionVector = positionA.subtract(positionB);
        collisionVector = collisionVector.normalize();

        // Here we determine how 'direct' or 'glancing' the collision was for each ball
        double vA = collisionVector.dotProduct(velocityA);
        double vB = collisionVector.dotProduct(velocityB);

        // If you don't detect the collision at just the right time, balls might collide
        // again before they leave
        // each others' collision detection area, and bounce twice.
        // This stops these secondary collisions by detecting
        // whether a ball has already begun moving away from its pair, and returns the
        // original velocities
        if (vB <= 0 && vA >= 0 && !isCue) {
            return new Pair<>(velocityA, velocityB);
        }

        // This is the optimisation function described in the gamasutra link. Rather
        // than handling the full quadratic
        // (which as we have discovered allowed for sneaky typos)
        // this is a much simpler - and faster - way of obtaining the same results.
        double optimizedP = (2.0 * (vA - vB)) / (massA + massB);

        // Now we apply that calculated function to the pair of balls to obtain their
        // final velocities
        Point2D velAPrime = velocityA.subtract(collisionVector.multiply(optimizedP).multiply(massB));
        Point2D velBPrime = velocityB.add(collisionVector.multiply(optimizedP).multiply(massA));

        return new Pair<>(velAPrime, velBPrime);
    }

    public void firstDifficultySelection(){

        GridPane difficultySelecting = new GridPane();
        difficultySelecting.setAlignment(Pos.TOP_CENTER);
        difficultySelecting.setVgap(10);
        difficultySelecting.setHgap(10);
        difficultySelecting.setPadding(new Insets(10, 10, 10, 10));

        double buttonXSize = 25d;
        double buttonYSize = 25d;

        Button originalDiffBtn = new Button("Original");
        GridPane.setHalignment(originalDiffBtn, HPos.CENTER);
        originalDiffBtn.setMinSize(buttonXSize, buttonYSize);

        Button easyDiffBtn = new Button("Easy");
        GridPane.setHalignment(easyDiffBtn, HPos.CENTER);
        easyDiffBtn.setMaxWidth(Double.MAX_VALUE);
        easyDiffBtn.setMinSize(buttonXSize, buttonYSize);

        Button normalDiffBtn = new Button("Normal");
        GridPane.setHalignment(normalDiffBtn, HPos.CENTER);
        normalDiffBtn.setMinSize(buttonXSize, buttonYSize);

        Button hardDiffBtn = new Button("Hard");
        GridPane.setHalignment(hardDiffBtn, HPos.CENTER);
        hardDiffBtn.setMaxWidth(Double.MAX_VALUE);
        hardDiffBtn.setMinSize(buttonXSize, buttonYSize);

        difficultySelecting.add(originalDiffBtn, 0, 1);
        difficultySelecting.add(easyDiffBtn, 0, 2);
        difficultySelecting.add(normalDiffBtn, 0, 3);
        difficultySelecting.add(hardDiffBtn, 0, 4);

        Stage difficultyDialogue = new Stage();
        difficultyDialogue.initModality(Modality.APPLICATION_MODAL);
        difficultyDialogue.initStyle(StageStyle.UTILITY);
        difficultyDialogue.setAlwaysOnTop(true);

        difficultyDialogue.setTitle("Game Difficulty");
        Scene menuScene = new Scene(difficultySelecting, 140, 170, Color.GRAY);
        difficultyDialogue.setScene(menuScene);
        difficultyDialogue.show();

        originalDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Original Difficulty Selected");
                difficulty = 0;
                App.setStage(new Stage());
                App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));
                difficultyDialogue.close();

                seconds = 0;
                startTime = System.nanoTime();
            }
        });

        easyDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Easy Difficulty Selected");
                difficulty = 1;
                App.setStage(new Stage());
                App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));
                difficultyDialogue.close();
            }
        });

        normalDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Normal Difficulty Selected");
                difficulty = 2;
                App.setStage(new Stage());
                App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));
                difficultyDialogue.close();
            }
        });

        hardDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Hard Difficulty Selected");
                difficulty = 3;
                App.setStage(new Stage());
                App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));
                difficultyDialogue.close();
            }
        });
    }

    private void resetTimer() {
        seconds = 0;
        elapsedMinutes = 0;
        startTime = System.nanoTime();
        elapsedTimeLabel.setText(String.valueOf(seconds));
    }


    private void handleDifficultySelection(GridPane menuGrid, Button changeDifficultyBtn, Stage menuDialogue, Scene menuScene) {

        changeDifficultyBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                GridPane difficultySelecting = new GridPane();
                difficultySelecting.setAlignment(Pos.TOP_CENTER);
                difficultySelecting.setVgap(10);
                difficultySelecting.setHgap(10);
                difficultySelecting.setPadding(new Insets(10, 10, 10, 10));

                double buttonXSize = 25d;
                double buttonYSize = 25d;

                Button originalDiffBtn = new Button("Original");
                GridPane.setHalignment(originalDiffBtn, HPos.CENTER);
                originalDiffBtn.setMinSize(buttonXSize, buttonYSize);

                originalDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        System.out.println("Original Difficulty Selected");
                        menuScene.setRoot(menuGrid);
                        menuDialogue.hide();

                        System.out.println("Original Difficulty Selected");
                        difficulty = 0;
                        App.getPrimaryStage().close();
                        App.setStage(new Stage());
                        App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));

                        reset();
                        winFlag = false;
                        resetTimer();
                    }
                });

                Button easyDiffBtn = new Button("Easy");
                GridPane.setHalignment(easyDiffBtn, HPos.CENTER);
                easyDiffBtn.setMaxWidth(Double.MAX_VALUE);
                easyDiffBtn.setMinSize(buttonXSize, buttonYSize);

                easyDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        System.out.println("Easy Difficulty Selected");
                        menuScene.setRoot(menuGrid);
                        menuDialogue.hide();

                        difficulty = 1;
                        App.getPrimaryStage().close();
                        App.setStage(new Stage());
                        App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));

                        reset();
                        winFlag = false;
                        resetTimer();
                    }
                });

                Button normalDiffBtn = new Button("Normal");
                GridPane.setHalignment(normalDiffBtn, HPos.CENTER);
                normalDiffBtn.setMinSize(buttonXSize, buttonYSize);

                normalDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        System.out.println("Normal Difficulty Selected");
                        menuScene.setRoot(menuGrid);
                        menuDialogue.hide();

                        difficulty = 2;
                        App.getPrimaryStage().close();
                        App.setStage(new Stage());
                        App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));

                        reset();
                        winFlag = false;
                        resetTimer();
                    }
                });

                Button hardDiffBtn = new Button("Hard");
                GridPane.setHalignment(hardDiffBtn, HPos.CENTER);
                hardDiffBtn.setMaxWidth(Double.MAX_VALUE);
                hardDiffBtn.setMinSize(buttonXSize, buttonYSize);

                hardDiffBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        System.out.println("Hard Difficulty Selected");
                        menuScene.setRoot(menuGrid);
                        menuDialogue.hide();

                        difficulty = 3;
                        App.getPrimaryStage().close();
                        App.setStage(new Stage());
                        App.startGame(App.getPrimaryStage(), App.checkConfig(difficulty));

                        reset();
                        winFlag = false;
                        resetTimer();
                    }
                });

                Button emptyBtn = new Button();
                emptyBtn.setVisible(false);

                Button returnButton = new Button("Return");
                GridPane.setHalignment(hardDiffBtn, HPos.CENTER);
                returnButton.setMaxWidth(Double.MAX_VALUE);
                returnButton.setMinSize(buttonXSize, buttonYSize);

                returnButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        menuScene.setRoot(menuGrid);
                    }
                });

                difficultySelecting.add(originalDiffBtn, 0, 1);
                difficultySelecting.add(easyDiffBtn, 0, 2);
                difficultySelecting.add(normalDiffBtn, 0, 3);
                difficultySelecting.add(hardDiffBtn, 0, 4);
                difficultySelecting.add(emptyBtn, 0, 5);
                difficultySelecting.add(returnButton, 0, 20);

                menuScene.setRoot(difficultySelecting);
            }
        });
    }

    private void handleCheatMenu(GridPane menuGrid, Button cheatBtn, Scene menuScene) {

        cheatBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                GridPane cheatsSelecting = new GridPane();
                cheatsSelecting.setAlignment(Pos.TOP_CENTER);
                cheatsSelecting.setVgap(10);
                cheatsSelecting.setHgap(10);
                cheatsSelecting.setPadding(new Insets(10, 10, 10, 10));

                Button removeRedBtn = new Button("Remove Red");
                GridPane.setHalignment(removeRedBtn, HPos.CENTER);
                removeRedBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("red");
                        removeRedBtn.setDisable(true);
                    }
                });

                Button removeYellowBtn = new Button("Remove Yellow");
                GridPane.setHalignment(removeYellowBtn, HPos.CENTER);
                removeYellowBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("yellow");
                        removeYellowBtn.setDisable(true);
                    }
                });

                Button removeGreenBtn = new Button("Remove Green");
                GridPane.setHalignment(removeGreenBtn, HPos.CENTER);
                removeGreenBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("limegreen");
                        removeBallFromTable("green");
                        removeGreenBtn.setDisable(true);
                    }
                });

                Button removeBrownBtn = new Button("Remove Brown");
                GridPane.setHalignment(removeBrownBtn, HPos.CENTER);
                removeBrownBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("brown");
                        removeBrownBtn.setDisable(true);
                    }
                });


                Button removeBlueBtn = new Button("Remove Blue");
                GridPane.setHalignment(removeBlueBtn, HPos.CENTER);
                removeBlueBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("blue");
                        removeBlueBtn.setDisable(true);
                    }
                });

                Button removePurpleBtn = new Button("Remove Purple");
                GridPane.setHalignment(removePurpleBtn, HPos.CENTER);
                removePurpleBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("purple");
                        removePurpleBtn.setDisable(true);
                    }
                });

                Button removeBlackBtn = new Button("Remove Black");
                GridPane.setHalignment(removeBlackBtn, HPos.CENTER);
                removeBlackBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("black");
                        removeBlackBtn.setDisable(true);
                    }
                });

                Button removeOrangeBtn = new Button("Remove Orange");
                GridPane.setHalignment(removeOrangeBtn, HPos.CENTER);
                removeOrangeBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        saveMemento();
                        removeBallFromTable("orange");
                        removeOrangeBtn.setDisable(true);
                    }
                });

                Button emptyBtn = new Button();
                emptyBtn.setVisible(false);

                Button returnBtn = new Button("Return");
                GridPane.setHalignment(returnBtn, HPos.CENTER);

                returnBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        menuScene.setRoot(menuGrid);
                    }
                });

                disableRemoveButton(removeRedBtn, removeYellowBtn,
                        removeGreenBtn, removeBrownBtn,
                        removeBlueBtn, removeBlackBtn,
                        removeOrangeBtn, removePurpleBtn);

                removeRedBtn.setMaxWidth(Double.MAX_VALUE);
                removeRedBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removeYellowBtn.setMaxWidth(Double.MAX_VALUE);
                removeYellowBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removeGreenBtn.setMaxWidth(Double.MAX_VALUE);
                removeGreenBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removeBrownBtn.setMaxWidth(Double.MAX_VALUE);
                removeBrownBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removeBlueBtn.setMaxWidth(Double.MAX_VALUE);
                removeBlueBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removePurpleBtn.setMaxWidth(Double.MAX_VALUE);
                removePurpleBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                removeBlackBtn.setMaxWidth(Double.MAX_VALUE);
                removeBlackBtn.setMinWidth(removeOrangeBtn.getMinWidth());

                cheatsSelecting.add(removeRedBtn, 0, 1);
                cheatsSelecting.add(removeYellowBtn, 0, 2);
                cheatsSelecting.add(removeGreenBtn, 0, 3);
                cheatsSelecting.add(removeBrownBtn, 0, 4);
                cheatsSelecting.add(removeBlueBtn, 0, 5);
                cheatsSelecting.add(removePurpleBtn, 0, 6);
                cheatsSelecting.add(removeBlackBtn, 0, 7);
                cheatsSelecting.add(removeOrangeBtn, 0, 8);
                cheatsSelecting.add(emptyBtn, 0, 9);
                cheatsSelecting.add(returnBtn, 0, 10);


                menuScene.setRoot(cheatsSelecting);
            }
        });
    }

    private void disableRemoveButton(Button removeRedBtn, Button removeYellowBtn,
                                     Button removeGreenBtn, Button removeBrownBtn,
                                     Button removeBlueBtn, Button removeBlackBtn,
                                     Button removeOrangeBtn, Button removePurpleBtn) {
        if(!isColouredBallOnTable("red"))
            removeRedBtn.setDisable(true);
        else
            removeRedBtn.setDisable(false);

        if(!isColouredBallOnTable("brown"))
            removeBrownBtn.setDisable(true);
        else
            removeBrownBtn.setDisable(false);

        if(!isColouredBallOnTable("blue"))
            removeBlueBtn.setDisable(true);
        else
            removeBlueBtn.setDisable(false);

        if(!isColouredBallOnTable("yellow"))
            removeYellowBtn.setDisable(true);
        else
            removeYellowBtn.setDisable(false);

        if(!isColouredBallOnTable("orange"))
            removeOrangeBtn.setDisable(true);
        else
            removeOrangeBtn.setDisable(false);

        if(!isColouredBallOnTable("green") && !isColouredBallOnTable("limegreen"))
            removeGreenBtn.setDisable(true);
        else
            removeGreenBtn.setDisable(false);

        if(!isColouredBallOnTable("black"))
            removeBlackBtn.setDisable(true);
        else
            removeBlackBtn.setDisable(false);

        if(!isColouredBallOnTable("purple"))
            removePurpleBtn.setDisable(true);
        else
            removePurpleBtn.setDisable(false);
    }

    private void removeBallFromTable(String colour) {
        for(Ball ball: balls){
            if(ball.getColour().equals(Paint.valueOf(colour))){
                if(ball.isActive()) {
                    ball.setActive(false);
                    addScore(ball);
                    scoreLabel.setText(String.valueOf(score));
                }
            }
        }
    }

    private boolean isColouredBallOnTable(String colour){
        for(Ball ball: balls){
            if(ball.getColour().equals(Paint.valueOf(colour)) && ball.isActive()){
                return true;
            }
        }
        return false;
    }

    private boolean ballsStationary(){
        for(Ball ball: balls){
            if(ball.getxVel() >= 0.01d || ball.getxVel() <= -0.01d){
                if(ball.getyVel() >= 0.01d || ball.getyVel() <= -0.01d) {
                    return false;
                }
            }
        }

        return true;
    }

    private void saveMemento() {
//        System.out.println("Memento Saved");
        undoButton.setDisable(false);
        ArrayList<BallMemento> ballMementos = new ArrayList<>();

        for(Ball ball: balls){
            PocketStrategy cloneStrategy = ball.getStrategy().clone();
            BallMemento ballMemento = new BallMemento(ball.getxPos(), ball.getyPos(), ball.isActive(), cloneStrategy);
            ballMementos.add(ballMemento);
        }

        SystemMemento memento = new SystemMemento.Builder()
                .setBalls(ballMementos)
                .setWinFlag(winFlag)
                .setScore(score)
                .setElapsedSeconds(seconds)
                .setElapsedMinutes(elapsedMinutes)
                .buildObject();


        mementoCaretaker.saveMemento(memento);
    }

    private void loadMemento(){
//        System.out.println("Memento loaded");
        SystemMemento memento = mementoCaretaker.loadLastMemento();


        int i = 0;
        for(Ball tableBall: balls){
            tableBall.setxVel(0);
            tableBall.setyVel(0);

            // Weird bug for some reason setting the position increases it by 50
            // No idea why so I just subtract 50 and it works fine
            // Couldnt get to the bottom of this
            tableBall.setxPos(memento.getBallMementos().get(i).getxPos()-50);
            tableBall.setyPos(memento.getBallMementos().get(i).getyPos()-50);
            tableBall.setActive(memento.getBallMementos().get(i).getIsActive());
            tableBall.setStrategy(memento.getBallMementos().get(i).getBallStrategy());

            i++;
            if(i == balls.size()){ break; }
        }

        winFlag = memento.getWinFlag();

        score = memento.getScore();
        scoreLabel.setText((String.valueOf(score)));

        seconds = memento.getElapsedSeconds();
        elapsedTimeLabel.setText(String.valueOf(seconds));

        elapsedMinutes = memento.getElapsedMinutes();

        startTime = System.nanoTime();
    }
}
