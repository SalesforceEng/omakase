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

package com.salesforce.omakase.ast;

import com.salesforce.omakase.ast.declaration.Declaration;
import com.salesforce.omakase.ast.declaration.KeywordValue;
import com.salesforce.omakase.ast.declaration.NumericalValue;
import com.salesforce.omakase.ast.selector.ClassSelector;
import com.salesforce.omakase.ast.selector.IdSelector;
import com.salesforce.omakase.ast.selector.Selector;
import com.salesforce.omakase.data.Keyword;
import com.salesforce.omakase.data.Property;
import com.salesforce.omakase.test.functional.StatusChangingBroadcaster;
import com.salesforce.omakase.writer.StyleWriter;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

/** Unit tests for Rule. */
@SuppressWarnings("JavaDoc")
public class RuleTest {
    @org.junit.Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void canAddSelector() {
        Rule rule = new Rule();
        assertThat(rule.selectors()).isEmpty();
        rule.selectors().append(new Selector(new ClassSelector("name")));
        assertThat(rule.selectors()).hasSize(1);
    }

    @Test
    public void canAddDeclaration() {
        Rule rule = new Rule();
        assertThat(rule.declarations()).isEmpty();
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        assertThat(rule.declarations()).hasSize(1);
    }

    @Test
    public void asRuleAlwaysPresent() {
        Rule rule = new Rule();
        assertThat(rule.asRule().isPresent()).isTrue();
    }

    @Test
    public void asAtRuleAlwaysAbsent() {
        Rule rule = new Rule();
        assertThat(rule.asAtRule().isPresent()).isFalse();
    }

    @Test
    public void propagatesBroadcast() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));

        rule.selectors().append(s);
        rule.declarations().append(d);

        assertThat(s.status() == Status.UNBROADCASTED);
        assertThat(d.status() == Status.UNBROADCASTED);

        rule.propagateBroadcast(new StatusChangingBroadcaster());

        assertThat(s.status() != Status.UNBROADCASTED);
        assertThat(d.status() != Status.UNBROADCASTED);
    }

    @Test
    public void copy() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        rule.selectors().append(s);
        rule.declarations().append(d);

        Rule copy = (Rule)rule.copy();
        assertThat(copy.selectors()).hasSize(1);
        assertThat(copy.declarations()).hasSize(1);
    }

    @Test
    public void isWritableTrueWhenSelectorsAndDeclarationsWritable() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        rule.selectors().append(s);
        rule.declarations().append(d);

        assertThat(rule.isWritable()).isTrue();
    }

    @Test
    public void isWritableFalseWhenNoSelectors() {
        Rule rule = new Rule();
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        rule.declarations().append(d);
        assertThat(rule.isWritable()).isFalse();
    }

    @Test
    public void isWritableFalseWhenSelectorsNotWritable() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        rule.selectors().append(s);
        rule.declarations().append(d);

        s.destroy();
        assertThat(rule.isWritable()).isFalse();
    }

    @Test
    public void isWritableFalseWhenNoDeclarations() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        rule.selectors().append(s);
        assertThat(rule.isWritable()).isFalse();
    }

    @Test
    public void isWritableFalseWhenDeclarationsNotWritable() {
        Rule rule = new Rule();
        Selector s = new Selector(new ClassSelector("name"));
        Declaration d = new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE));
        rule.selectors().append(s);
        rule.declarations().append(d);

        d.destroy();
        assertThat(rule.isWritable()).isFalse();
    }

    @Test
    public void writeVerbose() throws IOException {
        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        rule.selectors().append(new Selector(new IdSelector("id")));
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        rule.declarations().append(new Declaration(Property.MARGIN, NumericalValue.of(5, "px")));

        assertThat(StyleWriter.verbose().writeSnippet(rule)).isEqualTo(".class, #id {\n  display: none;\n  margin: 5px;\n}");
    }

    @Test
    public void writeInline() throws IOException {
        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        rule.selectors().append(new Selector(new IdSelector("id")));
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        rule.declarations().append(new Declaration(Property.MARGIN, NumericalValue.of(5, "px")));

        assertThat(StyleWriter.inline().writeSnippet(rule)).isEqualTo(".class, #id {display:none; margin:5px}");
    }

    @Test
    public void writeCompressed() throws IOException {
        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        rule.selectors().append(new Selector(new IdSelector("id")));
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        rule.declarations().append(new Declaration(Property.MARGIN, NumericalValue.of(5, "px")));

        assertThat(StyleWriter.compressed().writeSnippet(rule)).isEqualTo(".class,#id{display:none;margin:5px}");
    }

    @Test
    public void writeWhenDetached() throws IOException {
        Stylesheet stylesheet = new Stylesheet(null);

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        rule.selectors().append(new Selector(new IdSelector("id")));
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        rule.declarations().append(new Declaration(Property.MARGIN, NumericalValue.of(5, "px")));
        stylesheet.append(rule);

        rule.destroy();

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo("");
    }

    @Test
    public void writeWhenFirstSelectorDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        Selector selector = new Selector(new ClassSelector("class1"));
        Selector selector2 = new Selector(new ClassSelector("class2"));
        Selector selector3 = new Selector(new ClassSelector("class3"));
        rule.selectors().append(selector).append(selector2).append(selector3);
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        stylesheet.append(rule);

        selector.destroy();

        assertThat(StyleWriter.inline().writeSnippet(stylesheet)).isEqualTo(".class2, .class3 {display:none}");
    }

    @Test
    public void writeWhenMiddleSelectorDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        Selector selector = new Selector(new ClassSelector("class1"));
        Selector selector2 = new Selector(new ClassSelector("class2"));
        Selector selector3 = new Selector(new ClassSelector("class3"));
        rule.selectors().append(selector).append(selector2).append(selector3);
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        stylesheet.append(rule);

        selector2.destroy();

        assertThat(StyleWriter.inline().writeSnippet(stylesheet)).isEqualTo(".class1, .class3 {display:none}");
    }

    @Test
    public void writeWhenLastSelectorDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        Selector selector = new Selector(new ClassSelector("class1"));
        Selector selector2 = new Selector(new ClassSelector("class2"));
        Selector selector3 = new Selector(new ClassSelector("class3"));
        rule.selectors().append(selector).append(selector2).append(selector3);
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        stylesheet.append(rule);

        selector3.destroy();

        assertThat(StyleWriter.inline().writeSnippet(stylesheet)).isEqualTo(".class1, .class2 {display:none}");
    }

    @Test
    public void writeWhenAllSelectorsAreDetached() throws IOException {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        Selector selector = new Selector(new ClassSelector("class"));
        rule.selectors().append(selector);
        rule.declarations().append(new Declaration(Property.DISPLAY, KeywordValue.of(Keyword.NONE)));
        stylesheet.append(rule);

        selector.destroy();

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo("");
    }

    @Test
    public void writeWhenFistDeclarationDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        Declaration declaration = new Declaration(Property.MARGIN, NumericalValue.of(1, "px"));
        Declaration declaration2 = new Declaration(Property.MARGIN, NumericalValue.of(2, "px"));
        Declaration declaration3 = new Declaration(Property.MARGIN, NumericalValue.of(3, "px"));
        rule.declarations().append(declaration).append(declaration2).append(declaration3);
        stylesheet.append(rule);

        declaration.destroy();

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo(".class {\n  margin: 2px;\n  margin: 3px;\n}");
    }

    @Test
    public void writeWhenMiddleDeclarationDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        Declaration declaration = new Declaration(Property.MARGIN, NumericalValue.of(1, "px"));
        Declaration declaration2 = new Declaration(Property.MARGIN, NumericalValue.of(2, "px"));
        Declaration declaration3 = new Declaration(Property.MARGIN, NumericalValue.of(3, "px"));
        rule.declarations().append(declaration).append(declaration2).append(declaration3);
        stylesheet.append(rule);

        declaration2.destroy();

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo(".class {\n  margin: 1px;\n  margin: 3px;\n}");
    }

    @Test
    public void writeWhenLastDeclarationDetached() {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        Declaration declaration = new Declaration(Property.MARGIN, NumericalValue.of(1, "px"));
        Declaration declaration2 = new Declaration(Property.MARGIN, NumericalValue.of(2, "px"));
        Declaration declaration3 = new Declaration(Property.MARGIN, NumericalValue.of(3, "px"));
        rule.declarations().append(declaration).append(declaration2).append(declaration3);
        stylesheet.append(rule);

        declaration3.destroy();

        assertThat(StyleWriter.inline().writeSnippet(stylesheet)).isEqualTo(".class {margin:1px; margin:2px}");
    }

    @Test
    public void writeWhenNoDeclarations() throws IOException {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        rule.selectors().append(new Selector(new IdSelector("id")));
        stylesheet.append(rule);

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo("");
    }

    @Test
    public void writeVerboseWhenAllDeclarationsDetached() throws IOException {
        Stylesheet stylesheet = new Stylesheet();

        Rule rule = new Rule();
        rule.selectors().append(new Selector(new ClassSelector("class")));
        Declaration declaration = new Declaration(Property.MARGIN, NumericalValue.of(5, "px"));
        rule.declarations().append(declaration);
        stylesheet.append(rule);

        declaration.destroy();

        assertThat(StyleWriter.verbose().writeSnippet(stylesheet)).isEqualTo("");
    }
}
