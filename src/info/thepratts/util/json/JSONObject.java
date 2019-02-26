/*
 * MIT License
 *
 * Copyright (c) 2015 Ken Pratt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package info.thepratts.util.json;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import static info.thepratts.util.json.JSONObject.LEXEME.BOD;
import static info.thepratts.util.json.JSONObject.LEXEME.COLON;
import static info.thepratts.util.json.JSONObject.LEXEME.COMMA;
import static info.thepratts.util.json.JSONObject.LEXEME.EOD;
import static info.thepratts.util.json.JSONObject.LEXEME.FALSE;
import static info.thepratts.util.json.JSONObject.LEXEME.L_BRACE;
import static info.thepratts.util.json.JSONObject.LEXEME.L_BRACKET;
import static info.thepratts.util.json.JSONObject.LEXEME.NULL;
import static info.thepratts.util.json.JSONObject.LEXEME.NUMBER;
import static info.thepratts.util.json.JSONObject.LEXEME.QUOTE;
import static info.thepratts.util.json.JSONObject.LEXEME.R_BRACE;
import static info.thepratts.util.json.JSONObject.LEXEME.R_BRACKET;
import static info.thepratts.util.json.JSONObject.LEXEME.STRING;
import static info.thepratts.util.json.JSONObject.LEXEME.TRUE;
import java.util.Set;

/**
 *
 * @author Ken Pratt
 */
public class JSONObject {

    final HashMap<String, Object> map = new HashMap<>();

    public void put(String key, Object obj) {
        map.put(key, obj);
    }

//    public void put(String key, List<? extends Object> values) {
//        map.put(key, new JSONArray(values));
//    }
//
//    public void put(String key, String value) {
//        map.put(key, new JSONEscaped(value));
//    }
//
//    public void put(String key, boolean value) {
//        map.put(key, new JSONNotQuoted(value));
//    }
//
//    public void put(String key, Number value) {
//        map.put(key, new JSONNotQuoted(value));
//    }
//
//    public void put(String key, Instant time) {
//        map.put(key, new JSONNotEscaped(time));
//    }
//
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    private <T> T walks(String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            if (!map.containsKey(path)) {
                throw new IllegalArgumentException("Key does not exist.");
            }
            return (T) map.get(path);
        } else {
            T j = (T) map.get(path.substring(0, index));
            if (j == null) {
                throw new IllegalArgumentException("Key does not exist.");
            }
            if (!(j instanceof JSONObject)) {
                throw new IllegalArgumentException("Key's value is not a JSONObject.");
            }
            return ((JSONObject) j).walks(path.substring(index + 1));
        }
    }

//    public String getAsString(String key) {
//        return walks(key).toString();
//    }
//
//    public Instant getAsInstant(String key) {
//        JValue ret = walks(key);
//        if (ret.getValue() instanceof Instant) {
//            return (Instant) ret.getValue();
//        } else {
//            return Instant.parse(ret.getValue().toString());
//        }
//    }
//
//    public Long getAsInt(String key) {
//        JValue ret = walks(key);
//        if (ret.getValue() instanceof Double) {
//            return (Long) ret.getValue();
//        } else {
//            return Long.parseLong(ret.getValue().toString());
//        }
//    }
//
//    public Double getAsFloat(String key) {
//        JValue ret = walks(key);
//        if (ret.getValue() instanceof Double) {
//            return (Double) ret.getValue();
//        } else {
//            return Double.parseDouble(ret.getValue().toString());
//        }
//    }
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> "\"" + e.getKey() + "\":"
                + (e.getValue() == null ? "null" : e.getValue() instanceof String ? "\"" + e.getValue() + "\"" : e.getValue().toString())).collect(Collectors.joining(",", "{", "}"));
    }

    private void insertSpaces(StringBuilder sb, int numSpaces) {
        for (int i = 0; i < numSpaces; i++) {
            sb.append(" ");
        }
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

    public static <T> T parse(final Reader doc) throws IOException {

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

//                private void read(int i) {
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                }
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

            JSONObject object() throws IOException {
                JSONObject top = new JSONObject();

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
                            top.put(key, null);
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

            private JSONArray array() throws IOException {
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
                return new JSONArray(list);
            }
        }

        Lexer lexer = new Lexer();
        // Start with lexer at BOD;

        T ret;
        if (null == lexer.lookAhead()) {
            throw new IOException("Can't parse JSON document.  Incorrect first token: " + lexer.lookAhead() + ".");
        } else // Do we have an object or an array?
        {
            switch (lexer.lookAhead()) {
                case L_BRACE:
                    lexer.match(L_BRACE);
                    ret = (T) lexer.object();
                    lexer.match(R_BRACE);
                    break;
                case L_BRACKET:
                    lexer.match(L_BRACKET);
                    ret = (T) lexer.array();
                    lexer.match(R_BRACKET);
                    break;
                default:
                    throw new IOException("Can't parse JSON document.  Incorrect first token: " + lexer.lookAhead() + ".");
            }
        }
        return ret;
    }
}
