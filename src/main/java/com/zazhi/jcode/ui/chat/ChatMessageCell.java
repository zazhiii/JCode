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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Renders one chat message in the conversation list.
 *
 * <p>The controls are created once and reused by {@link ListCell}, so the
 * {@code ListView} can recycle this cell efficiently while scrolling.</p>
 */
public final class ChatMessageCell extends ListCell<ChatMessage> {

    private static final double MAX_BUBBLE_WIDTH_RATIO = 0.75;

    private static final String USER_BUBBLE_STYLE = """
            -fx-background-color: #2f6feb;
            -fx-background-radius: 12 12 2 12;
            -fx-padding: 10 12 8 12;
            """;

    private static final String ASSISTANT_BUBBLE_STYLE = """
            -fx-background-color: #f1f3f5;
            -fx-background-radius: 12 12 12 2;
            -fx-padding: 10 12 8 12;
            """;

    private static final String SYSTEM_BUBBLE_STYLE = """
            -fx-background-color: #fff3cd;
            -fx-background-radius: 8;
            -fx-padding: 8 12 8 12;
            """;

    private static final String ERROR_BUBBLE_STYLE = """
            -fx-background-color: #fde8e8;
            -fx-background-radius: 8;
            -fx-border-color: #e57373;
            -fx-border-radius: 8;
            -fx-padding: 8 12 8 12;
            """;

    private final HBox wrapper = new HBox();
    private final VBox bubble = new VBox(6);
    private final Label roleLabel = new Label();
    private final Label contentLabel = new Label();
    private final Button copyButton = new Button("复制");

    public ChatMessageCell() {
        initialiseView();
    }

    private void initialiseView() {
        setText(null);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setStyle("-fx-background-color: transparent; -fx-padding: 4 10 4 10;");

        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(Insets.EMPTY);
        wrapper.prefWidthProperty().bind(widthProperty().subtract(24));

        bubble.setFillWidth(true);
        bubble.maxWidthProperty().bind(
                widthProperty().multiply(MAX_BUBBLE_WIDTH_RATIO)
        );

        roleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

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

        HBox actions = new HBox(copyButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().setAll(roleLabel, contentLabel, actions);
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
        configureBubble(
                Pos.CENTER_RIGHT,
                Pos.CENTER_RIGHT,
                "你",
                USER_BUBBLE_STYLE,
                "white",
                "rgba(255, 255, 255, 0.82)"
        );
        return wrapper;
    }

    private Node createAssistantBubble() {
        configureBubble(
                Pos.CENTER_LEFT,
                Pos.CENTER_LEFT,
                "JCode",
                ASSISTANT_BUBBLE_STYLE,
                "#202124",
                "#5f6368"
        );
        return wrapper;
    }

    private Node createSystemBubble() {
        configureBubble(
                Pos.CENTER,
                Pos.CENTER_LEFT,
                "系统",
                SYSTEM_BUBBLE_STYLE,
                "#5f4b00",
                "#806600"
        );
        return wrapper;
    }

    private Node createErrorBubble() {
        configureBubble(
                Pos.CENTER_LEFT,
                Pos.CENTER_LEFT,
                "错误",
                ERROR_BUBBLE_STYLE,
                "#8b1a1a",
                "#a33a3a"
        );
        return wrapper;
    }

    private void configureBubble(
            Pos wrapperAlignment,
            Pos bubbleAlignment,
            String roleText,
            String bubbleStyle,
            String contentColor,
            String secondaryColor
    ) {
        wrapper.setAlignment(wrapperAlignment);
        bubble.setAlignment(bubbleAlignment);
        bubble.setStyle(bubbleStyle);

        roleLabel.setText(roleText);
        roleLabel.setTextFill(javafx.scene.paint.Color.web(secondaryColor));
        contentLabel.setTextFill(javafx.scene.paint.Color.web(contentColor));
        copyButton.setTextFill(javafx.scene.paint.Color.web(secondaryColor));
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
        roleLabel.setText("");
        setText(null);
        setGraphic(null);
    }
}
