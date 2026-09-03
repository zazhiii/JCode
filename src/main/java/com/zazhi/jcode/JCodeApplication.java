package com.zazhi.jcode;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author zazhi
 * @date 2026/8/18
 */
public class JCodeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Agent agent = new Agent();

        Pane pane = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));

//        BorderPane borderPane = new BorderPane();
//
//        List<String> messages = new ArrayList<>();
//
//        VBox messageBox = new VBox();
//
//        HBox inputHbox = new HBox();
//        TextField textField = new TextField();
//        inputHbox.getChildren().add(textField);
//        inputHbox.setAlignment(Pos.CENTER);
//        Button submitButton = new Button("Submit");
//        submitButton.setOnAction(e -> {
//            String text = textField.getText();
//            if(text.trim().equals("")) return;
//            messageBox.getChildren().add(new Label("me: " + text));
//            textField.clear();
//
//            // TODO: 这里应该异步提交，不然会阻塞ui操作
//            String resp;
//            try {
//                 resp = agent.query(text);
//            } catch (Exception ex) {
//                throw new RuntimeException(ex);
//            }
//            messageBox.getChildren().add(new Label("agent: " + resp));
//        });
//        inputHbox.getChildren().add(submitButton);
//
//        borderPane.setBottom(inputHbox);
//        borderPane.setCenter(messageBox);

        stage.setScene(new Scene(pane, 900, 600));
        stage.setTitle("JCode");
        stage.show();
    }
}
