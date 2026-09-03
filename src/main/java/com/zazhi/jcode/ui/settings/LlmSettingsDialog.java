package com.zazhi.jcode.ui.settings;

import com.zazhi.jcode.Config;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * LLM 配置对话框，写入 {@code ~/.jcode/config.toml}。
 */
public final class LlmSettingsDialog {

    private LlmSettingsDialog() {
    }

    public static void show(Window owner) {
        Config current = Config.load();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("LLM 配置");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField baseUrlField = new TextField(current.getBaseUrl());
        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setText(current.getApiKey());
        TextField modelIdField = new TextField(current.getModelId());

        baseUrlField.setPrefColumnCount(36);
        apiKeyField.setPrefColumnCount(36);
        modelIdField.setPrefColumnCount(36);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("llm.base_url"), 0, 0);
        grid.add(baseUrlField, 1, 0);
        grid.add(new Label("llm.api_key"), 0, 1);
        grid.add(apiKeyField, 1, 1);
        grid.add(new Label("llm.model_id"), 0, 2);
        grid.add(modelIdField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> button);
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveButtonType) {
                return;
            }
            try {
                new Config(
                        baseUrlField.getText(),
                        apiKeyField.getText(),
                        modelIdField.getText()
                ).save();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initOwner(owner);
                alert.setTitle("已保存");
                alert.setHeaderText(null);
                alert.setContentText("配置已更新。");
                alert.showAndWait();
            } catch (RuntimeException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(owner);
                alert.setTitle("保存失败");
                alert.setHeaderText(null);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
    }
}
