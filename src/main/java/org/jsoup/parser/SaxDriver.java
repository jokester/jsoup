package org.jsoup.parser;

import java.util.ArrayList;

/**
 * "Driver" for a SAX-like HTML parser
 *
 * A SaxDriver feeds a stream of tokens to {@link SaxEventListener} registered to it.
 *
 * A SaxDriver only keeps status at token level.
 * The Document structure / validation should be handled by individual SaxEventListener.
 *
 * @author Wang Guan
 */
public final class SaxDriver {

    private final ArrayList<SaxEventListener> listeners = new ArrayList<SaxEventListener>();

    private CharacterReader characterReader;

    public SaxDriver(String input) {
        this.characterReader = new CharacterReader(input);
    }


    public void addListener(SaxEventListener listener) {
        listeners.add(listener);
    }

    private void start() {

    }


}
