package org.jsoup.parser;

import org.junit.Test;

import java.util.ArrayList;

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
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.parse(" \t\r\n");
    }

    @Test
    public void acceptTagSelector() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.parse(" > html \t\r>   \nbody > div > br\t\r");
    }

    @Test(expected = SaxHtmlElementsMatcher.SaxHtmlMatcherException.class)
    public void rejectTagSelector() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.parse(" html > body > div > br\t\r");
    }

    @Test
    public void acceptComplicatedSelector() {
        SaxHtmlElementsMatcher.ElementPath path = SaxHtmlElementsMatcher.ElementPath.parse(" > html \t\r   >   \nbody.ho > div#main-content > br\t\r");
    }
}
