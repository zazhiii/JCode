package com.zazhi.jcode.test;

/**
 * @author zazhi
 * @date 2026/9/6
 * @description:
 */
public class TestMessage {
    public static String input = "你好";
    public static String resp = "可以，下面这段 Java 代码基本等价于你这份 FXML，是纯 JavaFX 方式手动构建 UI。\n" +
            "\n" +
            "```java\n" +
            "import javafx.geometry.Pos;\n" +
            "import javafx.scene.control.*;\n" +
            "import javafx.scene.layout.*;\n" +
            "\n" +
            "public class MainView {\n" +
            "\n" +
            "    private TextArea inputTextArea;\n" +
            "    private Button sendButton;\n" +
            "    private ListView<String> chatListView;\n" +
            "\n" +
            "    public BorderPane createView() {\n" +
            "\n" +
            "        // =========================\n" +
            "        // 根布局\n" +
            "        // =========================\n" +
            "        BorderPane root = new BorderPane();\n" +
            "\n" +
            "        root.setPrefWidth(600);\n" +
            "        root.setPrefHeight(400);\n" +
            "\n" +
            "\n" +
            "        // =========================\n" +
            "        // 顶部 MenuBar\n" +
            "        // =========================\n" +
            "        MenuBar menuBar = new MenuBar();\n" +
            "\n" +
            "        Menu settingMenu = new Menu(\"设置\");\n" +
            "        settingMenu.setMnemonicParsing(false);\n" +
            "\n" +
            "        menuBar.getMenus().add(settingMenu);\n" +
            "\n" +
            "        root.setTop(menuBar);\n" +
            "\n" +
            "\n" +
            "        // =========================\n" +
            "        // 左侧 VBox\n" +
            "        // =========================\n" +
            "        VBox leftBox = new VBox();\n" +
            "\n" +
            "        leftBox.setPrefWidth(71);\n" +
            "        leftBox.setPrefHeight(310);\n" +
            "\n" +
            "        leftBox.setStyle(\n" +
            "                \"-fx-border-width: 1px;\" +\n" +
            "                \"-fx-border-color: black;\"\n" +
            "        );\n" +
            "\n" +
            "        root.setLeft(leftBox);\n" +
            "\n" +
            "\n" +
            "        // =========================\n" +
            "        // 右侧 VBox\n" +
            "        // =========================\n" +
            "        VBox rightBox = new VBox();\n" +
            "\n" +
            "        rightBox.setPrefWidth(74);\n" +
            "        rightBox.setPrefHeight(310);\n" +
            "\n" +
            "        rightBox.setStyle(\n" +
            "                \"-fx-border-width: 1;\" +\n" +
            "                \"-fx-border-color: black;\"\n" +
            "        );\n" +
            "\n" +
            "        root.setRight(rightBox);\n" +
            "\n" +
            "\n" +
            "        // =========================\n" +
            "        // 中间聊天列表\n" +
            "        // =========================\n" +
            "        chatListView = new ListView<>();\n" +
            "\n" +
            "        chatListView.setPrefWidth(200);\n" +
            "        chatListView.setPrefHeight(200);\n" +
            "\n" +
            "        root.setCenter(chatListView);\n" +
            "\n" +
            "\n" +
            "        // =========================\n" +
            "        // 底部输入区域\n" +
            "        // =========================\n" +
            "        HBox bottomBox = new HBox();\n" +
            "\n" +
            "        bottomBox.setAlignment(Pos.CENTER);\n" +
            "        bottomBox.setFillHeight(false);\n" +
            "\n" +
            "        bottomBox.setPrefWidth(600);\n" +
            "        bottomBox.setPrefHeight(93);\n" +
            "\n" +
            "        bottomBox.setStyle(\n" +
            "                \"-fx-border-width: 1;\" +\n" +
            "                \"-fx-border-color: black;\"\n" +
            "        );\n" +
            "\n" +
            "\n" +
            "        // 输入框\n" +
            "        inputTextArea = new TextArea();\n" +
            "\n" +
            "        inputTextArea.setPrefWidth(200);\n" +
            "        inputTextArea.setPrefHeight(3);\n" +
            "\n" +
            "        inputTextArea.setPrefRowCount(15);\n" +
            "        inputTextArea.setPromptText(\"给JCode发消息\");\n" +
            "\n" +
            "        inputTextArea.setWrapText(true);\n" +
            "\n" +
            "\n" +
            "        // 发送按钮\n" +
            "        sendButton = new Button(\"发送\");\n" +
            "\n" +
            "        sendButton.setMnemonicParsing(false);\n" +
            "\n" +
            "        sendButton.setPrefWidth(54);\n" +
            "        sendButton.setPrefHeight(27);\n" +
            "\n" +
            "\n" +
            "        bottomBox.getChildren().addAll(\n" +
            "                inputTextArea,\n" +
            "                sendButton\n" +
            "        );\n" +
            "\n" +
            "        root.setBottom(bottomBox);\n" +
            "\n" +
            "\n" +
            "        return root;\n" +
            "    }\n" +
            "\n" +
            "    public TextArea getInputTextArea() {\n" +
            "        return inputTextArea;\n" +
            "    }\n" +
            "\n" +
            "    public Button getSendButton() {\n" +
            "        return sendButton;\n" +
            "    }\n" +
            "\n" +
            "    public ListView<String> getChatListView() {\n" +
            "        return chatListView;\n" +
            "    }\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "然后在 `Application` 中使用：\n" +
            "\n" +
            "```java\n" +
            "import javafx.application.Application;\n" +
            "import javafx.scene.Scene;\n" +
            "import javafx.stage.Stage;\n" +
            "\n" +
            "public class MainApplication extends Application {\n" +
            "\n" +
            "    @Override\n" +
            "    public void start(Stage stage) {\n" +
            "\n" +
            "        MainView mainView = new MainView();\n" +
            "\n" +
            "        Scene scene = new Scene(\n" +
            "                mainView.createView(),\n" +
            "                600,\n" +
            "                400\n" +
            "        );\n" +
            "\n" +
            "        stage.setScene(scene);\n" +
            "        stage.setTitle(\"JCode\");\n" +
            "\n" +
            "        stage.show();\n" +
            "    }\n" +
            "\n" +
            "    public static void main(String[] args) {\n" +
            "        launch(args);\n" +
            "    }\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "你原来的 FXML：\n" +
            "\n" +
            "```xml\n" +
            "fx:id=\"inputTextArea\"\n" +
            "```\n" +
            "\n" +
            "在纯 Java 里面就不再需要 `fx:id`，而是直接：\n" +
            "\n" +
            "```java\n" +
            "private TextArea inputTextArea;\n" +
            "```\n" +
            "\n" +
            "然后：\n" +
            "\n" +
            "```java\n" +
            "inputTextArea = new TextArea();\n" +
            "```\n" +
            "\n" +
            "同理：\n" +
            "\n" +
            "```xml\n" +
            "fx:id=\"sendButton\"\n" +
            "```\n" +
            "\n" +
            "对应：\n" +
            "\n" +
            "```java\n" +
            "private Button sendButton;\n" +
            "```\n" +
            "\n" +
            "而：\n" +
            "\n" +
            "```xml\n" +
            "fx:controller=\"com.zazhi.jcode.ui.controller.MainController\"\n" +
            "```\n" +
            "\n" +
            "这个概念在纯 Java UI 中也可以不用 FXML Controller 机制了。\n" +
            "\n" +
            "你可以自己把事件直接注册进去：\n" +
            "\n" +
            "```java\n" +
            "sendButton.setOnAction(event -> {\n" +
            "    String text = inputTextArea.getText();\n" +
            "\n" +
            "    if (!text.isBlank()) {\n" +
            "        chatListView.getItems().add(text);\n" +
            "        inputTextArea.clear();\n" +
            "    }\n" +
            "});\n" +
            "```\n" +
            "\n" +
            "如果你还希望保持 **View 和 Controller 分离**，那可以这样：\n" +
            "\n" +
            "```java\n" +
            "public class MainController {\n" +
            "\n" +
            "    private final MainView view;\n" +
            "\n" +
            "    public MainController(MainView view) {\n" +
            "        this.view = view;\n" +
            "\n" +
            "        initEvents();\n" +
            "    }\n" +
            "\n" +
            "    private void initEvents() {\n" +
            "\n" +
            "        view.getSendButton().setOnAction(event -> {\n" +
            "\n" +
            "            String message =\n" +
            "                    view.getInputTextArea().getText();\n" +
            "\n" +
            "            if (message == null || message.isBlank()) {\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            view.getChatListView()\n" +
            "                    .getItems()\n" +
            "                    .add(message);\n" +
            "\n" +
            "            view.getInputTextArea().clear();\n" +
            "        });\n" +
            "    }\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "启动时：\n" +
            "\n" +
            "```java\n" +
            "@Override\n" +
            "public void start(Stage stage) {\n" +
            "\n" +
            "    MainView view = new MainView();\n" +
            "\n" +
            "    BorderPane root = view.createView();\n" +
            "\n" +
            "    MainController controller =\n" +
            "            new MainController(view);\n" +
            "\n" +
            "    Scene scene = new Scene(root, 600, 400);\n" +
            "\n" +
            "    stage.setScene(scene);\n" +
            "    stage.show();\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "不过你这份 FXML 里还有一个地方我建议改一下：\n" +
            "\n" +
            "```xml\n" +
            "<TextArea\n" +
            "    prefHeight=\"3.0\"\n" +
            "    prefRowCount=\"15\"\n" +
            "    prefWidth=\"200.0\"\n" +
            "/>\n" +
            "```\n" +
            "\n" +
            "`prefHeight=\"3.0\"` 和 `prefRowCount=\"15\"` 实际上有点冲突。\n" +
            "\n" +
            "`TextArea` 希望显示 15 行，但你又把首选高度设置成了 `3px`。\n" +
            "\n" +
            "纯 Java 里建议直接去掉：\n" +
            "\n" +
            "```java\n" +
            "inputTextArea.setPrefHeight(3);\n" +
            "```\n" +
            "\n" +
            "保留：\n" +
            "\n" +
            "```java\n" +
            "inputTextArea.setPrefRowCount(3);\n" +
            "```\n" +
            "\n" +
            "甚至对于你这个聊天输入框，我更推荐：\n" +
            "\n" +
            "```java\n" +
            "inputTextArea.setPrefRowCount(3);\n" +
            "inputTextArea.setWrapText(true);\n" +
            "```\n" +
            "\n" +
            "另外你现在的布局：\n" +
            "\n" +
            "```text\n" +
            "┌──────────────────────────────────────────┐\n" +
            "│ 设置                                      │\n" +
            "├──────┬───────────────────────────┬───────┤\n" +
            "│      │                           │       │\n" +
            "│ 左栏 │       ListView            │ 右栏  │\n" +
            "│      │                           │       │\n" +
            "├──────┴───────────────────────────┴───────┤\n" +
            "│          TextArea          [发送]        │\n" +
            "└──────────────────────────────────────────┘\n" +
            "```\n" +
            "\n" +
            "本身非常适合你现在这个 **JCode AI Coding Agent 桌面客户端**。\n" +
            "\n" +
            "如果继续往下做，我建议下一步把中间的：\n" +
            "\n" +
            "```java\n" +
            "ListView<String>\n" +
            "```\n" +
            "\n" +
            "改成：\n" +
            "\n" +
            "```java\n" +
            "ListView<ChatMessage>\n" +
            "```\n" +
            "\n" +
            "然后通过：\n" +
            "\n" +
            "```java\n" +
            "chatListView.setCellFactory(...)\n" +
            "```\n" +
            "\n" +
            "自己做 `ListCell`，这样才能实现类似 ChatGPT / Claude Code 的：\n" +
            "\n" +
            "* 用户消息靠右\n" +
            "* AI 消息靠左\n" +
            "* tool call 单独卡片\n" +
            "* tool result 折叠显示\n" +
            "* Markdown 渲染\n" +
            "* 代码块\n" +
            "* Copy 按钮\n" +
            "\n" +
            "这会比直接往 `ListView<String>` 塞字符串更适合你的 JCode。\n";
}
