package PoolGame;

import PoolGame.config.*;

import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

/** Main application entry point. */
public class App extends Application {
    private static Stage primaryStage;
    public static void setStage(Stage stage){
        primaryStage = stage;
    }
    public static Stage getPrimaryStage() { return primaryStage; }

    /**
     * @param args First argument is the path to the config file
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    /**
     * Starts the application.
     * 
     * @param primaryStage The primary stage for the application.
     */
    public void start(Stage primaryStage) {
        GameManager.getInstance().firstDifficultySelection();
    }

    public static void startGame(Stage primaryStage, String configPath) {
        ReaderFactory tableFactory = new TableReaderFactory();
        Reader tableReader = tableFactory.buildReader();
        tableReader.parse(configPath, GameManager.getInstance());

        ReaderFactory ballFactory = new BallReaderFactory();
        Reader ballReader = ballFactory.buildReader();
        ballReader.parse(configPath, GameManager.getInstance());
        GameManager.getInstance().buildManager();

        // START GAME MANAGER
        GameManager.getInstance().run();
        primaryStage.setTitle("Pool");
        primaryStage.setScene(GameManager.getInstance().getScene());
        primaryStage.show();

        GameManager.getInstance().run();
    }

    /**
     * Checks if the config file path is given as an argument.
     * 
     * args
     * @return config path.
     */
    public static String checkConfig(int difficulty) {
        String configPath = "";

        switch (difficulty){
            case 0:
                configPath = "./src/main/resources/config.json";
                break;
            case 1:
                configPath = "./src/main/resources/config_easy.json";
                break;
            case 2:
                configPath = "./src/main/resources/config_normal.json";
                break;
            case 3:
                configPath = "./src/main/resources/config_hard.json";
                break;
        }
        return configPath;
    }
}
