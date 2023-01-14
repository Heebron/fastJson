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

import java.io.*;

import static info.thepratts.util.json.JSON.LEXEME.*;

/**
 *
 * @author Ken Pratt &lt;kenpratt@comcast.net&gt;
 */
public class JSON {

    static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char v = value.charAt(i);
            switch (v) {
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\t' -> sb.append("\\t");
                case '"' -> sb.append("\\\"");
                default -> {
                    if (Character.isISOControl(v)) {
                        sb.append(String.format("\\u%04x", (int) v));
                    } else {
                        sb.append(v);
                    }
                }
            }
        }
        return sb.toString();
    }

    enum LEXEME {

        // The tokens of JSON
        BOD,
        EOD,
        L_BRACKET('['),
        R_BRACKET(']'),
        L_BRACE('{'),
        R_BRACE('}'),
        COLON(':'),
        STRING('"'),
        COMMA(','),
        TRUE("Tt"),
        FALSE("Ff"),
        NULL("Nn"),
        NUMBER("-+0123456789.");

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
    }

    public static JSONStream objectsFrom(final Reader data) {
        return new JSONStream(data);
    }

    public static <T> T from(final String data) throws IOException {
        return from(new StringReader(data));
    }

    public static <T> T from(final InputStream data) throws IOException {
        return from(new InputStreamReader(data));
    }

    @SuppressWarnings("unchecked")
    public static <T> T from(final Reader data) throws IOException {

        class Lexer {

            int ch;
            Object value;
            final StringBuilder sb = new StringBuilder();
            LEXEME token = BOD;

            Lexer() throws IOException {
                ch = data.read();
            }

            /**
             * Consume the entire token. 'token' is left with the token type and
             * value its 'value' if it is a string or a number. If the next
             * token is not 'expected', then throw an exception with note.
             */
            void nextTokenColonCheck() throws IOException {
                nextToken();
                if (token != LEXEME.COLON) {
                    throw new IOException("Missing ':'.");
                }
            }

            /**
             * Consume the entire token. 'token' is left with the token type and
             * value its 'value' if it is a string or a number.
             */
            void nextToken() throws IOException {
                // Eat up whitespace
                consumeWhitespace();

                if (ch == -1) {
                    token = EOD;
                    return;
                }

                token = LEXEME.map(ch);

                if (token == null) {
                    throw new IOException("Invalid character '" + (char) ch + "' encountered while searching for next token.");
                }

                switch (token) {
                    case FALSE: // Order and lack of break stmt are correct here. This is set up to consume proper # of characters.
                        data.read();
                    case TRUE:
                    case NULL:
                        data.read();
                        data.read();
                        data.read();
                        ch = data.read();
                        break;
                    case NUMBER:
                        sb.setLength(0);
                        boolean isDecimal = false;
                        // index 0
                        if (ch == '-' || ch == '+' || Character.isDigit(ch)) {
                            sb.append((char) ch);
                        } else {
                            throw new IOException("Expected a number, got " + (char) ch + " instead.");
                        }

                        // Beyond index 0
                        for (;;) {
                            ch = data.read();
                            if (Character.isDigit(ch)) {
                                sb.append((char) ch);
                            } else if (ch == '.'
                                    || ch == 'e'
                                    || ch == '+'
                                    || ch == '-'
                                    || ch == 'E') {
                                sb.append((char) ch);
                                isDecimal = true;
                            } else {
                                break;
                            }
                        }

                        if (isDecimal) {
                            value = Double.valueOf(sb.toString());
                        } else {
                            value = Long.valueOf(sb.toString());
                        }

                        break;
                    case STRING: // Quoted String
                        sb.setLength(0);
                        ch = data.read();
                        // Look for end quote
                        while (ch != '"') {
                            if (ch == -1) {
                                throw new IOException("End of document reached in string.");
                            }
                            if (ch == '\\') {
                                sb.append("\\");
                                ch = data.read();
                                if (ch == -1) {
                                    throw new IOException("End of document reached in string.");
                                }
                            }
                            sb.append((char) ch);
                            ch = data.read();
                        }
                        value = sb.toString();
                        ch = data.read();
                        break;
                    case L_BRACE:
                    case L_BRACKET:
                    case R_BRACKET:
                    case R_BRACE:
                    case COLON:
                    case COMMA:
                        ch = data.read();
                        break;
                    default:
                        throw new IOException("Unexpected token " + token);
                }

            }

            void consumeWhitespace() throws IOException {
                while (Character.isWhitespace(ch)) {
                    ch = data.read();
                }
            }

            JSONObject object() throws IOException {
                JSONObject top = new JSONObject();

                for (;;) {
                    // Process key.
                    nextToken();

                    switch (token) {
                        case COMMA -> nextToken();
                        // more objects
                        case R_BRACE -> {
                            return top; // all done
                        }
                    }

                    if (token != STRING) {
                        throw new IOException("Expected a key name in quotes but got '" + token + "'.");
                    }

                    String key = (String) value;
                    nextTokenColonCheck();
                    nextToken();

                    // Process value.
                    switch (token) {
                        case TRUE -> top.put(key, true);
                        case FALSE -> top.put(key, false);
                        case STRING, NUMBER -> top.put(key, value);
                        case L_BRACKET -> // []
                                top.put(key, array());
                        case L_BRACE -> // {}
                                top.put(key, object());
                        case NULL -> top.put(key, null);
                        default ->
                            // Should never get here.
                                throw new IOException("Invalid token: " + token);
                    }
                }
            }

            private JSONArray array() throws IOException {
                JSONArray list = new JSONArray();

                for (;;) {
                    // grab the next token
                    nextToken();
                    switch (token) {
                        case STRING:
                        case NUMBER:
                            list.add(value);
                            break;
                        case L_BRACKET: // Embedded array []
                            list.add(array());
                            break;
                        case L_BRACE: // Embedded object {}
                            list.add(object());
                            break;
                        case TRUE:
                            list.add(true);
                            break;
                        case FALSE:
                            list.add(false);
                            break;
                        case NULL:
                            list.add(null);
                            break;
                        case COMMA:
                            break;
                        case R_BRACKET:
                            return list;
                        default:
                            throw new IOException("Unexpected token '" + token + "' encountered while processing an array.");
                    }
                }
            }
        }

        Lexer lexer = new Lexer();
        // Start with lexer at beginning of data (BOD)

        T ret;

        lexer.nextToken();
        // Do we have an object or an array?
        switch (lexer.token) {
            case L_BRACE -> // {}
                    ret = (T) lexer.object();
            case L_BRACKET -> // []
                    ret = (T) lexer.array();
            case EOD -> {
                return null;
            }
            default -> throw new IOException("Can't parse JSON document. Must start with '{' or '['.");
        }
        return ret;
    }

    protected static void _indent(final StringBuilder sb, int numSpaces) {
        while (numSpaces-- > 0) {
            sb.append(' ');
        }
    }
}
