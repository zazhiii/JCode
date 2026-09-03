package com.zazhi.jcode.ui.chat;

import com.zazhi.jcode.ui.enums.MessageRole;
import com.zazhi.jcode.ui.markdown.MarkdownView;
import com.zazhi.jcode.ui.records.ChatMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
    private final MarkdownView contentView = new MarkdownView();
    private final Button copyButton = new Button("复制");

    public ChatMessageCell() {
        initialiseView();
    }

    private void initialiseView() {
        setText(null);
//        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setStyle("-fx-background-color: transparent; -fx-padding: 4 10 4 10;");

        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(Insets.EMPTY);
        // 容器的期望宽度绑定到单元格的宽度减去24
        wrapper.prefWidthProperty().bind(widthProperty().subtract(24));
        // 鼠标移入显示复制按钮，移出隐藏
        wrapper.setOnMouseEntered(event -> copyButton.setVisible(true));
        wrapper.setOnMouseExited(event -> copyButton.setVisible(false));

        bubble.setFillWidth(true);
        // 气泡宽度最多占75%的单元格宽度
        bubble.maxWidthProperty().bind(
                widthProperty().multiply(MAX_BUBBLE_WIDTH_RATIO)
        );

        contentView.setMinWidth(0);
        contentView.maxWidthProperty().bind(bubble.maxWidthProperty().subtract(8));

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

        bubble.getChildren().setAll(contentView, actions);
        wrapper.getChildren().setAll(bubble);
    }

    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            clearCell();
            return;
        }

        MessageRole role = message.role() == null
                ? MessageRole.ERROR
                : message.role();

        Node graphic = switch (role) {
            case USER -> configureBubble(
                    Pos.CENTER_RIGHT,
                    Pos.CENTER_RIGHT,
                    USER_BUBBLE_STYLE,
                    Color.WHITE,
                    "rgba(255, 255, 255, 0.82)"
            );
            case ASSISTANT -> configureBubble(
                    Pos.CENTER_LEFT,
                    Pos.CENTER_LEFT,
                    ASSISTANT_BUBBLE_STYLE,
                    Color.web("#202124"),
                    "#5f6368"
            );
            case SYSTEM -> configureBubble(
                    Pos.CENTER,
                    Pos.CENTER_LEFT,
                    SYSTEM_BUBBLE_STYLE,
                    Color.web("#5f4b00"),
                    "#806600"
            );
            case ERROR -> configureBubble(
                    Pos.CENTER_LEFT,
                    Pos.CENTER_LEFT,
                    ERROR_BUBBLE_STYLE,
                    Color.web("#8b1a1a"),
                    "#a33a3a"
            );
        };

        contentView.setMarkdown(message.content() == null ? "" : message.content());
        setGraphic(graphic);
    }

    private Node configureBubble(
            Pos wrapperAlignment,
            Pos bubbleAlignment,
            String bubbleStyle,
            Color contentColor,
            String secondaryColor
    ) {
        wrapper.setAlignment(wrapperAlignment);
        bubble.setAlignment(bubbleAlignment);
        bubble.setStyle(bubbleStyle);
        contentView.setTextColor(contentColor);
        copyButton.setTextFill(Color.web(secondaryColor));
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
        contentView.setMarkdown("");
        setText(null);
        setGraphic(null);
    }
}
