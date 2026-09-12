package com.zazhi.gui;


import com.zazhi.core.Agent;
import com.zazhi.core.test.TestMessage;
import com.zazhi.gui.ui.MainView;
import com.zazhi.gui.ui.chat.ChatMessageCell;
import com.zazhi.gui.ui.enums.MessageRole;
import com.zazhi.gui.ui.records.ChatMessage;
import com.zazhi.gui.ui.settings.GeneralSettingsView;
import com.zazhi.gui.ui.settings.ModelSettingsView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zazhi
 * @date 2026/8/18
 * @description:
 */
public class JCodeApplication extends Application {
    private final Agent agent = Agent.getInstance();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final ExecutorService agentExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView();

        // 测试
        messages.add(new ChatMessage(
                MessageRole.USER,
                TestMessage.input
        ));
        messages.add(new ChatMessage(
                MessageRole.ASSISTANT,
                TestMessage.resp
        ));

        mainView.getChatListView().setItems(messages);
        mainView.getChatListView().setCellFactory(listView -> new ChatMessageCell());
        mainView.getSendButton().setOnAction(
                event -> sendMessage(mainView.getInputTextArea(), mainView.getChatListView())
        );

        Scene scene = new Scene(mainView, 1200, 720);

        // 设置菜单跳转
        setMenuItemOnAction(mainView.getGeneralSettingsMenuItem(), scene, new GeneralSettingsView());
        setMenuItemOnAction(mainView.getModelSettingsMenuItem(), scene, new ModelSettingsView());

        stage.setScene(scene);
        stage.setTitle("JCode");
        stage.show();
    }

    private void setMenuItemOnAction(MenuItem menuIteme, Scene scene, Parent root){
        menuIteme.setOnAction(event -> navTo(scene, root));
    }

    private void navTo(Scene scene, Parent root) {
        scene.setRoot(root);
    }

    private void sendMessage(TextArea inputTextArea, ListView<ChatMessage> chatListView) {
        String input = inputTextArea.getText().trim();

        if (input.isEmpty()) {
            return;
        }

        messages.add(new ChatMessage(MessageRole.USER, input));
        inputTextArea.clear();

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
                                        messages.add(new ChatMessage(
                                                MessageRole.ERROR,
                                                error.getMessage()
                                        ));
                                    } else {
                                        messages.add(new ChatMessage(
                                                MessageRole.ASSISTANT,
                                                (String) response
                                        ));
                                    }

//                        setRunning(false);
                                    chatListView.scrollTo(messages.size() - 1);
                                })
                );
    }
}
