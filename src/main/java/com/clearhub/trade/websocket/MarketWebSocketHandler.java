package com.clearhub.trade.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new SessionState(session));
        send(session, "{\"type\":\"hello\",\"message\":\"connected\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if ("subscribe".equals(node.path("type").asText())) {
                SessionState state = sessions.get(session.getId());
                if (state != null && node.hasNonNull("commodityId")) {
                    state.commodityId = node.get("commodityId").asLong();
                }
            }
        } catch (Exception ex) {
            log.debug("Invalid websocket message: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void broadcast(long commodityId, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("Failed to serialize websocket payload", ex);
            return;
        }
        sessions.values().forEach(state -> {
            if (state.commodityId != null && state.commodityId == commodityId) {
                send(state.session, json);
            }
        });
    }

    public int sessionCount() {
        return sessions.size();
    }

    private void send(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException ex) {
            log.debug("Failed to send websocket message", ex);
        }
    }

    private static final class SessionState {
        private final WebSocketSession session;
        private volatile Long commodityId;

        private SessionState(WebSocketSession session) {
            this.session = session;
        }
    }
}
