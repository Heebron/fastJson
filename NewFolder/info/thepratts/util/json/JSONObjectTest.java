/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.thepratts.util.json;

import java.io.FileReader;
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
public class JSONObjectTest {

    public JSONObjectTest() {
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
    public void testParse_Reader_empty_string() throws Exception {
        JSONObject doc = JSONObject.from("{\"name\":\"\"}");
        assertEquals("", doc.get("name"));
    }

    @Test
    public void testParse_Reader_non_empty_string() throws Exception {
        JSONObject doc = JSONObject.from("{\"name\":\"Fred\"}");
        assertEquals("Fred", doc.get("name"));
    }

    @Test
    public void testParse_Reader_null_string() throws Exception {
        JSONObject doc = JSONObject.from("{\"name\":null}");
        assertNull(doc.get("name"));
    }

    @Test
    public void testParse_Reader_double() throws Exception {
        JSONObject doc = JSONObject.from("{\"value1\":12.50,\"value2\":-12.50}");
        assertEquals(Double.valueOf(12.50), doc.get("value1"));
        assertEquals(Double.valueOf(-12.50), doc.get("value2"));
    }

    @Test
    public void testParse_Reader_boolean() throws Exception {
        JSONObject doc = JSONObject.from("{\"value1\":true,\"value2\":false}");
        assertTrue(doc.get("value1"));
        assertFalse(doc.get("value2"));
    }

    @Test
    public void testParse_Reader_long() throws Exception {
        JSONObject doc = JSONObject.from("{\"value1\":-112,\"value2\":112}");
        assertEquals(Long.valueOf(-112), doc.get("value1"));
        assertEquals(Long.valueOf(112), doc.get("value2"));
    }

    @Test
    public void testParseFile03() throws Exception {
        JSONObject doc = JSONObject.from(new FileReader("tests/test-03.json"));
    }

    @Test
    public void testParseFile01() throws Exception {
        JSONObject doc = JSONObject.from(new FileReader("tests/test-01.json"));
    }

    @Test
    public void testParseFile02() throws Exception {
        JSONObject doc = JSONObject.from(new FileReader("tests/test-02.json"));
    }

    @Test
    public void testParseFile04() throws Exception {
        JSONArray doc = JSONObject.from(new FileReader("tests/test-04.json"));
    }

    @Test
    public void testParseFile05() throws Exception {
        JSONArray doc = JSONObject.from(new FileReader("tests/test-05.json"));
        assertEquals("REPETWIRE", ((JSONObject) doc.get(0)).get("company"));
        assertEquals("REPETWIRE", doc.getJSONObject(0).get("company"));

        assertEquals("Delores", doc.getJSONObject(1).get("name", "first"));
        assertEquals("est", doc.getJSONObject(1).getJSONArray("tags").get(3));
        assertEquals(3L, doc.getJSONObject(2).getJSONArray("range").get(3));
        assertEquals("Tammi Davis", doc.getJSONObject(2).getJSONArray("friends").getJSONObject(1).get("name"));
    }

    @Test
    public void testOptional() throws Exception {
        JSONObject doc = JSONObject.from("{\"value1\":true,\"value2\":false}");
        assertTrue(doc.get("value1"));
        assertFalse(doc.get("value2"));
        assertEquals("fred", doc.opt("asd").orElse("fred"));
        assertEquals(true, doc.opt("value1").orElse("fred"));
    }

    @Test
    public void empty() throws Exception {
        JSONObject doc = JSONObject.from("{}");
        assertTrue(doc.isEmpty());
    }

    @Test
    public void testParseFileSample() throws Exception {
        JSONObject doc = JSONObject.from(new FileReader("tests/sample.json"));
        String v =doc.toString(2);
        System.out.println(v);
    }

}
