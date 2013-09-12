/**
 * ADD LICENSE
 */
package com.salesforce.omakase.parser.selector;

import com.google.common.base.Optional;
import com.salesforce.omakase.Message;
import com.salesforce.omakase.ast.selector.IdSelector;
import com.salesforce.omakase.broadcaster.Broadcaster;
import com.salesforce.omakase.parser.AbstractParser;
import com.salesforce.omakase.parser.ParserException;
import com.salesforce.omakase.parser.Stream;
import com.salesforce.omakase.parser.token.Tokens;

/**
 * Parses an {@link IdSelector}.
 * <p/>
 * #rant The spec conflicts itself with ID selectors. In the actual description of ID selectors it says the name must be an
 * identifier (ident), however in the grammar it is "HASH", which is technically just #(name), where "name" is nmchar+ (think like
 * a hex color value). Just another example of the contradictory information all throughout the CSS "spec". #/rant
 *
 * @author nmcwilliams
 * @see IdSelector
 */
public class IdSelectorParser extends AbstractParser {

    @Override
    public boolean parse(Stream stream, Broadcaster broadcaster) {
        // note: important not to skip whitespace anywhere in here, as it could skip over a descendant combinator
        stream.collectComments(false);

        // snapshot the current state before parsing
        Stream.Snapshot snapshot = stream.snapshot();

        // first character must be a hash
        if (!stream.optionallyPresent(Tokens.HASH)) return false;

        // parse the id name
        Optional<String> name = stream.readIdent();
        if (!name.isPresent()) throw new ParserException(stream, Message.EXPECTED_VALID_ID);

        // broadcast the new id selector
        IdSelector selector = new IdSelector(snapshot.line, snapshot.column, name.get());
        selector.comments(stream.flushComments());
        broadcaster.broadcast(selector);
        return true;
    }

}
