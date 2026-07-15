package com.ggardet.codingagent.coding.tui;

/// A single line displayed in the conversation view.
///
/// @param type the message origin
/// @param content the text to display
record Message(MessageType type, String content) {
}
