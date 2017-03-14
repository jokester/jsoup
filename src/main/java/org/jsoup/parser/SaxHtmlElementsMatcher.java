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
 * This is be more effective, but less W3C-compliant than a complete parser like {@link HtmlTreeBuilder}
 * Please only use it on documents that are known to be clean and valid.
 * TODO `SaxHtmlMatcher` might be a better name?
 */
public class SaxHtmlElementsMatcher extends SaxEventListener.NopSaxEventListener {

    private static final String dummyURL = "";

    /**
     * A subclass can override this to retrieve element immediately
     * @param e
     */
    public void onElementMatched(Element e) {}


    static class PartialTreeBuilder implements SaxEventListener {

        private final ElementPath path;

        private final ArrayList<Element> nodeStack = new ArrayList<Element>(16);
        private final ArrayList<Element> found = new ArrayList<Element>();
        PartialTreeBuilder(String selector) {
            path = ElementPath.create(selector);
        }

        public void onStartTag(Token.StartTag token) {
            boolean matchingBefore = path.isMatching();
            path.onStartTag(token);
            boolean matchingAfter = path.isMatching();

            Element newElem = new Element(Tag.valueOf(token.tagName, ParseSettings.htmlDefault),
                    dummyURL, token.getAttributes());

            if (nodeStack.size() != 0) {
                 Element parent = nodeStack.get(nodeStack.size() - 1);
                 // TODO learn
            }

            nodeStack.add(newElem);
        }

        public void onEndTag(Token.EndTag token) {
            boolean matchingBefore = path.isMatching();
            boolean matchingAfter = path.isMatching();
            if (matchingBefore && !matchingAfter) {
                // pop nodeStack
            }
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

        public static ElementPath create(String selector) {
            return new ElementPath(selector);
        }

        private final ElementQualifier[] qualifiers;

        private final ArrayList<Token.StartTag> tagStack = new ArrayList<Token.StartTag>(16);

        // index of right-most matched tag
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

        /**
         * User of this class may check isMatching() after each onStartTag()
         *
         * @return whether the last StartTag / EndTag belong to matched part
         * <p>
         * When used to build Elements, user should detect transition
         * of isMatching() and start/finish build of Element.
         */
        public boolean isMatching() {
            return matchedDepth >= qualifiers.length - 1;
        }

        @Override
        public void onStartTag(Token.StartTag token) {
            int tagDepth = tagStack.size();
            tagStack.add(token);

            if (tagDepth < qualifiers.length) {
                ElementQualifier qualifier = qualifiers[tagDepth];
                if (qualifier.match(token)) {
                    matchedDepth = tagDepth;
                }
            }
        }

        @Override
        public void onEndTag(Token.EndTag token) {
            tagStack.remove(tagStack.size() - 1);
            int tagDepth = tagStack.size() - 1;

            if (tagDepth < matchedDepth) {
                matchedDepth = tagDepth;
            }

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
