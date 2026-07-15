package com.ggardet.codingagent.coding.tool;

/// A single web-search result.
///
/// @param title the result title
/// @param url the result URL
/// @param description a short content snippet, possibly empty
public record SearchResult(String title, String url, String description) {
}
