import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DESEngine {
    private String content;
    private ScriptEngine engine;

    public DESEngine() {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("javascript");
            content = readFileContent("src/des.js");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encode(String data, String firstKey, String secondKey, String thirdKey) {
        try {
            String ops = String.format("strEnc('%s' , '%s' , '%s' , '%s')", data, firstKey, secondKey, thirdKey);
            return (String) engine.eval(content + "\n" + ops);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decode(String data, String firstKey, String secondKey, String thirdKey) {
        try {
            String ops = String.format("strDec('%s' , '%s' , '%s' , '%s')", data, firstKey, secondKey, thirdKey);
            return (String) engine.eval(content + "\n" + ops);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String readFileContent(String path) {
        StringBuilder content = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(path));
            while (sc.hasNextLine()) {
                content.append(sc.nextLine());
                content.append('\n');
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
        return content.toString();
    }

}
