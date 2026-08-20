package com.zazhi.jcode;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zazhi
 * @date 2026/8/18
 * @description:
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Agent agent = new Agent();

        BorderPane borderPane = new BorderPane();

        List<String> messages = new ArrayList<>();

        VBox messageBox = new VBox();


        HBox inputHbox = new HBox();
        TextField textField = new TextField();
        inputHbox.getChildren().add(textField);
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            String text = textField.getText();
            if(text.trim().equals("")) return;
//            messages.add("me: " + text);
            messageBox.getChildren().add(new Label("me: " + text));
            textField.clear();

            String resp;
            try {
                 resp = agent.query(text);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
//            messages.add("agent: " + resp);

            messageBox.getChildren().add(new Label("agent: " + resp));

        });
        inputHbox.getChildren().add(submitButton);

        borderPane.setBottom(inputHbox);
        borderPane.setCenter(messageBox);

        stage.setScene(new Scene(borderPane, 300, 300));
        stage.show();
    }
}
