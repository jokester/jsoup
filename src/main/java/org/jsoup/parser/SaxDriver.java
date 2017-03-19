package org.jsoup.parser;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;

/**
 * "Driver" for a SAX-like HTML parser
 * <p>
 * A SaxDriver feeds a stream of tokens to {@link SaxEventListener} registered to it.
 * <p>
 * A SaxDriver keeps little status.
 * Correct document structure / validation should be handled by individual SaxEventListener.
 *
 * @author Wang Guan
 */
public final class SaxDriver {

    private final ArrayList<SaxEventListener> listeners = new ArrayList<SaxEventListener>();

    /**
     * Void elements that *may* have self-closing tag
     */
    public static final String[] VoidElements = new String[]{
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "keygen",
            "link",
            "meta",
            "param,",
            "source",
            "track",
            "wbr",
    };

    private CharacterReader characterReader;

    // TODO can we read from stream instead of String? (original jsoup can't)
    public SaxDriver(String input) {
        characterReader = new CharacterReader(input);
    }

    public void addListener(SaxEventListener listener) {
        listeners.add(listener);
    }

    public void start() {
        ParseErrorList errors = ParseErrorList.tracking(16);
        Tokeniser tokeniser = new Tokeniser(characterReader, errors);
        Token t;
        while ((t = tokeniser.read()) != null) {
            if (t instanceof Token.Character) {
                feedListeners((Token.Character) t);
            } else if (t instanceof Token.StartTag) {
                feedListeners(t.asStartTag());
                // "unfolds" when t is a self-closing tag
                // or feed a fake closing tag if t is a void element
                if (t.asStartTag().isSelfClosing() || StringUtil.inSorted(t.asStartTag().normalName(), VoidElements))
                    feedListeners(new Token.EndTag().name(t.asStartTag().name()).asEndTag());
            } else if (t instanceof Token.EndTag) {
                if (!StringUtil.inSorted(((Token.EndTag) t).normalName(), VoidElements))
                  feedListeners((Token.EndTag) t);
            } else if (t instanceof Token.Doctype) {
                feedListeners((Token.Doctype) t);
            } else if (t instanceof Token.Comment) {
                feedListeners((Token.Comment) t);
            } else if (t instanceof Token.EOF) {
                feedListeners((Token.EOF) t);
            } else  {
                throw new NoSuchMethodError("Unexpected token class:" + t.getClass().getCanonicalName());
            }

            t.reset();

            if (t instanceof Token.EOF)
                break;
        }
    }

    private void feedListeners(Token.Character t) {
        for (SaxEventListener l : listeners) {
            l.onCharacter(t);
        }
    }

    private void feedListeners(Token.StartTag t) {
        for (SaxEventListener l : listeners) {
            l.onStartTag(t);
        }
    }

    private void feedListeners(Token.EndTag t) {
        for (SaxEventListener l : listeners) {
            l.onEndTag(t);
        }
    }

    private void feedListeners(Token.Doctype t) {
        for (SaxEventListener l : listeners) {
            l.onDocType(t);
        }
    }

    private void feedListeners(Token.Comment t) {
        for (SaxEventListener l : listeners) {
            l.onComment(t);
        }
    }

    private void feedListeners(Token.EOF t) {
        for (SaxEventListener l : listeners) {
            l.onEOF(t);
        }
    }
}
