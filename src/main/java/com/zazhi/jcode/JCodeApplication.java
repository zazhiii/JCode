package com.zazhi.jcode;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
public class JCodeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Agent agent = new Agent();

        Pane root = (Pane)FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));

        stage.setScene(new Scene(root, 1200, 720));
        stage.setTitle("JCode");
        stage.show();
    }
}
