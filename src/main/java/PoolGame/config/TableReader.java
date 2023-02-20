package PoolGame.config;

import PoolGame.GameManager;
import PoolGame.objects.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Readers table section of JSON. */
public class TableReader implements Reader {
    /**
     * Parses the JSON file and builds the table.
     * 
     * @param path        The path to the JSON file.
     * @param gameManager The game manager.
     */
    public void parse(String path, GameManager gameManager) {
        JSONParser parser = new JSONParser();
        try {
            Object object = parser.parse(new FileReader(path));

            // convert Object to JSONObject
            JSONObject jsonObject = (JSONObject) object;

            // reading the Table section:
            JSONObject jsonTable = (JSONObject) jsonObject.get("Table");

            // reading a value from the table section
            String tableColour = (String) jsonTable.get("colour");

            // reading a coordinate from the nested section within the table
            // note that the table x and y are of type Long (i.e. they are integers)
            Long tableX = (Long) ((JSONObject) jsonTable.get("size")).get("x");
            Long tableY = (Long) ((JSONObject) jsonTable.get("size")).get("y");

            // getting the friction level.
            // This is a double which should affect the rate at which the balls slow down
            Double tableFriction = (Double) jsonTable.get("friction");

            // Check friction level is between 0 and 1
            if (tableFriction >= 1 || tableFriction <= 0) {
                System.out.println("Friction must be between 0 and 1");
                System.exit(0);
            }

            ArrayList<Double[]> pocketMeasurements = new ArrayList<>();

            JSONArray jsonPockets = (JSONArray) jsonTable.get("pockets");

            for (Object obj : jsonPockets) {
                JSONObject jsonPocket = (JSONObject) obj;

                JSONObject positionJson = (JSONObject) jsonPocket.get("position");
                double xPos = (Double) positionJson.get("x");
                double yPos = (Double) positionJson.get("y");

                double radius = (Double) jsonPocket.get("radius");

                Double[] pocketMeasurement = new Double[3];
                pocketMeasurement[0] = xPos;
                pocketMeasurement[1] = yPos;
                pocketMeasurement[2] = radius;
                pocketMeasurements.add(pocketMeasurement);
            }

            gameManager.setTable(new Table(tableColour, tableX, tableY, tableFriction, pocketMeasurements));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
