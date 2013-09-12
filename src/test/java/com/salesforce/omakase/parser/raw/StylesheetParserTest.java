/**
 * ADD LICENSE
 */
package com.salesforce.omakase.parser.raw;

import com.salesforce.omakase.broadcaster.QueryableBroadcaster;
import com.salesforce.omakase.parser.ParserException;
import com.salesforce.omakase.parser.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link StylesheetParser}.
 *
 * @author nmcwilliams
 */
@SuppressWarnings("JavaDoc")
public class StylesheetParserTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testEof() {
        exception.expect(ParserException.class);
        exception.expectMessage("Extraneous text found at the end of the source");
        new StylesheetParser().parse(new Stream(".abc{color:red}   `"), new QueryableBroadcaster());
    }
}
