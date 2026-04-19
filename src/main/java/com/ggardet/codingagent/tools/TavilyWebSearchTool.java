package com.ggardet.codingagent.tools;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class TavilyWebSearchTool {
    private final RestClient restClient;
    private final String apiKey;
    private final int maxResults;

    private static final Logger logger = LoggerFactory.getLogger(TavilyWebSearchTool.class);
    private static final String TAVILY_API_BASE_URL = "https://api.tavily.com";
    private static final String SEARCH_PATH = "/search";

    private TavilyWebSearchTool(final String apiKey, final int maxResults) {
        Assert.hasText(apiKey, "API key must not be null or empty");
        this.apiKey = apiKey;
        this.maxResults = maxResults;
        this.restClient = RestClient.builder()
                .baseUrl(TAVILY_API_BASE_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Tool(name = "WebSearch", description = """
        - Allows Claude to search the web and use the results to inform responses
        - Provides up-to-date information for current events and recent data
        - Returns search result information formatted as search result blocks, including links as markdown hyperlinks
        - Use this tool for accessing information beyond Claude's knowledge cutoff
        - Searches are performed automatically within a single API call
        - Domain filtering is natively supported server-side, so no API quota is wasted

        CRITICAL REQUIREMENT - You MUST follow this:
        - After answering the user's question, you MUST include a "Sources:" section at the end of your response
        - In the Sources section, list all relevant URLs from the search results as markdown hyperlinks: [Title](URL)
        - This is MANDATORY - never skip including sources in your response
        - Example format:

            [Your answer here]

            Sources:
            - [Source Title 1](https://example.com/1)
            - [Source Title 2](https://example.com/2)

        IMPORTANT - Use the correct year in search queries:
        - When searching for recent information, documentation, or current events, always include the current year in your query
        - Example: If searching for latest React docs, search for "React documentation 2025" rather than older years
        """)
    public String webSearch(
            @ToolParam(description = "The search query to use") final String query,
            @ToolParam(description = "Only include search results from these domains", required = false) final List<String> includeDomains,
            @ToolParam(description = "Never include search results from these domains", required = false) final List<String> excludeDomains) {
        if (!StringUtils.hasText(query)) {
            logger.warn("Empty search query provided");
            return JsonParser.toJson(Collections.emptyList());
        }
        try {
            final var requestBody = buildRequestBody(query, includeDomains, excludeDomains);
            final var response = executeSearch(requestBody);
            if (response.isEmpty()) {
                logger.warn("Empty response from Tavily API for query: {}", query);
                return JsonParser.toJson(Collections.emptyList());
            }
            final var results = parseResults(response);
            logger.debug("Search for '{}' returned {} results", query, results.size());
            return JsonParser.toJson(results);
        } catch (final RestClientException exception) {
            logger.error("Error executing Tavily Search API request for query: {}", query, exception);
            return JsonParser.toJson(Collections.emptyList());
        }
    }

    private Map<String, Object> buildRequestBody(
            final String query,
            final List<String> includeDomains,
            final List<String> excludeDomains) {
        final var body = new java.util.HashMap<String, Object>();
        body.put("api_key", this.apiKey);
        body.put("query", query);
        body.put("max_results", this.maxResults);
        if (!CollectionUtils.isEmpty(includeDomains)) {
            body.put("include_domains", includeDomains);
        }
        if (!CollectionUtils.isEmpty(excludeDomains)) {
            body.put("exclude_domains", excludeDomains);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeSearch(final Map<String, Object> requestBody) {
        try {
            final Map<String, Object> response = this.restClient.post()
                    .uri(SEARCH_PATH)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (_, errorResponse) ->
                            logger.error("Client error from Tavily API: {}", errorResponse.getStatusCode()))
                    .onStatus(HttpStatusCode::is5xxServerError, (_, errorResponse) ->
                            logger.error("Server error from Tavily API: {}", errorResponse.getStatusCode()))
                    .body(Map.class);
            return response != null ? response : Collections.emptyMap();
        } catch (final Exception exception) {
            logger.error("Failed to execute Tavily search request", exception);
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> parseResults(final Map<String, Object> response) {
        final var rawResults = (List<Map<String, Object>>) response.get("results");
        if (CollectionUtils.isEmpty(rawResults)) {
            return Collections.emptyList();
        }
        return rawResults.stream()
                .filter(entry -> entry != null && entry.get("title") != null && entry.get("url") != null)
                .map(entry -> new SearchResult(
                        (String) entry.get("title"),
                        (String) entry.get("url"),
                        entry.get("content") != null ? (String) entry.get("content") : ""))
                .toList();
    }

    public record SearchResult(String title, String url, String description) {
    }

    public static Builder builder(final String apiKey) {
        return new Builder(apiKey);
    }

    public static class Builder {
        private final String apiKey;

        private Builder(final String apiKey) {
            if (!StringUtils.hasText(apiKey)) {
                throw new IllegalArgumentException("API key must not be null or empty");
            }
            this.apiKey = apiKey;
        }

        public TavilyWebSearchTool build() {
            final int maxResults = 10;
            return new TavilyWebSearchTool(this.apiKey, maxResults);
        }
    }
}
