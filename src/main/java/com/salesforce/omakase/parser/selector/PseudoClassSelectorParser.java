/**
 * ADD LICENSE
 */
package com.salesforce.omakase.parser.selector;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.salesforce.omakase.Broadcaster;
import com.salesforce.omakase.ast.selector.PseudoClassSelector;
import com.salesforce.omakase.emitter.SubscriptionType;
import com.salesforce.omakase.parser.AbstractParser;
import com.salesforce.omakase.parser.Stream;
import com.salesforce.omakase.parser.token.Tokens;

/**
 * TODO Description
 * 
 * @author nmcwilliams
 */
public class PseudoClassSelectorParser extends AbstractParser {
    /** these can use pseudo class syntax but are actually pseudo elements */
    private static final Set<String> FAKES = Sets.newHashSet("first-line", "first-letter", "before", "after");

    @Override
    public boolean parse(Stream stream, Broadcaster broadcaster) {
        stream.snapshot();

        // gather the line and column before advancing the stream
        int line = stream.line();
        int column = stream.column();

        // first character must be a colon
        if (!Tokens.COLON.matches(stream.current())) return false;

        // if there is another colon immediately after then it's a pseudo element selector instead.
        if (Tokens.COLON.matches(stream.peek(2))) return false;

        // skip past the one colon
        stream.next();

        // read the name
        Optional<String> name = stream.readIdent();

        // some pseudo elements look like pseudo classes. we don't want to parse those
        if (!name.isPresent() || FAKES.contains(name.get())) return stream.rollback();

        // FIXME pseudo elements with functions

        // create the selector and broadcast it
        PseudoClassSelector selector = new PseudoClassSelector(line, column, name.get());
        broadcaster.broadcast(SubscriptionType.CREATED, selector);

        return true;
    }

}