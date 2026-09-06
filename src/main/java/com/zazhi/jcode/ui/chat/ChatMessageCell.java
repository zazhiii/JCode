package com.zazhi.jcode.ui.chat;

import com.zazhi.jcode.ui.enums.MessageRole;
import com.zazhi.jcode.ui.records.ChatMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Renders one chat message in the conversation list.
 *
 * <p>The controls are created once and reused by {@link ListCell}, so the
 * {@code ListView} can recycle this cell efficiently while scrolling.</p>
 */
public final class ChatMessageCell extends ListCell<ChatMessage> {

    private static final double MAX_BUBBLE_WIDTH_RATIO = 0.75;
    private final HBox wrapper = new HBox();
    private final VBox bubble = new VBox(6);
    private final Label contentLabel = new Label();
    private final Button copyButton = new Button("复制");

    public ChatMessageCell() {
        initialiseView();
    }

    private void initialiseView() {
        setText(null);
//        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setBackground(new Background(new BackgroundFill(Paint.valueOf("transparent"), CornerRadii.EMPTY, Insets.EMPTY)));
        setPadding(new Insets(4, 10, 4, 10));

        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(Insets.EMPTY);
        // 容器的期望宽度绑定到单元格的宽度减去24
        wrapper.prefWidthProperty().bind(widthProperty().subtract(24));

        // 左右填满
        bubble.setFillWidth(true);
        // 气泡宽度最多占75%的单元格宽度
        bubble.maxWidthProperty().bind(
                widthProperty().multiply(MAX_BUBBLE_WIDTH_RATIO)
        );
        // 鼠标移入显示复制按钮，移出隐藏
        bubble.setOnMouseEntered(event -> copyButton.setVisible(true));
        bubble.setOnMouseExited(event -> copyButton.setVisible(false));


        contentLabel.setWrapText(true);
        contentLabel.setMinWidth(0);
        contentLabel.setMaxWidth(Double.MAX_VALUE);

        copyButton.setFocusTraversable(false);
        copyButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 1 4 1 4;
                -fx-font-size: 11px;
                -fx-cursor: hand;
                """);
        copyButton.setOnAction(event -> copyCurrentMessage());
        copyButton.setVisible(false);

        HBox actions = new HBox(copyButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().setAll(contentLabel, actions);
        wrapper.getChildren().setAll(bubble);
    }

    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            clearCell();
            return;
        }

        contentLabel.setText(message.content() == null ? "" : message.content());
        contentLabel.setAccessibleText(contentLabel.getText());

        MessageRole role = message.role() == null
                ? MessageRole.ERROR
                : message.role();

        Node graphic = switch (role) {
            case USER -> createUserBubble();
            case ASSISTANT -> createAssistantBubble();
            case SYSTEM -> createSystemBubble();
            case ERROR -> createErrorBubble();
        };

        setGraphic(graphic);
    }

    private Node createUserBubble() {
        // 内容在右侧
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        bubble.setAlignment(Pos.CENTER_RIGHT);
        // 设置气泡样式
        contentLabel.setBackground(new Background(new BackgroundFill(Paint.valueOf("#2f6feb"),
                new CornerRadii(12, 12, 2, 12, false), Insets.EMPTY)));
        contentLabel.setPadding(new Insets(10, 12, 8, 12));
        copyButton.setTextFill(Color.web("#5f6368"));
        return wrapper;
    }

    private Node createAssistantBubble() {
        // 内容在左侧
        wrapper.setAlignment(Pos.CENTER_LEFT);
        bubble.setAlignment(Pos.CENTER_LEFT);
        // 设置气泡样式
        contentLabel.setBackground(new Background(new BackgroundFill(Paint.valueOf("#f1f3f5"),
                new CornerRadii(12, 12, 12, 2, false), Insets.EMPTY)));
        contentLabel.setPadding(new Insets(10, 12, 8, 12));
        copyButton.setTextFill(Color.web("#5f6368"));
        return wrapper;
    }

    private Node createSystemBubble() {
        // 内容在左侧
        wrapper.setAlignment(Pos.CENTER);
        bubble.setAlignment(Pos.CENTER_LEFT);
        // 设置气泡样式
        contentLabel.setBackground(new Background(new BackgroundFill(Paint.valueOf("#fff3cd"),
                new CornerRadii(8), Insets.EMPTY)));
        contentLabel.setPadding(new Insets(8, 12, 8, 12));
        copyButton.setTextFill(Color.web("#5f6368"));
        return wrapper;
    }

    private Node createErrorBubble() {
        wrapper.setAlignment(Pos.CENTER_LEFT);
        bubble.setAlignment(Pos.CENTER_LEFT);
        // 设置气泡样式
        contentLabel.setBackground(new Background(new BackgroundFill(Paint.valueOf("#8b1a1a"),
                new CornerRadii(8), Insets.EMPTY)));
        contentLabel.setPadding(new Insets(8, 12, 8, 12));
        contentLabel.setBorder(new Border(new BorderStroke(Paint.valueOf("#e57373"), BorderStrokeStyle.SOLID,
                new CornerRadii(8), new BorderWidths(1))));
        copyButton.setTextFill(Color.web("#5f6368"));
        return wrapper;
    }

    private void copyCurrentMessage() {
        ChatMessage message = getItem();
        if (message == null || message.content() == null) {
            return;
        }

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(message.content());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private void clearCell() {
        contentLabel.setText("");
        setText(null);
        setGraphic(null);
    }
}
