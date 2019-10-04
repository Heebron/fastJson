/*
 * The MIT License
 *
 * Copyright 2019 kpratt.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.thepratts.util.json;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.Function;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kpratt
 */
public class JSONTest {

    public JSONTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTwoObjInArray() throws IOException {
        JSONObject doc = JSON.from("{\"a\":[{},{}]}");
        assertEquals(2, ((JSONArray) doc.get("a")).size());
    }

    @Test
    public void testParse_Reader_empty_string() throws Exception {
        JSONObject doc = JSON.from("{\"name\":\"\"}");
        assertEquals("", doc.get("name"));
    }

    @Test
    public void testParse_Reader_non_empty_string() throws Exception {
        JSONObject doc = JSON.from("{\"name\":\"Fred\"}");
        assertEquals("Fred", doc.get("name"));
    }

    @Test
    public void testParse_Reader_null_string() throws Exception {
        JSONObject doc = JSON.from("{\"name\":null}");
        assertNull(doc.get("name"));
    }

    @Test
    public void testParse_Reader_double() throws Exception {
        JSONObject doc = JSON.from("{\"value1\":12.50,\"value2\":-12.50}");
        assertEquals(Double.valueOf(12.50), doc.get("value1"));
        assertEquals(Double.valueOf(-12.50), doc.get("value2"));
    }

    @Test
    public void testParse_Reader_boolean() throws Exception {
        JSONObject doc = JSON.from("{\"value1\":true,\"value2\":false}");
        assertTrue(doc.get("value1"));
        assertFalse(doc.get("value2"));
    }

    @Test
    public void testParse_Reader_long() throws Exception {
        JSONObject doc = JSON.from("{\"value1\":-112,\"value2\":112}");
        assertEquals(Long.valueOf(-112), doc.get("value1"));
        assertEquals(Long.valueOf(112), doc.get("value2"));
    }

    @Test
    public void testParseFile03() throws Exception {
        JSONObject doc = JSON.from(new FileReader("samples/test-03.json"));
    }

    @Test
    public void testParseFile01() throws Exception {
        JSONObject doc = JSON.from(new FileReader("samples/test-01.json"));
    }

    @Test
    public void testParseFile02() throws Exception {
        JSONObject doc = JSON.from(new FileReader("samples/test-02.json"));
    }

    @Test
    public void testParseFile04() throws Exception {
        JSONArray doc = JSON.from(new FileReader("samples/test-04.json"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParseFile05() throws Exception {
        JSONArray<JSONObject> doc = JSON.from(new FileReader("samples/test-05.json"));
        assertEquals("REPETWIRE", ((JSONObject) doc.get(0)).get("company"));
        assertEquals("REPETWIRE", doc.get(0).get("company"));
        assertEquals("Delores", doc.get(1).get("name", "first"));
        assertEquals("est", doc.get(1).getJSONArray("tags").get(3));
        assertEquals(3L, doc.get(2).getJSONArray("range").get(3));
        assertEquals("Tammi Davis", ((JSONArray<JSONObject>) doc.get(2).get("friends")).get(1).get("name"));
    }

    @Test
    public void testOptional() throws Exception {
        JSONObject doc = JSON.from("{\"value1\":true,\"value2\":false}");
        assertTrue(doc.get("value1"));
        assertFalse(doc.get("value2"));
        assertEquals("fred", doc.opt("asd").orElse("fred"));
        assertEquals(true, doc.opt("value1").orElse("fred"));
    }

    @Test
    public void empty() throws Exception {
        JSONObject doc = JSON.from("{}");
        assertTrue(doc.isEmpty());
    }

    @Test
    public void testParseFileSample() throws Exception {
        JSONObject doc = JSON.from(new FileReader("samples/sample.json"));
    }

    @Test
    public void testEmpty01() throws IOException {
        JSONObject doc1 = JSON.from("");
        assertNull(doc1);
    }

    @Test
    public void testEmpty02() throws IOException {
        JSONObject doc2 = JSON.from("     ");
        assertNull(doc2);
    }

    @Test
    public void testBadStart() {
        try {
            JSONObject doc2 = JSON.from(";");
            fail("Should not get here!");
        } catch (IOException ex) {
            // expected
        }
    }

    @Test
    public void utfTest01() throws IOException {
        JSONObject doc = JSON.from("{\"a\":\"Horníková\"}");
        assertEquals("Horníková", doc.get("a"));
    }

    @Test
    public void streamTest01() throws IOException {
        JSONStream i = JSON.objectsFrom(new StringReader("{\"first\":true}"));
        Object obj1 = i.next();
        assertTrue(obj1 instanceof JSONObject);
        assertTrue(((JSONObject) obj1).get("first"));
        assertNull(i.next());
    }

    @Test
    public void streamTest02() throws IOException {
        JSONStream i = JSON.objectsFrom(new StringReader("{\"first\":true}{\"first\":false}"));
        JSONObject obj1 = i.next();
        assertTrue(((JSONObject) obj1).get("first"));
        obj1 = i.next();
        assertFalse(((JSONObject) obj1).get("first"));
        assertNull(i.next());
    }

    @Test
    public void streamTest03() throws IOException {
        JSONStream i = JSON.objectsFrom(new StringReader("  {  \"first\":true}     {\"first\":false}"));
        JSONObject obj1 = i.next();
        assertTrue(((JSONObject) obj1).get("first"));
        obj1 = i.next();
        assertFalse(((JSONObject) obj1).get("first"));
        assertNull(i.next());
    }

    @Test
    public void streamTest04() throws IOException {
        JSONStream i = JSON.objectsFrom(new StringReader(""));
        assertNull(i.next());
    }

    @Test
    public void streamTest05() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        append(new FileReader("samples/test-01.json"), out);
        append(new FileReader("samples/test-02.json"), out);
        append(new FileReader("samples/test-03.json"), out);
        append(new FileReader("samples/sample.json"), out);
        JSONStream i = JSON.objectsFrom(new StringReader(out.toString()));
        JSONObject obj = i.next();
        assertTrue((Long) (((JSONObject) ((JSONArray) obj.get("x", "inputs")).get(0)).get("sequence")) == 4294967293l);

        obj = i.next();
        assertEquals(obj.get("name"), "");

        obj = i.next();
        assertEquals(obj.get("op"), "utx");

        obj = i.next();
        assertEquals(((JSONObject) ((JSONArray) obj.get("transactions")).get(3)).get("tx_hash"), "375b279a3cef235127ea74e6014bc7ced9a32f9175fa8cb4f9e47e0f11942aa2");

        assertNull(i.next());
    }

    Function<Number, Integer> asInt = v -> ((Number) v).intValue();

    @Test
    public void testNumberInt() throws IOException {
        JSONObject obj = JSON.from("{\"number\":1234}");

        int value = obj.opt("number").map(v -> ((Number) v).intValue()).get();

        assertEquals(1234, value);
    }

    @Test
    public void testNumberLong() throws IOException {
        JSONObject obj = JSON.from("{\"number\":1234}");

        long value = obj.opt("number").map(v -> ((Number) v).longValue()).get();

        assertEquals(1234, value);
    }

    private void append(FileReader fileReader, ByteArrayOutputStream out) throws IOException {
        int ch;
        while (-1 != (ch = fileReader.read())) {
            out.write(ch);
        }
    }
}
