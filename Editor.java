import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import editor.TextBuffer;
import editor.Render;
import editor.KeyEventHandler;
import editor.MouseEventHandler;

public class Editor extends Application {
    private static String fileName;

    private static int WINDOW_WIDTH = 500;
    private static int WINDOW_HEIGHT = 500;

    private TextBuffer text;
    private Render renderLayout;
    private KeyEventHandler keyEventHandler;
    private MouseEventHandler mouseEventHandler;

    private Group root;
    private Group textRoot;

    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        root = new Group();
        // The Scene represents the window: its height and width will be the height and width of the window displayed.
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, Color.WHITE);

        textRoot = new Group();
        root.getChildren().add(textRoot);
        text = new TextBuffer();
        renderLayout = new Render(root, textRoot, text, WINDOW_WIDTH, WINDOW_HEIGHT);
        openFile(fileName);
        // To get information about what keys the user is pressing, create an EventHandler.
        keyEventHandler = new KeyEventHandler(textRoot, text, renderLayout, fileName);
        mouseEventHandler = new MouseEventHandler(textRoot, text, renderLayout);

        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnKeyReleased(keyEventHandler);
        scene.setOnMouseClicked(mouseEventHandler);

        // Adjust the window size
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldSceneWidth, Number newSceneWidth) {
                renderLayout.updateWindowWidth((double) newSceneWidth);
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldSceneHeight, Number newSceneHeight) {
                renderLayout.updateWindowHeight((double) newSceneHeight);
            }
        });


        primaryStage.setTitle("Editor");

        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void openFile(String inputFilename) {
        try {
            File inputFile = new File(inputFilename);
            if (!inputFile.exists()) {
                inputFile.createNewFile();
            } else {
                FileReader reader = new FileReader(inputFile);
                BufferedReader bufferedReader = new BufferedReader(reader);

                int intRead = -1;
                // Keep reading from the file input read() returns -1.
                while ((intRead = bufferedReader.read()) != -1) {
                    char charRead = (char) intRead;
                    String stringChar = Character.toString(charRead);
                    Text inputText = new Text(stringChar);
                    text.add(inputText);
                    textRoot.getChildren().add(text.getCurrentPos(), inputText);
                    text.moveRight();
                }
                renderLayout.renderAll();
                System.out.println("Successfully opened file " + inputFilename);
                // Close the reader.
                bufferedReader.close();
                reader.close();
            }
        } catch (IOException ioException) {
            System.out.println("Error when editing; exception was: " + ioException);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected usage: Editor <file path>");
            System.exit(1);
        }
        fileName = args[0];
        launch(args);
    }
}
