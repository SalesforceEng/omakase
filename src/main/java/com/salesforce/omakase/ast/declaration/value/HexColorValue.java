/**
 * ADD LICENSE
 */
package com.salesforce.omakase.ast.declaration.value;

import com.salesforce.omakase.As;
import com.salesforce.omakase.ast.AbstractSyntax;
import com.salesforce.omakase.ast.Syntax;
import com.salesforce.omakase.emitter.Description;
import com.salesforce.omakase.emitter.Subscribable;
import com.salesforce.omakase.writer.StyleAppendable;
import com.salesforce.omakase.writer.StyleWriter;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.salesforce.omakase.emitter.SubscribableRequirement.REFINED_DECLARATION;

/**
 * TESTME
 * <p/>
 * A hex color value (e.g., "fffeee"). The value is always converted to lower-case.
 *
 * @author nmcwilliams
 * @see HexColorValue
 */
@Subscribable
@Description(value = "individual hex color value", broadcasted = REFINED_DECLARATION)
public class HexColorValue extends AbstractSyntax implements Term {
    private String color;

    /**
     * Constructs a new instance of a {@link HexColorValue}.
     *
     * @param line
     *     The line number.
     * @param column
     *     The column number.
     * @param color
     *     The hex color (do not include the #).
     */
    public HexColorValue(int line, int column, String color) {
        super(line, column);
        this.color = color.toLowerCase();
    }

    /**
     * Constructs a new instance of a {@link HexColorValue} (used for dynamically created {@link Syntax} units).
     *
     * @param color
     *     The hex color (do not include the #).
     */
    public HexColorValue(String color) {
        color(color);
        // TODO validation?
    }

    /**
     * Sets the value of the color (converted to lower-case).
     *
     * @param color
     *     The hex color (do not include the #).
     *
     * @return this, for chaining.
     */
    public HexColorValue color(String color) {
        checkNotNull(color, "color cannot be null");
        this.color = color.toLowerCase();
        return this;
    }

    /**
     * Gets the color value (do not include the #).
     *
     * @return The color value.
     */
    public String color() {
        return color;
    }

    /**
     * Gets whether this hex color is shorthand (has a length of three).
     *
     * @return True if the length of this color is 3.
     */
    public boolean isShorthand() {
        return color.length() == 3;
    }

    @Override
    public void write(StyleWriter writer, StyleAppendable appendable) throws IOException {
        if (writer.isCompressed()) {
            // TODO optimizations
            appendable.append('#').append(color);
        } else {
            appendable.append('#').append(color);
        }
    }

    @Override
    public String toString() {
        return As.string(this)
            .add("color", color)
            .toString();
    }

    /**
     * Creates a new {@link HexColorValue} instance using the given color (do not include the #).
     * <p/>
     * Example:
     * <pre>
     * <code> HexColorValue.of("fffeee")</code>
     * </pre>
     *
     * @param color
     *     The color value.
     *
     * @return The new {@link HexColorValue} instance.
     */
    public static HexColorValue of(String color) {
        return new HexColorValue(color);
    }
}
