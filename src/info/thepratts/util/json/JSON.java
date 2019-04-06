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
import static info.thepratts.util.json.JSON.LEXEME.QUOTE;
import static info.thepratts.util.json.JSON.LEXEME.R_BRACE;
import static info.thepratts.util.json.JSON.LEXEME.R_BRACKET;
import static info.thepratts.util.json.JSON.LEXEME.STRING;
import static info.thepratts.util.json.JSON.LEXEME.TRUE;
import static info.thepratts.util.json.JSON.LEXEME.FALSE;
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

    public static <T> T from(String data) throws IOException {
        return from(new StringReader(data));
    }

    public static <T> T from(InputStream data) throws IOException {
        return from(new InputStreamReader(data));
    }

    public static <T> T from(final Reader data) throws IOException {

        class Lexer {

            class ReadAhead {

                int next;

                ReadAhead() throws IOException {
                    next = data.read();
                }

                int read() throws IOException {
                    int ret = next;
                    next = data.read();
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
                // Look for end quote
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

            void nextToken() throws IOException {
                int ch = reader.read();

                if (ch == -1) {
                    token = EOD;
                    return;
                }

                token = LEXEME.map(ch);

                switch (token) {
                    case FALSE: // Order and lack of break stmt are correct here.
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

                // Is it an empry object?
                if (lookAhead() == R_BRACE) {
                    return top;
                }

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
                                top.put(key, Double.valueOf(value));
                            } else {
                                top.put(key, Long.valueOf(value));
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
                JSONArray list = new JSONArray();

                if (lookAhead() == R_BRACE) {
                    return list;
                }

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
                                list.add(Double.valueOf(value));
                            } else {
                                list.add(Long.valueOf(value));
                            }
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
    protected static void _indent(StringBuilder sb, int numSpaces) {
        while (numSpaces-- > 0) {
            sb.append(' ');
        }
    }

}
