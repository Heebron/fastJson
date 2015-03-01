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

    public void put(String key, JValue objOrArray) {
        map.put(key, objOrArray);
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
        STRING,
        EOD,
        L_BRACKET('['),
        R_BRACKET(']'),
        L_BRACE('{'),
        R_BRACE('}'),
        COLON(':'),
        QUOTE('"'),
        COMMA(','),
        TRUE("Tt"),
        FALSE("Ff"),
        NULL("Nn"),
        NUMBER("-0123456789.");

        private final String mappings;

        LEXEME() {
            this.mappings = null;
        }

        LEXEME(Character c) {
            this.mappings = c + "";
        }

        LEXEME(String mappings) {
            this.mappings = mappings;
        }

        static final LEXEME[] map = new LEXEME[126];

        static {
            // Set up the lookup table.
            for (LEXEME t : LEXEME.values()) {
                if (t.mappings != null) {
                    for (int i = 0; i < t.mappings.length(); i++) {
                        map[t.mappings.charAt(i)] = t;
                    }
                }
            }
        }

        static LEXEME map(int ch) {
            return map[ch];
        }
    };

    static JValue parse(final Reader doc) throws IOException {

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

                private void read(int i) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                        }
                        sb.append("\\");
                        ch = reader.read();
                    }
                    sb.append((char) ch);
                }
                value = sb.toString();
            }

            private int unicode() throws IOException {
                int total = 0;
                for (int i = 3; i > -1; i--) {
                    int ch = reader.read();
                    if (ch == -1) {
                        throw new IOException("End of document reached while parsing a unicode escape sequence.");
                    }
                    if (ch >= '0' && ch <= '9') {
                        ch -= '0';
                    } else {
                        ch = ch | 0x20;
                        if (ch >= 'A' && ch <= 'F') {
                            ch -= 'A';
                        }
                    }
                    if (ch < 0x0 || ch > 0xf) {
                        throw new IOException("Invalid character in unicode escape sequence.");
                    }
                    total += (ch << i * 4);
                }
                return total;
            }

            void nextToken() throws IOException {
                int ch = reader.read();

                if (ch == -1) {
                    token = EOD;
                    return;
                }

                token = LEXEME.map(ch);

                switch (token) {
                    case FALSE:
                        reader.read();
                    case NULL:
                    case TRUE:
                        reader.read();
                        reader.read();
                        reader.read();
                        break;
                    case NUMBER:
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
                }

                // Consume trailing whitespace
                consumeWhitepace();
            }

            void match(LEXEME nextToken) throws IOException {
                if (nextToken == STRING) { // This is forced.
                    nextString();
                    token = STRING;
                } else {
                    nextToken();
                    if (token != nextToken) {
                        throw new IOException("Expected '" + nextToken + "', but got '" + token + "'.");
                    }
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
                return LEXEME.map(reader.lookAhead());
            }

            JsonObject object() throws IOException {
                JsonObject top = new JsonObject();

                out:
                for (;;) {
                    // Process key.
                    match(QUOTE);
                    match(STRING);
                    String key = value;
                    match(QUOTE);
                    match(COLON);
                    nextToken();

                    // Process value.
                    switch (token) {
                        case TRUE:
                            top.put(key, true);
                            break;
                        case FALSE:
                            top.put(key, false);
                            break;
                        case QUOTE:
                            match(STRING);
                            top.put(key, value);
                            match(QUOTE);
                            break;
                        case L_BRACKET:
                            top.put(key, array());
                            match(R_BRACKET);
                            break;
                        case L_BRACE:
                            top.put(key, object());
                            match(R_BRACE);
                            break;
                        case NULL:
                            top.put(key, new JsonObject());
                            break;
                        case NUMBER:
                            if (isDecimal) {
                                top.put(key, Double.parseDouble(value));
                            } else {
                                top.put(key, Long.parseLong(value));
                            }
                            break;
                        default:
                            // Should never get here.
                            throw new IOException("Invalid token: " + token);
                    }

                    if (lookAhead() != COMMA) {
                        break;
                    }

                    match(COMMA);
                }
                return top;
            }

            private JsonArray array() throws IOException {
                List list = new ArrayList<>();

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
                            list.add(array());
                            match(R_BRACKET);
                            break;
                        case L_BRACE: // Embedded object
                            match(L_BRACE);
                            list.add(object());
                            match(R_BRACE);
                            break;
                        case TRUE:
                            match(TRUE);
                            list.add(true);
                            break;
                        case FALSE:
                            match(FALSE);
                            list.add(false);
                            break;
                        case NULL:
                            match(NULL);
                            list.add(null);
                            break;
                        case NUMBER:
                            match(NUMBER);
                            if (isDecimal) {
                                list.add(Double.parseDouble(value));
                            } else {
                                list.add(Long.parseLong(value));
                            }
                            break;
                        case COMMA:
                            match(COMMA);
                            break;
                        default:
                            break out;
                    }
                }
                return new JsonArray(list);
            }
        }

        Lexer lexer = new Lexer();
        // Start with lexer at BOD;

        JValue ret;
        // Do we have an object or an array?
        if (lexer.lookAhead() == L_BRACE) {
            lexer.match(L_BRACE);
            ret = lexer.object();
            lexer.match(R_BRACE);
        } else if (lexer.lookAhead() == L_BRACKET) {
            lexer.match(L_BRACKET);
            ret = lexer.array();
            lexer.match(R_BRACKET);
        } else {
            throw new IOException("Can't parse JSON document.  Incorrect first token: " + lexer.lookAhead() + ".");
        }
        return ret;
    }
}
