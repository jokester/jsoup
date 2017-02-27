package org.jsoup.parser;

/**
 * A {@link SaxEventListener} receives a stream of HTML tokens from {@link SaxDriver}.
 * <p>
 * A SaxEventListener can be defined for various tasks:
 * - extract data
 * - possibly build a partial DOM (using {@link org.jsoup.select.Collector}) TODO can this be done?
 * - validate HTML (NOTE: SaxDriver holds minimal state and does not validate)
 * <p>
 * <p>
 * TODO determine constraint
 * - A SaxEventListener MUST NOT hold copy of token (tokens get reused in by {@link Tokeniser}
 * - A SaxEventListeenser SHOULD NOT throw
 * <p>
 * <p>
 * Differences to a SAX interface for XML:
 * - HTML have less strict grammar, especially on closing tags.
 * - HTML
 *
 * @author Wang Guan
 */
public interface SaxEventListener {
    // event callbacks are currently mirrored from subtypes of Token
    // TODO do we need other events?

    void onStartTag(Token.StartTag token);

    void onEndTag(Token.EndTag token);

    void onDocType(Token.Doctype token);

    void onComment(Token.Comment token);

    void onCharacter(Token.Character token);

    void onEOF(Token.EOF token);

    /**
     * A default implementation that ignore all tokens
     */
    public class NopSaxEventListener implements SaxEventListener {

        public void onStartTag(Token.StartTag token) {
        }

        public void onEndTag(Token.EndTag token) {

        }

        public void onDocType(Token.Doctype token) {

        }

        public void onComment(Token.Comment token) {

        }

        public void onCharacter(Token.Character token) {

        }

        public void onEOF(Token.EOF token) {

        }
    }
}
