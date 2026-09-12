package com.zazhi.gui.ui.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class SettingsView extends BorderPane {

    private final VBox sidebar;
    private final StackPane contentPane;

    public SettingsView() {

        // 左侧导航栏
        sidebar = new VBox(5);
        sidebar.setPrefWidth(220);

        // 右侧内容区域
        contentPane = new StackPane();

        // 左侧按钮
        ToggleGroup toggleGroup = new ToggleGroup();
        ToggleButton generalButton = createNavButton("通用", toggleGroup);
        ToggleButton modelButton = createNavButton("模型", toggleGroup);
//        Button agentButton = new Button("机器人");
//        Button mcpButton = new Button("MCP 与工具");
//        Button sshButton = new Button("远程 SSH");

        sidebar.getChildren().addAll(
                generalButton,
                modelButton
        );

        setLeft(sidebar);
        setCenter(contentPane);

        // 默认页面
        showContent(new ModelSettingsView());

        generalButton.setOnAction(e ->
                showContent(new GeneralSettingsView()));

        modelButton.setOnAction(e ->
                showContent(new ModelSettingsView()));

//        agentButton.setOnAction(e ->
//                showContent(new AgentSettingsView()));
//
//        mcpButton.setOnAction(e ->
//                showContent(new McpSettingsView()));
    }

    private void showContent(Node content) {
        contentPane.getChildren().setAll(content);
    }

    private ToggleButton createNavButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
//        button.getStyleClass().add("settings-nav-item");
        button.setBackground(new Background(new BackgroundFill(Paint.valueOf("transparent"),
                new CornerRadii(8), Insets.EMPTY)));
        button.setTextFill(Paint.valueOf("#ccc"));
        button.setFont(new Font(15));
        button.setPadding(new Insets(12, 20, 12, 20));

        button.setOnMouseEntered(e -> {
            button.setBackground(new Background(new BackgroundFill(Paint.valueOf("#202124"),
                    new CornerRadii(8), Insets.EMPTY)));
        });

        button.selectedProperty().addListener((obs, oldVal, selected) -> {
            if(selected) {
               button.setBackground(new Background(new BackgroundFill(Paint.valueOf("#482821"),
                    new CornerRadii(8), Insets.EMPTY)));
            }
        });
        return button;
    }
}
