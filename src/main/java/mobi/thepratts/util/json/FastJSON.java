package mobi.thepratts.util.json;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author kpratt
 */
public class FastJSON {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        System.out.println("Output!");

        JsonObject top = JsonObject.parse(new FileReader("sample1.json"));

        System.out.println(top);

        System.out.println(top.getAsString("people.age"));

        System.out.println((List) top.get("people.children"));
    }
}
