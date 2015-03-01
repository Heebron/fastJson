/*
 * Copyright (C) 2015 kpratt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mobi.thepratts.util.json;

import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import static mobi.thepratts.util.json.JsonObject.LEXEME.BOD;
import static mobi.thepratts.util.json.JsonObject.LEXEME.COLON;
import static mobi.thepratts.util.json.JsonObject.LEXEME.COMMA;
import static mobi.thepratts.util.json.JsonObject.LEXEME.EOD;
import static mobi.thepratts.util.json.JsonObject.LEXEME.FALSE;
import static mobi.thepratts.util.json.JsonObject.LEXEME.L_BRACE;
import static mobi.thepratts.util.json.JsonObject.LEXEME.L_BRACKET;
import static mobi.thepratts.util.json.JsonObject.LEXEME.NULL;
import static mobi.thepratts.util.json.JsonObject.LEXEME.NUMBER;
import static mobi.thepratts.util.json.JsonObject.LEXEME.QUOTE;
import static mobi.thepratts.util.json.JsonObject.LEXEME.R_BRACE;
import static mobi.thepratts.util.json.JsonObject.LEXEME.R_BRACKET;
import static mobi.thepratts.util.json.JsonObject.LEXEME.STRING;
import static mobi.thepratts.util.json.JsonObject.LEXEME.TRUE;

/**
 *
 * @author Ken Pratt
 */
public class JsonObject implements JValue {

    final HashMap<String, JValue> map = new HashMap<>();

    @Override
    public Object getValue() {
        return map;
    }

    public void put(String key, JsonObject doc) {
        map.put(key, doc);
    }

    public void put(String key, List<? extends Object> values) {
        map.put(key, new JsonArray(values));
    }

    public void put(String key, String value) {
        map.put(key, new JsonEscaped(value));
    }

    public void put(String key, boolean value) {
        map.put(key, new JsonNotQuoted(value));
    }

    public void put(String key, Number value) {
        map.put(key, new JsonNotQuoted(value));
    }

    public void put(String key, Instant time) {
        map.put(key, new JsonNotEscaped(time));
    }

    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    private JValue walks(String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            if (!map.containsKey(path)) {
                throw new IllegalArgumentException("Key does not exist.");
            }
            return map.get(path);
        } else {
            JValue j = map.get(path.substring(0, index));
            if (j == null) {
                throw new IllegalArgumentException("Key does not exist.");
            }
            if (!(j instanceof JsonObject)) {
                throw new IllegalArgumentException("Key's value is not a JSONObject.");
            }
            return ((JsonObject) j).walks(path.substring(index + 1));
        }
    }

    public String getAsString(String key) {
        JValue ret = walks(key);
        return ret.getValue().toString();
    }

    public Instant getAsInstant(String key) {
        JValue ret = walks(key);
        if (ret.getValue() instanceof Instant) {
            return (Instant) ret.getValue();
        } else {
            return Instant.parse(ret.getValue().toString());
        }
    }

    public <T> T get(String key) {
        JValue ret = walks(key);
        return (T) ret.getValue();
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> "\"" + e.getKey() + "\":"
                + e.getValue().toString()).collect(Collectors.joining(",", "{", "}"));
    }

    static String escape(String value) {
        return value.replace("\"", "\\\"");
    }

    enum LEXEME {

        // The tokens of JSON
        BOD,
        L_BRACKET('['), R_BRACKET(']'),
        L_BRACE('{'), R_BRACE('}'),
        COLON(':'), QUOTE('"'),
        COMMA(','),
        STRING, TRUE, FALSE, NULL, NUMBER,
        EOD;
        private final Character c;

        LEXEME() {
            this.c = null;
        }

        LEXEME(Character c) {
            this.c = c;
        }

        static final LEXEME[] map = new LEXEME[92];

        static {
            for (LEXEME t : LEXEME.values()) {
                if (t.c != null) {
                    map[t.c - 34] = t;
                }
            }
        }

        static LEXEME map(int ch) {
            int index = ch - 34;
            if (index < 0 || index > 91) {
                return null;
            }
            return map[index];
        }
    };

    static JsonObject parse(final Reader doc) throws IOException {

        class Lexer {

            class ReadAhead {

                int next;

                ReadAhead() throws IOException {
                    next = doc.read();
                }

                int read() throws IOException {
                    int ret = next;
                    next = doc.read();
                    return ret;
                }

                int lookAhead() {
                    return next;
                }
            }

            Lexer() throws IOException {
                this.reader = new ReadAhead();
            }

            ReadAhead reader;
            LEXEME token = BOD;
            String value;
            StringBuilder sb = new StringBuilder();
            boolean isDecimal;

            void nextString() throws IOException {
                sb.setLength(0);
                sb.append((char) reader.read());
                while (reader.lookAhead() != '"') {
                    int ch = reader.read();
                    if (ch == -1) {
                        throw new IOException("End of document reached in string.");
                    }
                    if (ch == '\\') {
                        if (reader.lookAhead() == -1) {
                            throw new IOException("End of document reached in quote escape.");
                        } else if (reader.lookAhead() != '"') {
                            throw new IOException("Incorrect escape sequence \\" + reader.lookAhead());
                        }
                        ch = reader.read();
                    }
                    sb.append((char) ch);
                }
                value = sb.toString();
            }

            void nextToken() throws IOException {
                int ch = reader.read();
                if (ch == -1) {
                    token = EOD;
                    return;
                }
                token = LEXEME.map(ch);
                if (token == null) {
                    if (ch == 't' || ch == 'T') {
                        token = TRUE;
                        reader.read();
                        reader.read();
                        reader.read();
                    } else if (ch == 'f' || ch == 'F') {
                        token = FALSE;
                        reader.read();
                        reader.read();
                        reader.read();
                        reader.read();
                    } else if (ch == 'n' || ch == 'N') {
                        token = NULL;
                        reader.read();
                        reader.read();
                        reader.read();
                    } else {
                        // Must be a number.
                        sb.setLength(0);
                        isDecimal = false;

                        // index 0
                        if (ch == '-' || Character.isDigit(ch)) {
                            sb.append((char) ch);
                        } else {
                            throw new IOException("Expected a number, got " + (char) reader.lookAhead() + " instead.");
                        }

                        // Beyond index 0
                        for (;;) {
                            if (Character.isDigit(reader.lookAhead())) {
                                sb.append((char) reader.read());
                            } else if (reader.lookAhead() == '.') {
                                sb.append((char) reader.read());
                                isDecimal = true;
                            } else if (reader.lookAhead() == 'E'
                                    || reader.lookAhead() == 'e'
                                    || reader.lookAhead() == '+'
                                    || reader.lookAhead() == '-') {
                                sb.append((char) reader.read());
                                isDecimal = true;
                            } else {
                                break;
                            }
                        }
                        value = sb.toString();
                        token = NUMBER;
                    }
                }

                // Consume trailing whitespace
                consumeWhitepace();
            }

            void match(LEXEME nextToken) throws IOException {
                if (nextToken.c != null || nextToken == TRUE || nextToken == FALSE || nextToken == NULL) {
                    nextToken();
                    if (token != nextToken) {
                        throw new IOException("Expected '" + nextToken + "', but got '" + token + "'.");
                    }
                } else if (nextToken == STRING) {
                    nextString();
                    token = STRING;
                }

                // Consume trailing whitespace
                consumeWhitepace();
            }

            void consumeWhitepace() throws IOException {
                while (Character.isWhitespace(reader.lookAhead())) {
                    reader.read();
                }
            }

            LEXEME lookAhead() {
                LEXEME ret = LEXEME.map(reader.lookAhead());
                if (ret != null) {
                    return ret;
                }

                int t = reader.lookAhead() | 0x20;
                return t == 't' ? TRUE : t == 'f' ? FALSE : t == 'n' ? NULL : null;
            }

            JsonObject object() throws IOException {
                JsonObject top = new JsonObject();

                for (;;) {
                    // Process key.
                    match(QUOTE);
                    match(STRING);
                    String key = value;
                    match(QUOTE);
                    match(COLON);
                    nextToken();
                    // Process value.
                    if (token == TRUE) {
                        top.put(key, true);
                    } else if (token == FALSE) {
                        top.put(key, false);
                    } else if (token == NULL) {
                        top.put(key, new JsonObject());
                    } else if (token == QUOTE) {
                        match(STRING);
                        top.put(key, value);
                        match(QUOTE);
                    } else if (token == L_BRACKET) {
                        top.put(key, array());
                        match(R_BRACKET);
                    } else if (token == L_BRACE) {
                        top.put(key, object());
                        match(R_BRACE);
                    } else if (token == NUMBER) {
                        if (isDecimal) {
                            top.put(key, Double.valueOf(value));
                        } else {
                            top.put(key, Long.valueOf(value));
                        }
                    }

                    if (lookAhead() == COMMA) {
                        match(COMMA);
                        continue;
                    }
                    break;
                }
                return top;
            }

            private List array() throws IOException {
                List list = new ArrayList<>();

                // TODO: use a switch stmt and make boolean and null work in arrays.
                out:
                for (;;) {
                    switch (lookAhead()) {
                        case QUOTE:
                            match(QUOTE);
                            match(STRING);
                            list.add(value);
                            match(QUOTE);
                            break;
                        case L_BRACKET: // Embedded arry
                            match(L_BRACKET);
                            list.add(new JsonArray(array()));
                            match(R_BRACKET);
                            break;
                        case L_BRACE: // Embedded object
                            match(L_BRACE);
                            list.add(object());
                            match(R_BRACE);
                            break;
                        case TRUE:
                            list.add(true);
                            match(TRUE);
                            break;
                        case FALSE:
                            list.add(false);
                            match(FALSE);
                            break;
                        case NULL:
                            list.add(null);
                            match(NULL);
                            break;
                        case COMMA:
                            match(COMMA);
                            break;
                        default:
                            break out;
                    }
                }
                return list;
            }
        }

        Lexer lexer = new Lexer();
        // Start with lexer at BOD;
        lexer.match(L_BRACE);
        JsonObject obj = lexer.object();
        lexer.match(R_BRACE);
        return obj;
    }
}
