package com.zazhi.jcode.ui.settings;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;


/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class ModelSettingsView extends BorderPane {
    public ModelSettingsView() {
        Button backButton = new Button("返回工作区");
        HBox topHBox = new HBox(backButton);
        topHBox.setAlignment(Pos.CENTER_LEFT);

        Scene scene = getScene();
        backButton.on

    }
}
