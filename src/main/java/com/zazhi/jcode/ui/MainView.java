package com.zazhi.jcode.ui;

import com.zazhi.jcode.ui.records.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;


/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class MainView extends BorderPane {
    private TextArea inputTextArea;
    private Button sendButton;
    private ListView<ChatMessage> chatListView;

    public MainView() {
        inputTextArea = new TextArea();
        inputTextArea.setPromptText("给JCode发送消息");
        inputTextArea.setWrapText(true);
        sendButton = new Button("发送");
        HBox bottomHBox = new HBox(inputTextArea, sendButton);
        bottomHBox.setBorder(new Border(new BorderStroke(Paint.valueOf("#000000"), BorderStrokeStyle.SOLID,
                new CornerRadii(1), new BorderWidths(1))));
        bottomHBox.setAlignment(Pos.CENTER);
        setBottom(bottomHBox);
//        setAlignment(bottomHBox, Pos.CENTER);

        MenuBar menuBar = new MenuBar(new Menu("设置"));
        setTop(menuBar);

        VBox leftVBox = new VBox();
        leftVBox.setBorder(new Border(new BorderStroke(Paint.valueOf("#000000"), BorderStrokeStyle.SOLID,
                new CornerRadii(1), new BorderWidths(1))));
        leftVBox.setPrefWidth(200);
        setLeft(leftVBox);

        VBox rightVBox = new VBox();
        rightVBox.setBorder(new Border(new BorderStroke(Paint.valueOf("#000000"), BorderStrokeStyle.SOLID,
                new CornerRadii(1), new BorderWidths(1))));
        rightVBox.setPrefWidth(200);
        setRight(rightVBox);

        chatListView = new ListView<>();
        setCenter(chatListView);
    }

    public TextArea getInputTextArea() {
        return inputTextArea;
    }

    public Button getSendButton() {
        return sendButton;
    }

    public ListView<ChatMessage> getChatListView() {
        return chatListView;
    }
}
