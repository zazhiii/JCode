package com.zazhi.jcode.ui.records;

import com.zazhi.jcode.ui.enums.MessageRole;

public record ChatMessage(
        MessageRole role,
        String content
) {
}