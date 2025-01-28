package com.weather.aggregation;

import java.io.IOException;
import java.util.*;

public class SimpleJsonParser {

    /**
     * Parses a JSON string into a Map.
     *
     * @param json The JSON string to parse.
     * @return A Map representing the JSON object.
     * @throws IOException If an error occurs during parsing.
     */
    public Map<String, Object> parse(String json) throws IOException {
        JsonTokenizer tokenizer = new JsonTokenizer(json);
        JsonToken token = tokenizer.nextToken();
        if (token.type != JsonTokenType.LEFT_BRACE) {
            throw new IOException("Expected '{' at the beginning of JSON object");
        }
        return parseObject(tokenizer);
    }

    private Map<String, Object> parseObject(JsonTokenizer tokenizer) throws IOException {
        Map<String, Object> map = new HashMap<>();
        JsonToken token;
        while ((token = tokenizer.nextToken()).type != JsonTokenType.RIGHT_BRACE) {
            if (token.type != JsonTokenType.STRING) {
                throw new IOException("Expected string key in JSON object");
            }
            String key = token.value;

            token = tokenizer.nextToken();
            if (token.type != JsonTokenType.COLON) {
                throw new IOException("Expected ':' after key in JSON object");
            }

            Object value = parseValue(tokenizer);

            map.put(key, value);

            token = tokenizer.peekToken();
            if (token.type == JsonTokenType.COMMA) {
                tokenizer.nextToken(); // consume comma and continue
            } else if (token.type == JsonTokenType.RIGHT_BRACE) {
                // End of object
                break;
            } else {
                throw new IOException("Expected ',' or '}' in JSON object");
            }
        }
        return map;
    }

    private Object parseValue(JsonTokenizer tokenizer) throws IOException {
        JsonToken token = tokenizer.nextToken();
        switch (token.type) {
            case LEFT_BRACE:
                return parseObject(tokenizer);
            case LEFT_BRACKET:
                return parseArray(tokenizer);
            case STRING:
                return token.value;
            case NUMBER:
                if (token.value.contains(".")) {
                    return Double.parseDouble(token.value);
                } else {
                    return Integer.parseInt(token.value);
                }
            case BOOLEAN:
                return Boolean.parseBoolean(token.value);
            case NULL:
                return null;
            default:
                throw new IOException("Unexpected token in JSON value");
        }
    }

    private List<Object> parseArray(JsonTokenizer tokenizer) throws IOException {
        List<Object> list = new ArrayList<>();
        JsonToken token;
        while ((token = tokenizer.peekToken()).type != JsonTokenType.RIGHT_BRACKET) {
            Object value = parseValue(tokenizer);
            list.add(value);

            token = tokenizer.peekToken();
            if (token.type == JsonTokenType.COMMA) {
                tokenizer.nextToken(); // consume comma and continue
            } else if (token.type == JsonTokenType.RIGHT_BRACKET) {
                // End of array
                break;
            } else {
                throw new IOException("Expected ',' or ']' in JSON array");
            }
        }
        tokenizer.nextToken(); // consume ']'
        return list;
    }

    /**
     * Generates a JSON string from a Map.
     *
     * @param map The Map to convert to JSON.
     * @return A JSON string representing the Map.
     */
    public String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, map);
        return sb.toString();
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            appendObject(sb, (Map<String, Object>) value);
        } else if (value instanceof List) {
            appendArray(sb, (List<Object>) value);
        } else if (value instanceof String) {
            sb.append('"').append(escapeString((String) value)).append('"');
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else {
            // For other types, call toString()
            sb.append('"').append(escapeString(value.toString())).append('"');
        }
    }

    private void appendObject(StringBuilder sb, Map<String, Object> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append('"').append(escapeString(entry.getKey())).append("\":");
            appendValue(sb, entry.getValue());
            first = false;
        }
        sb.append('}');
    }

    private void appendArray(StringBuilder sb, List<Object> list) {
        sb.append('[');
        boolean first = true;
        for (Object value : list) {
            if (!first) {
                sb.append(',');
            }
            appendValue(sb, value);
            first = false;
        }
        sb.append(']');
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Inner classes for tokenizing the JSON string

    enum JsonTokenType {
        LEFT_BRACE,     // {
        RIGHT_BRACE,    // }
        LEFT_BRACKET,   // [
        RIGHT_BRACKET,  // ]
        COMMA,          // ,
        COLON,          // :
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        EOF
    }

    class JsonToken {
        JsonTokenType type;
        String value;

        JsonToken(JsonTokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    class JsonTokenizer {
        private String json;
        private int pos;
        private JsonToken nextToken;

        JsonTokenizer(String json) {
            this.json = json;
            this.pos = 0;
        }

        JsonToken nextToken() throws IOException {
            if (nextToken != null) {
                JsonToken token = nextToken;
                nextToken = null;
                return token;
            }
            skipWhitespace();
            if (pos >= json.length()) {
                return new JsonToken(JsonTokenType.EOF, null);
            }
            char ch = json.charAt(pos);
            switch (ch) {
                case '{':
                    pos++;
                    return new JsonToken(JsonTokenType.LEFT_BRACE, "{");
                case '}':
                    pos++;
                    return new JsonToken(JsonTokenType.RIGHT_BRACE, "}");
                case '[':
                    pos++;
                    return new JsonToken(JsonTokenType.LEFT_BRACKET, "[");
                case ']':
                    pos++;
                    return new JsonToken(JsonTokenType.RIGHT_BRACKET, "]");
                case ',':
                    pos++;
                    return new JsonToken(JsonTokenType.COMMA, ",");
                case ':':
                    pos++;
                    return new JsonToken(JsonTokenType.COLON, ":");
                case '"':
                    return new JsonToken(JsonTokenType.STRING, readString());
                default:
                    if (isDigit(ch) || ch == '-') {
                        return new JsonToken(JsonTokenType.NUMBER, readNumber());
                    } else if (startsWith("true")) {
                        pos += 4;
                        return new JsonToken(JsonTokenType.BOOLEAN, "true");
                    } else if (startsWith("false")) {
                        pos += 5;
                        return new JsonToken(JsonTokenType.BOOLEAN, "false");
                    } else if (startsWith("null")) {
                        pos += 4;
                        return new JsonToken(JsonTokenType.NULL, "null");
                    } else {
                        throw new IOException("Unexpected character at position " + pos + ": " + ch);
                    }
            }
        }

        JsonToken peekToken() throws IOException {
            if (nextToken == null) {
                nextToken = nextToken();
            }
            return nextToken;
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private boolean isDigit(char ch) {
            return ch >= '0' && ch <= '9';
        }

        private String readString() throws IOException {
            StringBuilder sb = new StringBuilder();
            pos++; // skip opening quote
            while (pos < json.length()) {
                char ch = json.charAt(pos);
                if (ch == '\\') {
                    pos++;
                    if (pos >= json.length()) {
                        throw new IOException("Unexpected end of JSON string");
                    }
                    ch = json.charAt(pos);
                    if (ch == '"' || ch == '\\' || ch == '/') {
                        sb.append(ch);
                    } else if (ch == 'b') {
                        sb.append('\b');
                    } else if (ch == 'f') {
                        sb.append('\f');
                    } else if (ch == 'n') {
                        sb.append('\n');
                    } else if (ch == 'r') {
                        sb.append('\r');
                    } else if (ch == 't') {
                        sb.append('\t');
                    } else if (ch == 'u') {
                        // Unicode escape sequence
                        if (pos + 4 >= json.length()) {
                            throw new IOException("Invalid Unicode escape sequence");
                        }
                        String hex = json.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    } else {
                        throw new IOException("Invalid escape character: \\" + ch);
                    }
                } else if (ch == '"') {
                    pos++; // skip closing quote
                    return sb.toString();
                } else {
                    sb.append(ch);
                }
                pos++;
            }
            throw new IOException("Unterminated string");
        }

        private String readNumber() {
            int start = pos;
            if (json.charAt(pos) == '-') {
                pos++;
            }
            while (pos < json.length() && isDigit(json.charAt(pos))) {
                pos++;
            }
            if (pos < json.length() && json.charAt(pos) == '.') {
                pos++;
                while (pos < json.length() && isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < json.length() && isDigit(json.charAt(pos))) {
                    pos++;
                }
            }
            return json.substring(start, pos);
        }

        private boolean startsWith(String s) {
            return json.startsWith(s, pos);
        }
    }
}
