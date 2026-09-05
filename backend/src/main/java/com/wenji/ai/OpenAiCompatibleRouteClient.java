package com.wenji.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleRouteClient implements AiRouteClient {

    private static final String SYSTEM_PROMPT = """
            你是城市文化研学路线规划助手。只能从候选资源中选择地点，并且只返回一个 JSON 对象，
            不要返回 Markdown、代码围栏或解释文字。日期和时间必须位于用户指定范围内，同一天的时间不能重叠。
            输出字段必须包含 title、summary、estimatedBudget、days、tips；days 中包含 date 和 items，
            items 中包含 resourceId、startTime、endTime、transportation、reason。
            """;

    private final AiRouteProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleRouteClient(AiRouteProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(String prompt) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        String endpoint = properties.getBaseUrl().replaceAll("/+$", "");
        if (!endpoint.endsWith("/chat/completions")) {
            endpoint += "/chat/completions";
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)));

        String response = RestClient.builder()
                .requestFactory(requestFactory)
                .build()
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank()) {
                throw new IllegalStateException("AI 服务未返回路线内容");
            }
            return content;
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析 AI 服务响应", exception);
        }
    }
}
