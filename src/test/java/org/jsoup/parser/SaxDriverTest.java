package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Test for SaxDriver / SaxEventListener
 */
public class SaxDriverTest {

    String simpleHTML = "<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>";

    @Test
    public void startParse() {
        final SaxDriver driver = new SaxDriver(simpleHTML);
        driver.addListener(new SaxEventListener.NopSaxEventListener());
        driver.start();
    }

    @Test
    public void saveAllTokens() {
        final SaxDriver driver = new SaxDriver(simpleHTML);

        final ArrayList<String> strings = new ArrayList<String>();
        driver.addListener(new SaxEventListener() {
            public void onStartTag(Token.StartTag token) {
                strings.add(token.toString());
            }

            public void onEndTag(Token.EndTag token) {
                strings.add(token.toString());
            }

            public void onDocType(Token.Doctype token) {
                strings.add(token.toString());
            }

            public void onComment(Token.Comment token) {
                strings.add(token.toString());
            }

            public void onCharacter(Token.Character token) {
                strings.add(token.toString());
            }

            public void onEOF(Token.EOF token) {
                strings.add(token.toString());
            }
        });
        driver.start();
    }

    @Test(expected = SaxHtmlElementsMatcher.SaxHtmlMatcherException.class)
    public void rejectEmptySelector1() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create(" \t\r\n");
    }

    @Test
    public void acceptTagSelector() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create(" > html \t\r>   \nbody > div > br\t\r");
    }

    @Test(expected = SaxHtmlElementsMatcher.SaxHtmlMatcherException.class)
    public void rejectIncorrectSelector1() {
        // should start with >
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create(" html > body > div > br\t\r");
    }

    @Test(expected = SaxHtmlElementsMatcher.SaxHtmlMatcherException.class)
    public void rejectIncorrectSelector2() {
        // should start with >
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create("> html >> body > div > br\t\r");
    }

    @Test
    public void acceptComplicatedSelector() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create(" > html \t\r   >   \nbody.ho > div#main-content > br\t\r");
    }

    @Test
    public void matchesStartEndTag() {
        Attributes idB = new Attributes();
        idB.put("id", "id-B");

        ParseErrorList errors = ParseErrorList.tracking(100);
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.create(">a     >   b#id-B", errors);
        assertEquals(0, path.matchedDepth);

        path.onStartTag((Token.StartTag) new Token.StartTag().name("a"));
        assertEquals(1, path.matchedDepth);

        path.onStartTag((Token.StartTag) new Token.StartTag().name("b"));
        assertEquals(1, path.matchedDepth);
        assertEquals(0, errors.size());

        path.onEndTag((Token.EndTag) new Token.EndTag().name("b"));
        assertEquals(1, path.matchedDepth);
        assertEquals(0, errors.size());

        path.onStartTag((Token.StartTag) new Token.StartTag().nameAttr("b", idB));
        assertEquals(2, path.matchedDepth);
        assertEquals(0, errors.size());

        // error 1: try to pop "a" when stack is ["a", "b"]
        path.onEndTag((Token.EndTag) new Token.EndTag().name("a"));
        assertEquals(2, path.matchedDepth);
        assertEquals(1, errors.size());

        path.onEndTag((Token.EndTag) new Token.EndTag().name("b"));
        assertEquals(1, path.matchedDepth);
        assertEquals(1, errors.size());

        path.onEndTag((Token.EndTag) new Token.EndTag().name("a"));
        assertEquals(0, path.matchedDepth);
        assertEquals(1, errors.size());

        // error 2: try to pop when stack is empty
        path.onEndTag((Token.EndTag) new Token.EndTag().name("b"));
        assertEquals(0, path.matchedDepth);
        assertEquals(2, errors.size());
    }
}
