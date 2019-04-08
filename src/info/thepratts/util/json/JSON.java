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

import static info.thepratts.util.json.JSON.LEXEME.BOD;
import static info.thepratts.util.json.JSON.LEXEME.COLON;
import static info.thepratts.util.json.JSON.LEXEME.COMMA;
import static info.thepratts.util.json.JSON.LEXEME.EOD;
import static info.thepratts.util.json.JSON.LEXEME.L_BRACE;
import static info.thepratts.util.json.JSON.LEXEME.L_BRACKET;
import static info.thepratts.util.json.JSON.LEXEME.NULL;
import static info.thepratts.util.json.JSON.LEXEME.NUMBER;
import static info.thepratts.util.json.JSON.LEXEME.R_BRACE;
import static info.thepratts.util.json.JSON.LEXEME.R_BRACKET;
import static info.thepratts.util.json.JSON.LEXEME.STRING;
import static info.thepratts.util.json.JSON.LEXEME.TRUE;
import static info.thepratts.util.json.JSON.LEXEME.FALSE;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

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
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                default:
                    if (Character.isISOControl(v)) {
                        sb.append(String.format("\\u%04x", (int) v));
                    } else {
                        sb.append(v);
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
    };

    public static JSONStream objectsFrom(final BufferedReader data) throws IOException {
        return new JSONStream(data);
    }

    public static <T> T from(final String data) throws IOException {
        return from(new StringReader(data));
    }

    public static <T> T from(final Reader data) throws IOException {

        class Lexer {

            int ch;
            Object value;
            StringBuilder sb = new StringBuilder();
            LEXEME token = BOD;

            Lexer() throws IOException {
                ch = data.read();
            }

            /**
             * Consume the entire token. 'token' is left with the token type and
             * value its 'value' if it is a string or a number. If the next
             * token is not 'expected', then throw an exception with the given
             * message.
             */
            void nextToken(final LEXEME expected, final String message) throws IOException {
                nextToken();
                if (token != expected) {
                    throw new IOException(message);
                }
            }

            /**
             * Consume the entire token. 'token' is left with the token type and
             * value its 'value' if it is a string or a number.
             */
            void nextToken() throws IOException {
                // Eat up whitespace
                consumeWhitepace();

                if (ch == -1) {
                    token = EOD;
                    return;
                }

                token = LEXEME.map(ch);

                if (token == null) {
                    throw new IOException("Invalid character '" + (char) ch + "' encountered while searching for next token.");
                }

                switch (token) {
                    case FALSE: // Order and lack of break stmt are correct here. This set up to consume proper # of characters.
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

            void consumeWhitepace() throws IOException {
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
                        case COMMA:
                            nextToken();
                            break; // more objects
                        case R_BRACE:
                            return top; // all done
                    }

                    if (token != STRING) {
                        throw new IOException("Expected a key name in quotes but got '" + token + "'.");
                    }

                    String key = (String) value;
                    nextToken(COLON, "Missing ':'.");
                    nextToken();

                    // Process value.
                    switch (token) {
                        case TRUE:
                            top.put(key, true);
                            break;
                        case FALSE:
                            top.put(key, false);
                            break;
                        case STRING:
                        case NUMBER:
                            top.put(key, value);
                            break;
                        case L_BRACKET: // []
                            top.put(key, array());
                            break;
                        case L_BRACE: // {}
                            top.put(key, object());
                            break;
                        case NULL:
                            top.put(key, null);
                            break;
                        default:
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
            case L_BRACE: // {}
                ret = (T) lexer.object();
                break;
            case L_BRACKET: // []
                ret = (T) lexer.array();
                break;
            default:
                throw new IOException("Can't parse JSON document. Must start with '{' or '['.");
        }

        return ret;
    }

    protected static void _indent(final StringBuilder sb, int numSpaces) {
        while (numSpaces-- > 0) {
            sb.append(' ');
        }
    }

}
