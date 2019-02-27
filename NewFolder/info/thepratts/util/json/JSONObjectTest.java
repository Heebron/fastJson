/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.thepratts.util.json;

import java.io.FileReader;
import java.io.Reader;
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
        JSONObject doc = JSONObject.parse("{\"name\":\"\"}");
        assertEquals("", doc.get("name"));
    }

    @Test
    public void testParse_Reader_non_empty_string() throws Exception {
        JSONObject doc = JSONObject.parse("{\"name\":\"Fred\"}");
        assertEquals("Fred", doc.get("name"));
    }

    @Test
    public void testParse_Reader_null_string() throws Exception {
        JSONObject doc = JSONObject.parse("{\"name\":null}");
        assertNull(doc.get("name"));
    }

    @Test
    public void testParse_Reader_double() throws Exception {
        JSONObject doc = JSONObject.parse("{\"value1\":12.50,\"value2\":-12.50}");
        assertEquals(Double.valueOf(12.50), doc.get("value1"));
        assertEquals(Double.valueOf(-12.50), doc.get("value2"));
    }

    @Test
    public void testParse_Reader_boolean() throws Exception {
        System.out.println("testParse_Reader_boolean");
        JSONObject doc = JSONObject.parse("{\"value1\":true,\"value2\":false}");
        assertTrue(doc.get("value1"));
        assertFalse(doc.get("value2"));
    }

    @Test
    public void testParse_Reader_long() throws Exception {
        JSONObject doc = JSONObject.parse("{\"value1\":-112,\"value2\":112}");
        assertEquals(Long.valueOf(-112), doc.get("value1"));
        assertEquals(Long.valueOf(112), doc.get("value2"));
    }

    @Test
    public void testParseFile03() throws Exception {
        System.out.println("Read File");
        JSONObject doc = JSONObject.parse(new FileReader("tests/test-03.json"));
    }
}
