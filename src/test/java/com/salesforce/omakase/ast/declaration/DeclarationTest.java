/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.salesforce.omakase.ast.declaration;

import com.salesforce.omakase.SupportMatrix;
import com.salesforce.omakase.ast.Comment;
import com.salesforce.omakase.ast.RawSyntax;
import com.salesforce.omakase.ast.Rule;
import com.salesforce.omakase.ast.Status;
import com.salesforce.omakase.data.Keyword;
import com.salesforce.omakase.data.Prefix;
import com.salesforce.omakase.data.Property;
import com.salesforce.omakase.parser.refiner.Refiner;
import com.salesforce.omakase.test.functional.StatusChangingBroadcaster;
import com.salesforce.omakase.test.util.Util;
import com.salesforce.omakase.util.Values;
import com.salesforce.omakase.writer.StyleAppendable;
import com.salesforce.omakase.writer.StyleWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

/** Unit tests for {@link Declaration}. */
@SuppressWarnings("JavaDoc")
public class DeclarationTest {
    @org.junit.Rule public final ExpectedException exception = ExpectedException.none();

    private RawSyntax rawName;
    private RawSyntax rawValue;
    private Declaration fromRaw;

    @Before
    public void setup() {
        rawName = new RawSyntax(2, 3, "display");
        rawValue = new RawSyntax(2, 5, "none");
        fromRaw = new Declaration(rawName, rawValue, new Refiner(new StatusChangingBroadcaster()));
    }

    @Test
    public void rawValues() {
        assertThat(fromRaw.rawPropertyName().get()).isSameAs(rawName);
        assertThat(fromRaw.rawPropertyValue().get()).isSameAs(rawValue);
        assertThat(fromRaw.line()).isEqualTo(rawName.line());
        assertThat(fromRaw.column()).isEqualTo(rawName.column());
    }

    @Test
    public void setPropertyName() {
        Declaration d = new Declaration(Property.MARGIN, NumericalValue.of(5, "px"));
        d.propertyName(PropertyName.using(Property.PADDING));
        assertThat(d.propertyName().name()).isEqualTo("padding");
    }

    @Test
    public void setPropertyNameUsingShorthand() {
        Declaration d = new Declaration(Property.MARGIN, NumericalValue.of(5, "px"));
        d.propertyName(Property.PADDING);
        assertThat(d.propertyName().name()).isEqualTo("padding");
    }

    @Test
    public void getPropertyNameWhenUnrefined() {
        assertThat(fromRaw.propertyName().name()).isEqualTo("display");
    }

    @Test
    public void getPropertyNameWhenRefined() {
        assertThat(fromRaw.propertyName().name()).isEqualTo("display");
        assertThat(fromRaw.propertyName().name()).isEqualTo("display");
    }

    @Test
    public void setPropertyValue() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        TermList newValue = TermList.singleValue(KeywordValue.of(Keyword.BLOCK));
        d.propertyValue(newValue);
        assertThat(d.propertyValue()).isSameAs(newValue);
    }

    @Test
    public void setPropertyValueShorthand() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        d.propertyValue(KeywordValue.of(Keyword.BLOCK));
        assertThat(Values.asKeyword(d.propertyValue()).isPresent()).isTrue();
    }

    @Test
    public void newPropertyValueIsBroadcasted() {
        Rule rule = new Rule(1, 1, new StatusChangingBroadcaster());
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        TermList newValue = TermList.singleValue(KeywordValue.of(Keyword.BLOCK));
        d.propertyValue(newValue);

        assertThat(newValue.status()).isSameAs(Status.UNBROADCASTED);
        rule.declarations().append(d);
        assertThat(newValue.status()).isNotSameAs(Status.UNBROADCASTED);
    }

    @Test
    public void setPropertyValueDoesntBroadcastAlreadyBroadcasted() {
        StatusChangingBroadcaster broadcaster = new StatusChangingBroadcaster();
        Rule rule = new Rule(1, 1, broadcaster);
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        d.status(Status.PROCESSED);

        TermList newValue = TermList.singleValue(KeywordValue.of(Keyword.BLOCK));
        newValue.status(Status.PROCESSED);
        d.propertyValue(newValue);

        rule.declarations().append(d);
        assertThat(broadcaster.all).isEmpty();
    }

    @Test
    public void setPropertyValueAssignsParent() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        TermList newValue = TermList.singleValue(KeywordValue.of(Keyword.BLOCK));
        d.propertyValue(newValue);
        assertThat(d.propertyValue().parentDeclaration().get()).isSameAs(d);
    }

    @Test
    public void propagatebroadcastBroadcastsPropertyValue() {
        PropertyValue pv = TermList.singleValue(KeywordValue.of(Keyword.NONE));
        Declaration d = new Declaration(Property.DISPLAY, pv);

        assertThat(pv.status()).isSameAs(Status.UNBROADCASTED);
        d.propagateBroadcast(new StatusChangingBroadcaster());
        assertThat(pv.status()).isNotSameAs(Status.UNBROADCASTED);
    }

    @Test
    public void getPropertyValueWhenUnrefined() {
        // should automatically refine to the property value)
        assertThat(fromRaw.propertyValue()).isNotNull();
    }

    @Test
    public void getPropertyValueWhenRefined() {
        // automatic refinement should not occur since we are already refined, hence should be the same object
        PropertyValue propertyValue = fromRaw.propertyValue();
        assertThat(fromRaw.propertyValue()).isSameAs(propertyValue);
    }

    @Test
    public void isPropertyWithAnotherPropertyNameTrue() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isProperty(PropertyName.using(Property.DISPLAY))).isTrue();
    }

    @Test
    public void isPropertyWithAnotherPropertyNameFalse() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isProperty(PropertyName.using(Property.COLOR))).isFalse();
    }

    @Test
    public void isPropertyStringTrue() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isProperty("display")).isTrue();
    }

    @Test
    public void isPropertyStringFalse() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isProperty("color")).isFalse();
    }

    @Test
    public void isPropertyTrue() {
        Declaration d = new Declaration(Property.DISPLAY, TermList.singleValue(KeywordValue.of(Keyword.NONE)));
        assertThat(d.isProperty(PropertyName.using(Property.DISPLAY))).isTrue();
    }

    @Test
    public void isPropertyFalse() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isProperty(Property.COLOR)).isFalse();
    }

    @Test
    public void isPropertyIgnorePrefixTrue() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        d.propertyName().prefix(Prefix.MOZ);
        assertThat(d.isPropertyIgnorePrefix(Property.DISPLAY)).isTrue();
    }

    @Test
    public void isPropertyIgnorePrefixFalse() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        d.propertyName().prefix(Prefix.MOZ);
        assertThat(d.isPropertyIgnorePrefix(Property.MARGIN)).isFalse();
    }

    @Test
    public void isPrefixedTrue() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        d.propertyName().prefix(Prefix.MOZ);
        assertThat(d.isPrefixed()).isTrue();
    }

    @Test
    public void isPrefixedFalse() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isPrefixed()).isFalse();
    }

    @Test
    public void isRefinedTrue() {
        fromRaw.refine();
        assertThat(fromRaw.isRefined()).isTrue();
    }

    @Test
    public void isRefinedFalse() {
        assertThat(fromRaw.isRefined()).isFalse();
    }

    @Test
    public void isRefinedTrueForDynamicallyCreatedUnit() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isRefined()).isTrue();
    }

    @Test
    public void refine() {
        fromRaw.refine();
        assertThat(fromRaw.propertyName()).isNotNull();
        assertThat(fromRaw.propertyValue()).isNotNull();
    }

    @Test
    public void setOrphanedComments() {
        Comment c = new Comment("c");
        fromRaw.orphanedComment(c);
        assertThat(fromRaw.orphanedComments()).containsExactly(c);
    }

    @Test
    public void getOrphanedCommentsWhenAbsent() {
        assertThat(fromRaw.orphanedComments()).isEmpty();
    }

    @Test
    public void writeVerboseRefined() throws IOException {
        TermList terms = TermList.ofValues(OperatorType.SPACE, NumericalValue.of(1, "px"), NumericalValue.of(2, "px"));
        Declaration d = new Declaration(Property.MARGIN, terms);
        StyleWriter writer = StyleWriter.verbose();
        assertThat(writer.writeSnippet(d)).isEqualTo("margin: 1px 2px");
    }

    @Test
    public void writeInlineRefined() throws IOException {
        TermList terms = TermList.ofValues(OperatorType.SPACE, NumericalValue.of(1, "px"), NumericalValue.of(2, "px"));
        Declaration d = new Declaration(Property.MARGIN, terms);
        StyleWriter writer = StyleWriter.inline();
        assertThat(writer.writeSnippet(d)).isEqualTo("margin:1px 2px");
    }

    @Test
    public void writeCompressedRefined() throws IOException {
        TermList terms = TermList.ofValues(OperatorType.SPACE, NumericalValue.of(1, "px"), NumericalValue.of(2, "px"));
        Declaration d = new Declaration(Property.MARGIN, terms);
        StyleWriter writer = StyleWriter.compressed();
        assertThat(writer.writeSnippet(d)).isEqualTo("margin:1px 2px");
    }

    @Test
    public void writeVerboseUnrefined() throws IOException {
        RawSyntax name = new RawSyntax(2, 3, "border");
        RawSyntax value = new RawSyntax(2, 5, "1px solid red");
        Declaration d = new Declaration(name, value, new Refiner(new StatusChangingBroadcaster()));

        StyleWriter writer = StyleWriter.verbose();
        assertThat(writer.writeSnippet(d)).isEqualTo("border: 1px solid red");
    }

    @Test
    public void writeInlineUnrefined() throws IOException {
        RawSyntax name = new RawSyntax(2, 3, "border");
        RawSyntax value = new RawSyntax(2, 5, "1px solid red");
        Declaration d = new Declaration(name, value, new Refiner(new StatusChangingBroadcaster()));

        StyleWriter writer = StyleWriter.inline();
        assertThat(writer.writeSnippet(d)).isEqualTo("border:1px solid red");
    }

    @Test
    public void writeCompressedUnrefined() throws IOException {
        RawSyntax name = new RawSyntax(2, 3, "border");
        RawSyntax value = new RawSyntax(2, 5, "1px solid red");
        Declaration d = new Declaration(name, value, new Refiner(new StatusChangingBroadcaster()));

        StyleWriter writer = StyleWriter.compressed();
        assertThat(writer.writeSnippet(d)).isEqualTo("border:1px solid red");
    }

    @Test
    public void isWritableWhenAttached() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        com.salesforce.omakase.ast.Rule rule = new com.salesforce.omakase.ast.Rule();
        rule.declarations().append(d);
        assertThat(d.isWritable()).isTrue();
    }

    @Test
    public void isNotWritableWhenDetached() {
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(d.isWritable()).isFalse();
    }

    @Test
    public void isWritableWhenUnrefinedAndAttached() {
        RawSyntax name = new RawSyntax(2, 3, "border");
        RawSyntax value = new RawSyntax(2, 5, "1px solid red");
        Declaration d = new Declaration(name, value, new Refiner(new StatusChangingBroadcaster()));
        com.salesforce.omakase.ast.Rule rule = new com.salesforce.omakase.ast.Rule();
        rule.declarations().append(d);
        assertThat(d.isWritable()).isTrue();
    }

    @Test
    public void isNotWritableWhenPropertyValueNotWritable() {
        Declaration d = new Declaration(Property.DISPLAY, new TestPropertyValue());
        com.salesforce.omakase.ast.Rule rule = new com.salesforce.omakase.ast.Rule();
        rule.declarations().append(d);
        assertThat(d.isWritable()).isFalse();
    }

    @Test
    public void toStringTest() {
        Declaration value = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        assertThat(value.toString()).isNotEqualTo(Util.originalToString(value));
    }

    private static final class TestPropertyValue extends AbstractPropertyValue {
        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isImportant() {
            return false;
        }

        @Override
        public PropertyValue important(boolean important) {
            return this;
        }

        @Override
        public void write(StyleWriter writer, StyleAppendable appendable) throws IOException {
        }

        @Override
        public PropertyValue copy() {
            return null;
        }

        @Override
        public PropertyValue copyWithPrefix(Prefix prefix, SupportMatrix support) {
            return null;
        }
    }
}
