package com.zazhi.jcode.ui.settings;

import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;

/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class GeneralSettingsView extends Pane {
    public GeneralSettingsView() {
        setBackground(new Background(new BackgroundFill(Paint.valueOf("pink"), null, null)));
    }
}
