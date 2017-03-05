package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Match and build HTML Element(s) from a stream of Sax events
 * <p>
 * Elements are matched with a very limited subset of CSS selector grammar, see {@link ElementPath}
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
    static class ElementPath extends SaxEventListener.NopSaxEventListener {

        public static ElementPath parse(String selector) {
            return new ElementPath(selector);
        }

        private final ElementQualifier[] qualifiers;
        private final ArrayList<Element> matched = new ArrayList<Element>(4);

        // depth in a complete DOM
        private int currentDepth = 0;
        private int matchedDepth = 0;

        public ElementPath(String selector) {
            TokenQueue tq = new TokenQueue(selector);

            ArrayList<ElementQualifier> pathSelector = new ArrayList<ElementQualifier>();

            while (!tq.isEmpty()) {
                tq.consumeWhitespace();
                if (tq.isEmpty() || !tq.matchesAny('>')) {
                    throw new SaxHtmlMatcherException("Expected '>' before %s", tq.remainder());
                }
                tq.consume();
                tq.consumeWhitespace();

                ArrayList<ElementQualifier> thisLevel = new ArrayList<ElementQualifier>();

                while (true) {
                    if (tq.matchChomp("#")) {
                        String id = tq.consumeCssIdentifier();
                        Validate.notEmpty(id);
                        thisLevel.add(new ElementQualifier.IdIs(id));
                    } else if (tq.matchChomp(".")) {
                        String klass = tq.consumeCssIdentifier();
                        Validate.notEmpty(klass);
                        thisLevel.add(new ElementQualifier.HaveClass(klass));
                    } else if (tq.matchesWord()) {
                        String tag = tq.consumeTagName();
                        Validate.notEmpty(tag);
                        thisLevel.add(new ElementQualifier.TagIs(tag));
                    } else {
                        int numQualifiers = thisLevel.size();
                        if (numQualifiers == 0) {
                            throw new SaxHtmlMatcherException("expected selector before %s", tq.remainder());
                        } else if (numQualifiers == 1) {
                            pathSelector.add(thisLevel.get(0));
                        } else {
                            pathSelector.add(new ElementQualifier.And(thisLevel));
                        }
                        tq.consumeWhitespace();
                        break;
                    }
                }
            }

            if (pathSelector.size() == 0) {
                throw new SaxHtmlMatcherException("expected selector");
            }

            this.qualifiers = pathSelector.toArray(ElementQualifier.EmptyQualifierArray);
        }

        Element[] getMatched() {
            return matched.toArray(new Element[0]);
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

        @Override
        public void onStartTag(Token.StartTag token) {
            super.onStartTag(token);
        }

        @Override
        public void onEndTag(Token.EndTag token) {
            super.onEndTag(token);
        }

        @Override
        public void onDocType(Token.Doctype token) {
            super.onDocType(token);
        }

        @Override
        public void onComment(Token.Comment token) {
            super.onComment(token);
        }

        @Override
        public void onCharacter(Token.Character token) {
            super.onCharacter(token);
        }

        @Override
        public void onEOF(Token.EOF token) {
            super.onEOF(token);
        }

    }


    /**
     * Part of NodePath
     */
    abstract static class ElementQualifier {

        private static final ElementQualifier[] EmptyQualifierArray = new ElementQualifier[0];

        abstract boolean match(Token.StartTag startTag);

        /**
         * Combined qualifiers for one element
         */
        static class And extends ElementQualifier {

            private final ElementQualifier[] qualifiers;

            And(List<ElementQualifier> qualifierList) {
                this(qualifierList.toArray(EmptyQualifierArray));
            }

            And(ElementQualifier... qualifiers) {
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
         * tag (default to be case insensitive)
         */
        static class TagIs extends ElementQualifier {
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
        static class IdIs extends ElementQualifier {
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
        static class HaveClass extends ElementQualifier {
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
