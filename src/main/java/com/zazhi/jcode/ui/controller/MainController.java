package com.zazhi.jcode.ui.controller;

import com.zazhi.jcode.Agent;
import com.zazhi.jcode.ui.chat.ChatMessageCell;
import com.zazhi.jcode.ui.enums.MessageRole;
import com.zazhi.jcode.ui.records.ChatMessage;
import com.zazhi.jcode.ui.settings.LlmSettingsDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    @FXML
    private ListView<ChatMessage> chatListView;

    private final ObservableList<ChatMessage> messages =
            FXCollections.observableArrayList();

    @FXML
    private TextField inputTextField;

    @FXML
    private Button sendButton;

    @FXML
    private MenuItem llmSettingsMenuItem;

    private final ExecutorService agentExecutor =
            Executors.newSingleThreadExecutor();

    private final Agent agent = new Agent();

    @FXML
    private void initialize() {
        messages.add(new ChatMessage(
                MessageRole.ASSISTANT,
                "你好，我是 **JCode** AI 助手。有什么可以帮你的吗？"
        ));

        chatListView.setItems(messages);
        chatListView.setCellFactory(listView -> new ChatMessageCell());

        sendButton.setOnAction(event -> sendMessage());
        inputTextField.setOnAction(event -> sendMessage());
        llmSettingsMenuItem.setOnAction(event ->
                LlmSettingsDialog.show(sendButton.getScene().getWindow())
        );
    }

    private void sendMessage() {
        String input = inputTextField.getText().trim();

        if (input.isEmpty()) {
            return;
        }

        messages.add(new ChatMessage(MessageRole.USER, input));
        inputTextField.clear();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return agent.query(input);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, agentExecutor)
                .whenComplete((response, error) ->
                        Platform.runLater(() -> {
                            if (error != null) {
                                Throwable root = error;
                                while (root.getCause() != null) {
                                    root = root.getCause();
                                }
                                messages.add(new ChatMessage(
                                        MessageRole.ERROR,
                                        root.getMessage() == null
                                                ? error.getMessage()
                                                : root.getMessage()
                                ));
                            } else {
                                messages.add(new ChatMessage(
                                        MessageRole.ASSISTANT,
                                        response
                                ));
                            }

                            chatListView.scrollTo(messages.size() - 1);
                        })
                );
    }
}
