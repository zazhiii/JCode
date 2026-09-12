package com.zazhi.gui;


import com.zazhi.core.AgentEngine;
import com.zazhi.core.AgentListener;
import com.zazhi.core.AgentSession;
import com.zazhi.core.Config;
import com.zazhi.core.permission.PermissionDecision;
import com.zazhi.core.permission.PermissionRequest;
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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Path;

/**
 * @author zazhi
 * @date 2026/8/18
 * @description:
 */
public class JCodeApplication extends Application {
    private AgentSession agent;
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final ExecutorService agentExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) {
        Path workspace = Path.of(System.getProperty("user.dir"));
        agent = new AgentEngine(Config.load(workspace)).createSession(
                workspace, this::requestPermission, AgentListener.noop());
        MainView mainView = new MainView();

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
                        return agent.submit(input);
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

    private PermissionDecision requestPermission(PermissionRequest request) {
        CompletableFuture<PermissionDecision> answer = new CompletableFuture<>();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("工具执行确认");
            alert.setHeaderText(request.reason());
            alert.setContentText(request.toolName() + "\n\n" + request.input());
            ButtonType selected = alert.showAndWait().orElse(ButtonType.CANCEL);
            answer.complete(selected == ButtonType.OK
                    ? PermissionDecision.ALLOW
                    : PermissionDecision.DENY);
        });
        return answer.join();
    }

    @Override
    public void stop() {
        agentExecutor.shutdownNow();
    }
}
