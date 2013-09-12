/**
 * ADD LICENSE
 */
package com.salesforce.omakase.parser;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.salesforce.omakase.Message;
import com.salesforce.omakase.ast.RawSyntax;
import com.salesforce.omakase.parser.token.Token;
import com.salesforce.omakase.parser.token.TokenEnum;
import com.salesforce.omakase.parser.token.Tokens;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static com.salesforce.omakase.parser.token.Tokens.NEWLINE;

/**
 * A tool for reading a String source one character at a time. Basically a glorified wrapper around a String.
 * <p/>
 * This provides methods for navigating through the source, matching against expected {@link Token}s, and keeps track of the
 * current line and column positions.
 *
 * @author nmcwilliams
 */
public final class Stream {
    /** the source to process */
    private final String source;

    /** cached length of the source */
    private final int length;

    /** current position in the source */
    private int index = 0;

    /** current line in the source */
    private int line = 1;

    /** current column in the source */
    private int column = 1;

    /** line from the original source from which this sub-stream was derived */
    private final int anchorLine;

    /** column from the original source from which this sub-stream was derived */
    private final int anchorColumn;

    /** last index checked, so that #skipWhitespace can be short-circuited if the index hasn't changed */
    private int lastCheckedWhitespaceIndex = -1;

    /** last index checked, so that #collectComments can be short-circuited if the index hasn't changed */
    private int lastCheckedCommentIndex = -1;

    /** if we are inside of a string */
    private boolean inString = false;

    /** the character that opened the last string */
    private Token stringToken = null;

    /** whether we should monitor if we are in a string or not (optional for perf) */
    private final boolean checkInString;

    /** collection of parsed CSS comments */
    private List<String> comments;

    /**
     * Creates a new instance of a {@link Stream}, to be used for reading one character at a time from the given source.
     *
     * @param source
     *     The source to read.
     */
    public Stream(CharSequence source) {
        this(source, 1, 1, true);
    }

    /**
     * Creates a new instance of a {@link Stream}, to be used for reading one character at a time from the given source. This will
     * use the line and column from the given {@link RawSyntax} as the anchor/starting point.
     *
     * @param raw
     *     The {@link RawSyntax} containing the source.
     */
    public Stream(RawSyntax raw) {
        this(raw.content(), raw.line(), raw.column(), true);
    }

    /**
     * Creates a new instance of a {@link Stream}, to be used for reading one character at a time from the given source. This will
     * use the line and column from the given {@link RawSyntax} as the anchor/starting point.
     *
     * @param raw
     *     The {@link RawSyntax} containing the source.
     * @param checkInString
     *     Whether the stream should keep track of whether we are in a string or not. The main reason to specify false here is for
     *     performance reasons, to avoid extra processing that we know wouldn't be relevant.
     */
    public Stream(RawSyntax raw, boolean checkInString) {
        this(raw.content(), raw.line(), raw.column(), checkInString);
    }

    /**
     * Creates a new instance of a {@link Stream}, to be used for reading one character at a time from the given source. This will
     * use the given starting line and column.
     *
     * @param source
     *     The source to read.
     * @param anchorLine
     *     The starting line.
     * @param anchorColumn
     *     The starting column.
     */
    public Stream(CharSequence source, int anchorLine, int anchorColumn) {
        this(source, anchorLine, anchorColumn, true);
    }

    /**
     * Creates a new instance of a {@link Stream}, to be used for reading one character at a time from the given source. This will
     * use the given starting line and column.
     *
     * @param source
     *     The source to read.
     * @param anchorLine
     *     The starting line.
     * @param anchorColumn
     *     The starting column.
     * @param checkInString
     *     Whether the stream should keep track of whether we are in a string or not. The main reason to specify false here is for
     *     performance reasons, to avoid extra processing that we know wouldn't be relevant.
     */
    public Stream(CharSequence source, int anchorLine, int anchorColumn, boolean checkInString) {
        checkNotNull(source, "source cannot be null");
        this.source = source.toString();
        this.length = source.length();
        this.anchorLine = anchorLine;
        this.anchorColumn = anchorColumn;
        this.checkInString = checkInString;

        // check if we are in a string
        if (checkInString) {
            updateInString();
        }
    }

    /**
     * Gets the current line number.
     *
     * @return The current line number.
     */
    public int line() {
        return line;
    }

    /**
     * Gets the current column position.
     *
     * @return The current column position.
     */
    public int column() {
        return column;
    }

    /**
     * Gets the current index position within the original source. Not to be confused with the current column position, which is
     * found with {@link #column()} instead. Note that unlike the line and column number, index is 0-based.
     *
     * @return The current index position.
     */
    public int index() {
        return index;
    }

    /**
     * Gets the original line of this {@link Stream} within the original source. This is mainly useful for sub-sequences
     * (sequences created from a substring of the original source).
     *
     * @return The line number of the start of this stream in the original source.
     */
    public int anchorLine() {
        return anchorLine;
    }

    /**
     * Gets the original column of this {@link Stream} within the original source. This is mainly useful for sub-sequences
     * (sequences created from a substring of the original source).
     *
     * @return The column number of the start of this stream in the original source.
     */
    public int anchorColumn() {
        return anchorColumn;
    }

    /**
     * Gets a string description of the position of this {@link Stream} within the original source.
     *
     * @return The message.
     */
    public StringBuilder anchorPositionMessage() {
        return new StringBuilder(64).append("(starting from line ")
            .append(anchorLine)
            .append(", column ")
            .append(anchorColumn)
            .append(" in original source)");
    }

    /**
     * Gets whether this a sub-sequence.
     *
     * @return True if either the {@link #anchorLine()} or {@link #anchorColumn()} is greater than 1.
     */
    public boolean isSubStream() {
        return anchorLine != 1 || anchorColumn != 1;
    }

    /**
     * Gets the original source.
     *
     * @return The full original source.
     */
    public String source() {
        return source;
    }

    /**
     * Gets the remaining text in the source, including the current character. This does not advance the current position.
     *
     * @return A substring of the source from the current position to the end of the source.
     */
    public String remaining() {
        return source.substring(index);
    }

    /**
     * Gets the length of the source.
     *
     * @return The number of characters in the source.
     */
    public int length() {
        return length;
    }

    /**
     * Whether we are currently inside of a string.
     *
     * @return True if we are inside of a string.
     */
    public boolean inString() {
        return inString;
    }

    /**
     * Gets whether the current character is preceded by the escape character
     *
     * @return If the current character is escaped.
     *
     * @see Tokens#ESCAPE
     */
    public boolean isEscaped() {
        return Tokens.ESCAPE.matches(peekPrevious());
    }

    /**
     * Gets whether we are at the end of the source.
     *
     * @return True of we are at the end of the source.
     */
    public boolean eof() {
        return index == length;
    }

    /**
     * Gets the character at the current position.
     *
     * @return The character at the current position.
     */
    public Character current() {
        return eof() ? null : source.charAt(index);
    }

    /**
     * Advance to the next character. This will automatically update the current line and column number as well.
     * <p/>
     * The spec encourages normalizing new lines to a single line feed character, however we choose not to do this preprocessing
     * as it isn't necessary for correct parsing. However by not doing this, if the source does not use LF then the line/column
     * number reported by this stream (e.g., in error messages) will be incorrect. This seems acceptable as that information is
     * mostly just useful for development purposes anyway. (http://dev.w3 .org/csswg/css-syntax/#preprocessing-the-input-stream)
     *
     * @return The next character (i.e., the character at the current position after the result of this call), or null if at the
     *         end of the stream.
     */
    public Character next() {
        // if we are at the end then return null
        if (eof()) return null;

        // update line and column info
        if (NEWLINE.matches(current())) {
            line += 1;
            column = 1;
        } else {
            column += 1;
        }

        // increment index position
        index += 1;

        // check if we are in a string
        if (checkInString) {
            updateInString();
        }

        // return the current character
        return current();
    }

    /**
     * Advance the current position to the given index. The index must not be longer than the total length of the source. If the
     * given index is less than the current index then the index will remain unchanged.
     *
     * @param newIndex
     *     Advance to this position.
     */
    public void forward(int newIndex) {
        checkArgument(newIndex <= length, "index out of range");
        while (newIndex > index) {
            next();
        }
    }

    /**
     * Gets the next character without advancing the current position.
     *
     * @return The next character, or null if at the end of the stream.
     */
    public Character peek() {
        return peek(1);
    }

    /**
     * Gets the character at the given number of characters forward without advancing the current position.
     *
     * @param numCharacters
     *     The number of characters ahead to peak.
     *
     * @return The character, or null if the end of the stream occurs first.
     */
    public Character peek(int numCharacters) {
        return (index + numCharacters < length) ? source.charAt(index + numCharacters) : null;
    }

    /**
     * Gets the previous character.
     *
     * @return The previous character, or null if we are at the beginning.
     */
    public Character peekPrevious() {
        return (index > 0) ? source.charAt(index - 1) : null;
    }

    /** If the current character is whitespace then skip it along with all subsequent whitespace characters. */
    public void skipWhitepace() {
        // don't check the same index twice
        if (lastCheckedWhitespaceIndex == index) return;

        // store the last checked index
        lastCheckedWhitespaceIndex = index;

        // nothing to skip if we are at the end
        if (eof()) return;

        // skip characters until the current character is not whitespace
        while (Tokens.WHITESPACE.matches(current())) {
            next();
        }
    }

    /**
     * Similar to {@link #next()}, this will advance to the next character, <b>but only</b> if the current character matches the
     * given {@link Token}. If the current character does not match then the current index will remain unchanged. If you don't
     * need the actual value, consider {@link #optionallyPresent(Token)} instead.
     *
     * @param token
     *     The token to match.
     *
     * @return The parsed character, or {@link Optional#absent()} if not matched.
     */
    public Optional<Character> optional(Token token) {
        // if the current character doesn't match then don't advance
        if (!token.matches(current())) return Optional.absent();

        Optional<Character> value = Optional.of(current());

        // advance to the next character
        next();

        return value;
    }

    /**
     * Same as {@link #optional(Token)}, except it returns the result of {@link Optional#isPresent()}. Basically use this when you
     * don't care about keeping the actual parsed value (e.g., because it's discarded, you already know what it is, etc...)
     *
     * @param token
     *     The token to match.
     *
     * @return True if there was a match, false otherwise.
     */
    public boolean optionallyPresent(Token token) {
        return optional(token).isPresent();
    }

    /**
     * Similar to {@link #optional(Token)}, except this works with {@link TokenEnum}s, checking each member of the given enum (in
     * the declared order) for a matching token.
     * <p/>
     * As with {@link #optional(Token)}, if the current character matches the index will be advanced by one.
     *
     * @param <T>
     *     Type of the enum.
     * @param klass
     *     Enum class.
     *
     * @return The matching enum instance, or {@link Optional#absent()} if none match.
     */
    public <T extends Enum<T> & TokenEnum<?>> Optional<T> optionalFromEnum(Class<T> klass) {
        for (T constant : klass.getEnumConstants()) {
            if (optionallyPresent(constant.token())) return Optional.of(constant);
        }
        return Optional.absent();
    }

    /**
     * Similar to {@link #next()}, except it will enforce that the <b>current</b> character matches the given {@link Token} before
     * advancing, otherwise an error will be thrown.
     *
     * @param token
     *     Ensure that the current token matches this {@link Token} before we advance.
     */
    public void expect(Token token) {
        if (!token.matches(current())) throw new ParserException(this, Message.EXPECTED_TO_FIND, token.description());
        next();
    }

    /**
     * Advances the current character position until the current character matches the given {@link Token}. If the given {@link
     * Token} is never matched then this will advance to the end of the stream.
     * <p/>
     * This will skip over values inside parenthesis (mainly because ';' can be a valid part of a declaration value, e.g.,
     * data-uris). This will also skip over values inside of strings, but {@link #checkInString} must be turned on.
     *
     * @param token
     *     The token to match.
     *
     * @return A string containing all characters that were matched, excluding the character that matched the given {@link
     *         Token}.
     */
    public String until(Token token) {
        checkArgument(token != Tokens.OPEN_PAREN, "cannot match this token. Use #chomp instead.");
        checkArgument(token != Tokens.CLOSE_PAREN, "cannot match this token. Use #chomp instead.");

        // save the current index so we can return the matched substring
        int start = index;

        // keep track whether we are inside parenthesis
        boolean insideParens = false;

        // continually parse until we reach the token or eof
        while (!eof()) {
            char current = source.charAt(index);

            if (!inString) {
                // check for closing parenthesis
                if (Tokens.OPEN_PAREN.matches(current) && !isEscaped()) {
                    insideParens = true;
                } else if (insideParens && Tokens.CLOSE_PAREN.matches(current) && !isEscaped()) {
                    insideParens = false;
                } else if (!insideParens && token.matches(current) && !isEscaped()) {
                    // if unescaped then this is the matching token
                    return source.substring(start, index);
                }
            }

            // continue to the next character
            next();

        }

        // closing token wasn't found, so return the substring from the start to the end of the stream
        return source.substring(start);
    }

    /**
     * Opposite of {@link #until(Token)}, this will advance past the current character and all subsequent characters for as long
     * as they match the given {@link Token}.
     *
     * @param token
     *     The token to match.
     *
     * @return A string containing all characters that were matched. If nothing matched then an empty string is returned.
     */
    public String chomp(Token token) {
        if (eof()) return "";

        int start = index;

        // advance past all characters that match the token
        while (token.matches(current())) {
            next();
        }

        return source.substring(start, index);
    }

    /**
     * Similar to {@link #chomp(Token)}, except this expects the value to be enclosed with an opening and closing delimiter {@link
     * Token}.
     * <p/>
     * The opening token must be present at the current position of this stream or an error will be thrown. In other words, don't
     * call this until you've checked that the opening token is there, and only if you expect it to be properly closed.
     * <p/>
     * The closing token will be skipped over if it is preceded by {@link Tokens#ESCAPE} (thus no need to worry about handling
     * escaping).
     *
     * @param openingToken
     *     The opening token.
     * @param closingToken
     *     The closing token.
     *
     * @return All content in between the opening and closing tokens (excluding the tokens themselves).
     */
    public String chompEnclosedValue(Token openingToken, Token closingToken) {
        // the opening token is required
        expect(openingToken);

        // save the current position
        int start = index;

        // set initial nesting level
        int level = 1;

        // track depth (nesting), unless the opening and closing tokens are the same
        boolean allowNesting = !openingToken.equals(closingToken);

        // unless the closing token is a string, skip over all string content
        boolean skipString = !closingToken.equals(Tokens.DOUBLE_QUOTE) && !closingToken.equals(Tokens.SINGLE_QUOTE);

        // keep parsing until we find the closing token
        while (!eof()) {
            // if we are in a string continue until we are out of it
            if (skipString && inString()) {
                next();
            } else {
                // if nesting is allowed then another occurrence of the openingToken increases the nesting level,
                // unless preceded by the escape symbol.
                if (allowNesting && openingToken.matches(current()) && !isEscaped()) {
                    level++;
                } else if (closingToken.matches(current()) && !isEscaped()) {
                    // decrement the nesting level
                    level--;

                    // once the nesting level reaches 0 then we have found the correct closing token
                    if (level == 0) {
                        next(); // move past the closing token
                        return source.substring(start, index - 1);
                    }
                }

                // we haven't found the correct closing token, so continue
                next();
            }
        }

        throw new ParserException(this, Message.EXPECTED_CLOSING, closingToken.description());
    }

    /**
     * Same as {@link #collectComments(boolean)}, with a default skipWhitespace of true.
     *
     * @return this, for chaining.
     */
    public Stream collectComments() {
        return collectComments(true);
    }

    /**
     * Parses all comments at the current position in the source.
     * <p/>
     * Comments can be retrieved wth {@link #flushComments()}. That method will return and remove all comments currently in the
     * buffer.
     * <p/>
     * This separation into the two methods allows for comments to be collected prematurely without needing to backtrack if the
     * parser later determines it doesn't match. The next parser can still retrieve the comments from the buffer even if another
     * parser triggered the collection of them.
     *
     * @param skipWhitespace
     *     If we should skip past whitespace before, between and after comments.
     *
     * @return this, for chaining.
     */
    public Stream collectComments(boolean skipWhitespace) {
        // if we already checked at this index then don't waste time checking again
        if (lastCheckedCommentIndex == index) return this;

        // store the last checked index
        lastCheckedCommentIndex = index;

        while (!eof()) {
            // skip whitespace
            if (skipWhitespace) {
                skipWhitepace();
            }

            // try to read a comment
            String comment = readComment();

            // add the comment to the buffer if a comment was found
            if (comment != null) {
                // delayed (re)creation of the comment buffer
                if (comments == null) {
                    comments = new ArrayList<>(2);
                }
                comments.add(comment);
            } else {
                return this;
            }
        }
        return this;
    }

    /**
     * Reads a single comment.
     *
     * @return The comment, or null.
     */
    private String readComment() {
        String comment = null;
        boolean inComment = false;

        // check for the opening comment
        if (Tokens.FORWARD_SLASH.matches(current()) && Tokens.STAR.matches(peek())) {
            inComment = true;

            // save the current position so we can grab the comment contents later
            int start = index;

            // skip the opening "/*" part
            forward(2);

            // continue until we reach the end of the comment
            while (inComment) {
                if (Tokens.FORWARD_SLASH.matches(current()) && Tokens.STAR.matches(peekPrevious())) {
                    inComment = false;

                    // grab the comment contents
                    comment = source.substring(start + 2, index - 1);
                } else {
                    if (eof()) throw new ParserException(this, Message.MISSING_COMMENT_CLOSE);
                    next();
                }
            }

            // skip the closing slash. Doing it here because there may be a comment immediately after.
            next();
        }

        return comment;
    }

    /**
     * Returns all CSS comments currently in the buffer.
     * <p/>
     * CSS comments are placed into the buffer when {@link #collectComments()} is called. After calling this method the buffer
     * will be emptied.
     *
     * @return The current list of CSS comments.
     */

    public List<String> flushComments() {
        // gather the comments from the queue
        List<String> flushed = (comments == null) ? ImmutableList.<String>of() : comments;

        // reset the queue
        comments = null;

        return flushed;
    }

    /**
     * Creates a snapshot of the current index, line, column, and other essential state information.
     * <p/>
     * Creating a snapshot allows you to parse content but then return to a previous state once it becomes clear that the content
     * does fully match as expected. To revert to the latest snapshot call {@link Snapshot#rollback()} on the snapshot returned
     * from this method.
     *
     * @return The created snapshot.
     */

    public Snapshot snapshot() {
        return new Snapshot(this, index, line, column, inString);
    }

    /**
     * Reads an ident token.
     * <p/>
     * XXX the spec allows for non ascii and escaped characters here as well.
     *
     * @return The matched token, or {@link Optional#absent()} if not matched.
     */
    public Optional<String> readIdent() {
        final Character current = current();

        if (Tokens.NMSTART.matches(current)) {
            // spec says idents can't start with -- or -[0-9] (www.w3.org/TR/CSS21/syndata.html#value-def-identifier)
            if (Tokens.HYPHEN.matches(current) && Tokens.HYPHEN_OR_DIGIT.matches(peek())) return Optional.absent();

            // return the full ident token
            return Optional.of(chomp(Tokens.NMCHAR));
        }
        return Optional.absent();
    }

    @Override
    public String toString() {
        return String.format("%s\u00BB%s", source.substring(0, index), source.substring(index));
    }

    /**
     * Updates the status about whether we are in a string.
     * <p/>
     * We are in a string once we encounter an unescaped {@link Tokens#DOUBLE_QUOTE} or {@link Tokens#SINGLE_QUOTE}. We remain in
     * this status until the matching quote symbol is encountered again, unescaped.
     */
    private void updateInString() {
        final Character current = current();

        if (Tokens.DOUBLE_QUOTE.matches(current) && !isEscaped()) {
            if (inString && stringToken.equals(Tokens.DOUBLE_QUOTE)) {
                // closing quote
                stringToken = null;
                inString = false;
            } else {
                // opening quote
                stringToken = Tokens.DOUBLE_QUOTE;
                inString = true;
            }
        } else if (Tokens.SINGLE_QUOTE.matches(current) && !isEscaped()) {
            if (inString && stringToken.equals(Tokens.SINGLE_QUOTE)) {
                // closing quote
                stringToken = null;
                inString = false;
            } else {
                // opening quote
                stringToken = Tokens.SINGLE_QUOTE;
                inString = true;
            }
        }
    }

    /** data object */
    public static final class Snapshot {
        private final Stream stream;

        /** the captured index */
        public final int index;

        /** the line at the captured index */
        public final int line;

        /** the column at the captured index */
        public final int column;

        /** whether we are in a string at the captured index */
        public final boolean inString;

        private Snapshot(Stream stream, int index, int line, int column, boolean inString) {
            this.stream = stream;
            this.index = index;
            this.line = line;
            this.column = column;
            this.inString = inString;
        }

        /**
         * Reverts to the state (index, line, column, etc...) captured within this given snapshot.
         *
         * @return always returns <b>false</b> (convenience for inlining return statements in parse methods).
         */

        public boolean rollback() {
            stream.index = index;
            stream.line = line;
            stream.column = column;
            stream.inString = inString;
            return false;
        }

        /**
         * Similar to {@link #rollback()}, but this will also throw a {@link ParserException} with the given message and optional
         * message args.
         * <p/>
         * This is a convenience function to combine the common scenario of rolling back before throwing an error so that the
         * error message indicates a more accurate location of where the error occurred.
         *
         * @param message
         *     The error message.
         * @param args
         *     Optional args for the error message.
         */
        public void rollback(Message message, Object... args) {
            rollback();
            throw new ParserException(stream, message, args);
        }
    }
}
