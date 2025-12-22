import io.megabrain.core.CodeAwareAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DebugAnalyzer {
    public static void main(String[] args) throws IOException {
        CodeAwareAnalyzer analyzer = new CodeAwareAnalyzer();
        System.out.println("Testing 'getUserById123':");
        printTokens(analyzer, "getUserById123");
        System.out.println("Testing 'XMLParser':");
        printTokens(analyzer, "XMLParser");
        System.out.println("Testing 'get_user_by_id':");
        printTokens(analyzer, "get_user_by_id");
        System.out.println("Testing 'UserService':");
        printTokens(analyzer, "UserService");
        System.out.println("Testing 'aBCdEF':");
        printTokens(analyzer, "aBCdEF");
        analyzer.close();
    }

    static void printTokens(CodeAwareAnalyzer analyzer, String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("test", text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                tokens.add(term.toString());
            }
            ts.end();
        }
        System.out.println("  Tokens: " + tokens);
    }
}
