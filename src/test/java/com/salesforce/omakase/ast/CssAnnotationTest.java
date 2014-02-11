/*
 * Copyright (C) 2014 salesforce.com, inc.
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

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Unit tests for {@link CssAnnotation}.
 *
 * @author nmcwilliams
 */
@SuppressWarnings("JavaDoc")
public class CssAnnotationTest {
    @Test
    public void testName() {
        CssAnnotation a = new CssAnnotation("test");
        assertThat(a.name()).isEqualTo("test");
    }

    @Test
    public void argumentWhenNoArgs() {
        CssAnnotation a = new CssAnnotation("test");
        assertThat(a.argument().isPresent()).isFalse();
    }

    @Test
    public void argumentWhenOneArg() {
        CssAnnotation a = new CssAnnotation("test", "one");
        assertThat(a.argument().get()).isEqualTo("one");
    }

    @Test
    public void argumentWhenMultipleArgs() {
        CssAnnotation a = new CssAnnotation("test", "one", "two");
        assertThat(a.argument().get()).isEqualTo("one");
    }

    @Test
    public void argumentAtIndexInBounds() {
        CssAnnotation a = new CssAnnotation("test", "one", "two");
        assertThat(a.argument(1).get()).isEqualTo("two");
    }

    @Test
    public void argumentAtIndexOutOfBounds() {
        CssAnnotation a = new CssAnnotation("test", "one", "two");
        assertThat(a.argument(2).isPresent()).isFalse();
    }

    @Test
    public void argumentAtIndexNoArgs() {
        CssAnnotation a = new CssAnnotation("test");
        assertThat(a.argument(2).isPresent()).isFalse();
    }

    @Test
    public void allArgumentsNoArgsPresent() {
        CssAnnotation a = new CssAnnotation("test");
        assertThat(a.arguments()).isEmpty();
    }

    @Test
    public void allArgumentsArgsPresent() {
        CssAnnotation a = new CssAnnotation("test", "one", "two");
        assertThat(a.arguments()).contains("one", "two");
    }

    @Test
    public void hasArgumentFalseNoArgs() {
        CssAnnotation a = new CssAnnotation("test");
        assertThat(a.hasArgument("one")).isFalse();
    }

    @Test
    public void hasArgumentFalseDifferentArg() {
        CssAnnotation a = new CssAnnotation("test", "a");
        assertThat(a.hasArgument("one")).isFalse();
    }

    @Test
    public void hasArgumentTrue() {
        CssAnnotation a = new CssAnnotation("test", "one");
        assertThat(a.hasArgument("one")).isTrue();
    }
}