package com.zazhi.jcode.ui.markdown;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.parser.Parser;

/**
 * 将 Markdown 渲染为 JavaFX 节点。
 */
public final class MarkdownView extends VBox {

    private static final Parser PARSER = Parser.builder().build();
    private static final double BASE_FONT_SIZE = 13;

    private Color textColor = Color.web("#202124");

    public MarkdownView() {
        setSpacing(6);
        setFillWidth(true);
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor == null ? Color.web("#202124") : textColor;
    }

    public void setMarkdown(String markdown) {
        getChildren().clear();
        if (markdown == null || markdown.isBlank()) {
            return;
        }
        acceptChildren(PARSER.parse(markdown), this, 0);
    }

    private void acceptChildren(org.commonmark.node.Node parent, VBox target, int listDepth) {
        org.commonmark.node.Node child = parent.getFirstChild();
        while (child != null) {
            org.commonmark.node.Node next = child.getNext();
            renderBlock(child, target, listDepth);
            child = next;
        }
    }

    private void renderBlock(org.commonmark.node.Node node, VBox target, int listDepth) {
        switch (node) {
            case Heading heading -> {
                TextFlow flow = newFlow();
                new InlineBuilder(flow, headingSize(heading.getLevel()), true).writeChildren(heading);
                target.getChildren().add(flow);
            }
            case Paragraph paragraph -> {
                TextFlow flow = newFlow();
                new InlineBuilder(flow, BASE_FONT_SIZE, false).writeChildren(paragraph);
                target.getChildren().add(flow);
            }
            case BulletList bulletList -> {
                for (org.commonmark.node.Node child = bulletList.getFirstChild();
                     child != null;
                     child = child.getNext()) {
                    if (child instanceof ListItem item) {
                        addListItem(item, "• ", target, listDepth);
                    }
                }
            }
            case OrderedList orderedList -> {
                int index = orderedList.getMarkerStartNumber();
                for (org.commonmark.node.Node child = orderedList.getFirstChild();
                     child != null;
                     child = child.getNext()) {
                    if (child instanceof ListItem item) {
                        addListItem(item, index + ". ", target, listDepth);
                        index++;
                    }
                }
            }
            case FencedCodeBlock codeBlock ->
                    target.getChildren().add(codeBlockNode(codeBlock.getLiteral()));
            case IndentedCodeBlock codeBlock ->
                    target.getChildren().add(codeBlockNode(codeBlock.getLiteral()));
            case BlockQuote blockQuote -> {
                VBox quote = new VBox(4);
                quote.setMaxWidth(Double.MAX_VALUE);
                quote.setStyle("""
                        -fx-padding: 4 8 4 10;
                        -fx-border-color: #c5c9ce;
                        -fx-border-width: 0 0 0 3;
                        -fx-background-color: rgba(0,0,0,0.03);
                        """);
                acceptChildren(blockQuote, quote, listDepth);
                target.getChildren().add(quote);
            }
            default -> acceptChildren(node, target, listDepth);
        }
    }

    private void addListItem(ListItem item, String marker, VBox target, int listDepth) {
        VBox itemBox = new VBox(4);
        itemBox.setFillWidth(true);
        itemBox.setMaxWidth(Double.MAX_VALUE);
        itemBox.setStyle("-fx-padding: 0 0 0 " + (listDepth * 12) + ";");

        boolean firstContent = true;
        for (org.commonmark.node.Node child = item.getFirstChild();
             child != null;
             child = child.getNext()) {
            if (child instanceof Paragraph paragraph) {
                TextFlow flow = newFlow();
                if (firstContent) {
                    flow.getChildren().add(styledText(marker, BASE_FONT_SIZE, false, false, false));
                    firstContent = false;
                }
                new InlineBuilder(flow, BASE_FONT_SIZE, false).writeChildren(paragraph);
                itemBox.getChildren().add(flow);
            } else if (child instanceof FencedCodeBlock codeBlock) {
                if (firstContent) {
                    TextFlow flow = newFlow();
                    flow.getChildren().add(styledText(marker, BASE_FONT_SIZE, false, false, false));
                    itemBox.getChildren().add(flow);
                    firstContent = false;
                }
                itemBox.getChildren().add(codeBlockNode(codeBlock.getLiteral()));
            } else if (child instanceof BulletList || child instanceof OrderedList) {
                if (firstContent) {
                    TextFlow flow = newFlow();
                    flow.getChildren().add(styledText(marker, BASE_FONT_SIZE, false, false, false));
                    itemBox.getChildren().add(flow);
                    firstContent = false;
                }
                VBox nested = new VBox(4);
                nested.setFillWidth(true);
                nested.setMaxWidth(Double.MAX_VALUE);
                renderBlock(child, nested, listDepth + 1);
                itemBox.getChildren().add(nested);
            }
        }

        if (firstContent) {
            TextFlow flow = newFlow();
            flow.getChildren().add(styledText(marker, BASE_FONT_SIZE, false, false, false));
            itemBox.getChildren().add(flow);
        }

        target.getChildren().add(itemBox);
    }

    private TextFlow newFlow() {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(Double.MAX_VALUE);
        return flow;
    }

    private final class InlineBuilder extends AbstractVisitor {
        private final TextFlow flow;
        private final double fontSize;
        private boolean bold;
        private boolean italic;

        InlineBuilder(TextFlow flow, double fontSize, boolean bold) {
            this.flow = flow;
            this.fontSize = fontSize;
            this.bold = bold;
        }

        void writeChildren(org.commonmark.node.Node parent) {
            org.commonmark.node.Node child = parent.getFirstChild();
            while (child != null) {
                org.commonmark.node.Node next = child.getNext();
                child.accept(this);
                child = next;
            }
        }

        @Override
        public void visit(org.commonmark.node.Text text) {
            flow.getChildren().add(styledText(text.getLiteral(), fontSize, bold, italic, false));
        }

        @Override
        public void visit(Code codeNode) {
            flow.getChildren().add(styledText(codeNode.getLiteral(), fontSize, bold, italic, true));
        }

        @Override
        public void visit(Emphasis emphasis) {
            boolean prev = italic;
            italic = true;
            writeChildren(emphasis);
            italic = prev;
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            boolean prev = bold;
            bold = true;
            writeChildren(strongEmphasis);
            bold = prev;
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            flow.getChildren().add(styledText(" ", fontSize, bold, italic, false));
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            flow.getChildren().add(new Text("\n"));
        }

        @Override
        public void visit(Link link) {
            boolean prev = italic;
            italic = true;
            writeChildren(link);
            italic = prev;
            if (link.getDestination() != null && !link.getDestination().isBlank()) {
                Text dest = styledText(
                        " (" + link.getDestination() + ")",
                        Math.max(11, fontSize - 1),
                        false,
                        true,
                        false
                );
                dest.setFill(textColor.deriveColor(0, 1, 1, 0.7));
                flow.getChildren().add(dest);
            }
        }
    }

    private Node codeBlockNode(String literal) {
        String content = literal == null ? "" : literal.stripTrailing();
        Label label = new Label(content);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("""
                -fx-font-family: Consolas, 'Courier New', monospace;
                -fx-font-size: 12px;
                -fx-background-color: #e8eaed;
                -fx-background-radius: 6;
                -fx-padding: 8 10 8 10;
                -fx-text-fill: #202124;
                """);
        return label;
    }

    private Text styledText(
            String value,
            double fontSize,
            boolean bold,
            boolean italic,
            boolean code
    ) {
        Text text = new Text(value == null ? "" : value);
        if (code) {
            text.setFont(Font.font("Consolas", FontWeight.NORMAL, FontPosture.REGULAR, fontSize));
            text.setFill(Color.web("#c7254e"));
        } else {
            FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
            FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
            text.setFont(Font.font("System", weight, posture, fontSize));
            text.setFill(textColor);
        }
        return text;
    }

    private static double headingSize(int level) {
        return switch (level) {
            case 1 -> 20;
            case 2 -> 17;
            case 3 -> 15;
            default -> 14;
        };
    }
}
