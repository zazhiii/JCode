package com.zazhi.gui.ui.records;


import com.zazhi.gui.ui.enums.MessageRole;

public record ChatMessage(
        MessageRole role,
        String content
) {
}