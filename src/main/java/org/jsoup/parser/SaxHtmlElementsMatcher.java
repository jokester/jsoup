package org.jsoup.parser;

import org.jsoup.helper.Validate;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Match and build HTML Element(s) from a stream of Sax events
 * <p>
 * Elements are matched with a very limited subset of CSS selector grammar, see {@link ElementsPath}
 * <p>
 * You can use normal selectors on Element.select() afterwards.
 */
public class SaxHtmlElementsMatcher extends SaxEventListener.NopSaxEventListener {

    /**
     * NodePath: path of node that uses
     * <p>
     * The selector grammar is determined in order to enable efficient node matching.
     * 1. a NodePath must contain absolute element path, starting from
     * root of document root. e.g. "> html > body > div#main"
     * 2. a PathSegment is of form 'tag.class#id' where at least one
     * of tag, classes, id is supplied
     */
    static class ElementsPath extends SaxEventListener.NopSaxEventListener {
        public ElementsPath(String selector) {
            // TODO parse selector into an array of PathSegment
            // TODO keep a stack of tags by far
            TokenQueue tq = new TokenQueue(selector);

            while (!tq.isEmpty()) {
                tq.consumeWhitespace();
                if (tq.consume() != '>') {
                    Validate.fail(String.format(Locale.ENGLISH, "Expected at '%s'", tq.remainder()));
                }

                ArrayList<String> tagQualifierStrings = null;
                ArrayList<String> idQualifierStrings = null;
                ArrayList<String> classQualifierStrings = null;

                while (true) {

                }
            }
        }

        private void parse() {

        }

        /**
         * User of this class may check isMatching() after each onStartTag()
         *
         * @return whether the last StartTag / EndTag belong to matched part
         * <p>
         * When used to build Elements, user should detect transition
         * of isMatching() and start/finish build of Element.
         */
        public boolean isMatching() {
            return false;
        }
    }

    /**
     * Part of NodePath
     */
    abstract static class ElementQualifier {
        abstract boolean match(Token.StartTag startTag);

        /**
         * Combined qualifiers for one element
         */
        abstract static class And extends ElementQualifier {
            private final ElementQualifier[] qualifiers;

            private And(ElementQualifier... qualifiers) {
                this.qualifiers = qualifiers;
                Validate.notNull(qualifiers);
                Validate.isTrue(qualifiers.length > 0, "qualifier array cannot be null");
            }

            boolean match(Token.StartTag startTag) {
                for (ElementQualifier q : qualifiers) {
                    if (!q.match(startTag))
                        return false;
                }
                return true;
            }
        }

        /**
         * tag
         */
        abstract static class TagIs extends ElementQualifier {
            private final String tag;
            private final boolean caseSensitive;

            TagIs(String tag) {
                this(tag, false);
            }

            TagIs(String tag, boolean caseSensitive) {
                Validate.notEmpty(tag);
                this.tag = tag;
                this.caseSensitive = caseSensitive;
            }

            boolean match(Token.StartTag startTag) {
                if (caseSensitive)
                    return tag.equals(startTag.tagName);
                else
                    return tag.equalsIgnoreCase(startTag.tagName);
            }
        }

        /**
         * id equality (case sensitive)
         */
        abstract static class IdIs extends ElementQualifier {
            private final String id;

            IdIs(String id) {
                Validate.notEmpty(id);
                this.id = id;
            }

            @Override
            boolean match(Token.StartTag startTag) {
                return id.equalsIgnoreCase(startTag.getAttributes().getIgnoreCase("id"));
            }
        }

        /**
         * have class (case sensitive)
         */
        abstract static class HaveClass extends ElementQualifier {
            private final String classname;

            HaveClass(String classname) {
                Validate.notNull(classname);
                this.classname = classname;
            }

            @Override
            boolean match(Token.StartTag startTag) {
                String classnames = startTag.getAttributes().getIgnoreCase("class");
                // FIXME
                return false;
            }
        }
    }

    public static class SaxHtmlMatcherException extends IllegalStateException {
        public SaxHtmlMatcherException(String msg, Object... params) {
            super(String.format(msg, params));
        }
    }
}
