package org.jsoup.parser;

/**
 * A {@link SaxEventListener} receives a stream of HTML tokens from {@link SaxDriver}.
 *
 * A SaxEventListener can be defined for various tasks:
 * - extract data
 * - possibly build a partial DOM (using {@link org.jsoup.select.Collector}) TODO can this be done?
 * - validate HTML (NOTE: SaxDriver holds minimal state and does not validate)
 *
 *
 * TODO determine constraint
 *
 *
 * Differences to a SAX interface for XML:
 * - HTML have less strict grammar, especially on closing tags.
 * - HTML
 * @author Wang Guan
 */
public interface SaxEventListener {
    // event callbacks are currently mirrored from subtypes of Token
    // TODO do we need others?

    void onStartTag(Token.StartTag tag);
    void onEndTag(Token.EndTag tag);
    void onDocType(Token.Doctype doctype);
    void onComment(Token.Comment comment);
    void onCharacter(Token.Character character);
    void onEOF(Token.EOF eof);
}
