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
 * <p>
 * ElementMatcher: matches a start tag by (tag name / id / class)
 * ElementPathMatcher: matches an array of start tags
 * TreeMatcher
 */
public class SaxHtmlElementsMatcher extends SaxEventListener.NopSaxEventListener {

    private static final String dummyURL = "";

    static class PartialTreeBuilder implements SaxEventListener {

        /**
         * A subclass can override this to retrieve element immediately
         *
         * @param e matched element
         */
        public void onElementMatched(Element e) {
        }


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

    static class PartialTreeMatcher extends SaxEventListener.NopSaxEventListener {

        private final ElementPath path;

        private final ArrayList<Element> nodeStack = new ArrayList<Element>(16);

        PartialTreeMatcher(String selector) {
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
    static class ElementPath {
        private final ElementQualifier[] qualifiers;
        private final ArrayList<String> tagStack = new ArrayList<String>(16);
        private final ParseErrorList errors;
        // num of matched tags. starting from left-most tag in tagStack
        int matchedDepth = 0;

        public ElementPath(String selector, ParseErrorList errors) {
            this.qualifiers = parseSelector(selector);
            this.errors = errors;
        }

        public static ElementPath create(String selector, ParseErrorList errors) {
            return new ElementPath(selector, errors);
        }

        public static ElementPath create(String selector) {
            return new ElementPath(selector, ParseErrorList.noTracking());
        }

        /**
         * @return whether the last StartTag / EndTag belongs to a Partial DOM Tree
         * <p>
         * When used to build Elements, user should detect transition
         * of isMatching() and start/finish build of Element.
         */
        public boolean isMatching() {
            return matchedDepth >= qualifiers.length;
        }

        public void onStartTag(Token.StartTag token) {
            final int tagDepth = tagStack.size();
            tagStack.add(token.name());

            // increase matchedDepth when the new tag is qualified
            if (matchedDepth == tagDepth && tagDepth < qualifiers.length) {
                ElementQualifier qualifier = qualifiers[tagDepth];
                if (qualifier.match(token)) {
                    matchedDepth = tagDepth + 1;
                }
            }
        }

        public boolean onEndTag(Token.EndTag token) {
            final int tagDepth = tagStack.size();
            if (tagDepth == 0) {
                unexpectedTag(token, "tagStack is already empty");
                return false;
            }

            final String rightMost = tagStack.get(tagDepth - 1);
            if (!rightMost.equals(token.name())) {
                unexpectedTag(token, "last StartTag was " + rightMost);
                return false;
            }

            // pop from tagStack and decrease matchedDepth
            tagStack.remove(tagDepth - 1);
            matchedDepth = Math.min(matchedDepth, tagDepth - 1);
            return true;
        }

        // this is the only possible error
        void unexpectedTag(Token.Tag tag, String message) {
            if (errors.canAddError()) {
                errors.add(new ParseError(-1, "Unexpected token '%s': %s", tag.toString(), message));
            }
        }

        @Deprecated
        Token.StartTag dup(Token.StartTag t) {
            // TODO remove this if not required
            return new Token.StartTag().nameAttr(t.name(), t.getAttributes());
        }

        ElementQualifier[] parseSelector(String selector) {
            ArrayList<ElementQualifier> pathSelector = new ArrayList<ElementQualifier>();

            TokenQueue tq = new TokenQueue(selector);
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

            return pathSelector.toArray(ElementQualifier.EmptyQualifierArray);
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
