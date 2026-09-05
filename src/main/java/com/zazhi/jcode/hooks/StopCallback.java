package com.zazhi.jcode.hooks;

import com.anthropic.models.messages.MessageParam;

import java.util.List;

/**
 *
 * @author lixh
 * @since 2026/9/6 0:05
 */
public interface StopCallback extends HooksCallback {
    void onStop(List<MessageParam> messages);
}
