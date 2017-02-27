package org.jsoup.parser;

import org.jsoup.helper.Validate;

/**
 * Match and build HTML Element(s) from a stream of Sax events
 * <p>
 * Elements are matched with a very limited subset of CSS selector grammar, see {@link ElementPath}
 * <p>
 * You can use normal selectors on Element.select() afterwards.
 */
public class SaxHtmlMatcher extends SaxEventListener.NopSaxEventListener {

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
        public ElementPath(String selector) {
            // TODO parse selector into an array of PathSegment
            // TODO keep a stack of tags by far
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
}
