/**
 * ADD LICENSE
 */
package com.salesforce.omakase.ast;

import com.salesforce.omakase.As;
import com.salesforce.omakase.broadcaster.Broadcaster;

/**
 * TESTME
 * <p/>
 * Base class for {@link Syntax} units.
 *
 * @author nmcwilliams
 */
public abstract class AbstractSyntax implements Syntax {
    private final int line;
    private final int column;

    private Broadcaster broadcaster;
    private Status status = Status.UNBROADCASTED;

    /** Creates a new instance with no line or number specified (used for dynamically created {@link Syntax} units). */
    public AbstractSyntax() {
        this(-1, -1);
    }

    /**
     * Creates a new instance with the given line and column numbers.
     *
     * @param line
     *     The line number.
     * @param column
     *     The column number.
     */
    public AbstractSyntax(int line, int column) {
        this(line, column, null);
    }

    /**
     * Creates a new instance with the given line and column numbers, and the given {@link Broadcaster} to be used for
     * broadcasting new units.
     *
     * @param line
     *     The line number.
     * @param column
     *     The column number.
     * @param broadcaster
     *     Used to broadcast new {@link Syntax} units.
     */
    public AbstractSyntax(int line, int column, Broadcaster broadcaster) {
        this.line = line;
        this.column = column;
        this.broadcaster = broadcaster;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public int column() {
        return column;
    }

    @Override
    public boolean hasSourcePosition() {
        return line != -1 && column != -1;
    }

    @Override
    public Syntax status(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public Syntax broadcaster(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
        return this;
    }

    @Override
    public Broadcaster broadcaster() {
        return broadcaster;
    }

    @Override
    public void propagateBroadcast(Broadcaster broadcaster) {
        // only broadcast ourselves once
        if (this.status == Status.UNBROADCASTED) {
            broadcaster.broadcast(this);
        }
    }

    @Override
    public String toString() {
        return As.stringNamed("")
            .add("line", line)
            .add("column", column)
            .toString();
    }
}
