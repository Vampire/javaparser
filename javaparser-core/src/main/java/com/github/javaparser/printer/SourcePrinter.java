/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.printer;

import com.github.javaparser.Position;
import com.github.javaparser.utils.Utils;

import java.util.Deque;
import java.util.LinkedList;

import static com.github.javaparser.Position.*;

public class SourcePrinter {
    private final String endOfLineCharacter;
    private final String indentation;

    private final Deque<String> indents = new LinkedList<>();
    private final StringBuilder buf = new StringBuilder();
    private Position cursor = new Position(1, 0);
    private boolean indented = false;

    SourcePrinter(final String indentation, final String endOfLineCharacter) {
        this.indentation = indentation;
        this.endOfLineCharacter = endOfLineCharacter;
        indents.push("");
    }

    /**
     * Add the default indentation to the current indentation and push it on the indentation stack.
     * Does not actually output anything.
     */
    public SourcePrinter indent() {
        String currentIndent = indents.peek();
        indents.push(currentIndent + indentation);
        return this;
    }

    /**
     * Add spaces to the current indentation until it is reaches "column" and push it on the indentation stack.
     * Does not actually output anything.
     */
    public SourcePrinter indentTo(int column) {
        if (indents.isEmpty()) {
            throw new IllegalStateException("Indent/unindent calls are not well-balanced.");
        }
        final String lastIndent = indents.peek();
        if(column< lastIndent.length()){
            throw new IllegalStateException("Attempt to indent less than the previous indent.");
        }

        StringBuilder newIndent = new StringBuilder(lastIndent);
        while (newIndent.length() < column) {
            newIndent.append(' ');
        }
        indents.push(newIndent.toString());
        return this;
    }

    /**
     * Pop the last indentation of the indentation stack.
     * Does not actually output anything.
     */
    public SourcePrinter unindent() {
        if (indents.isEmpty()) {
            // Since we start out with an empty indent on the stack, this will only occur
            // the second time we over-unindent.
            throw new IllegalStateException("Indent/unindent calls are not well-balanced.");
        }
        indents.pop();
        return this;
    }

    private void append(String arg) {
        buf.append(arg);
        cursor = cursor.withColumn(cursor.column + arg.length());
    }

    /**
     * Append the source string passed as argument to the buffer.
     * If this is being appended at the beginning of a line, performs indentation first.
     * <p>
     * The source line to be printed should not contain newline/carriage-return characters;
     * use {@link #println(String)} to automatically append a newline at the end of the source string.
     * If the source line passed as argument contains newline/carriage-return characters would
     * impredictably affect a correct computation of the current {@link #getCursor()} position.
     *
     * @param arg source line to be printed (should not contain newline/carriage-return characters)
     * @return this instance, for nesting calls to method as fluent interface
     * @see SourcePrinter#println(String)
     */
    public SourcePrinter print(final String arg) {
        if (!indented) {
            append(indents.peek());
            indented = true;
        }
        append(arg);
        return this;
    }

    /**
     * Append the source string passed as argument to the buffer, then append a newline.
     * If this is being appended at the beginning of a line, performs indentation first.
     * <p>
     * The source line to be printed should not contain newline/carriage-return characters.
     * If the source line passed as argument contains newline/carriage-return characters would
     * impredictably affect a correct computation of the current {@link #getCursor()} position.
     *
     * @param arg source line to be printed (should not contain newline/carriage-return characters)
     * @return this instance, for nesting calls to method as fluent interface
     */
    public SourcePrinter println(final String arg) {
        print(arg);
        println();
        return this;
    }

    /**
     * Append a newline to the buffer.
     *
     * @return this instance, for nesting calls to method as fluent interface
     */
    public SourcePrinter println() {
        buf.append(endOfLineCharacter);
        cursor = pos(cursor.line + 1, 0);
        indented = false;
        return this;
    }

    /**
     * Return the current cursor position (line, column) in the source printer buffer.
     * <p>
     * Please notice in order to guarantee a correct computation of the cursor position,
     * this printer expect the contracts of the methods {@link #print(String)} and {@link #println(String)}
     * has been respected through all method calls, meaning the source string passed as argument to those method
     * calls did not contain newline/carriage-return characters.
     *
     * @return the current cursor position (line, column).
     */
    public Position getCursor() {
        return cursor;
    }

    /**
     * @return the currently printed source code.
     */
    public String getSource() {
        return buf.toString();
    }

    /**
     * @return the currently printed source code.
     */
    @Override
    public String toString() {
        return getSource();
    }

    /**
     * Changes all EOL characters in "content" to the EOL character this SourcePrinter is using.
     */
    public String normalizeEolInTextBlock(String content) {
        return Utils.normalizeEolInTextBlock(content, endOfLineCharacter);
    }

    /**
     * Set the indent of the next line to the column the cursor is currently in.
     * Does not actually output anything.
     */
    public void indentToCursor() {
        indentTo(cursor.column);
    }

    /**
     * Adds an indent to the top of the stack that is a copy of the current top indent.
     * With this you announce "I'm going to indent the next line(s)" but not how far yet.
     * Once you do know, you can pop this indent ("unindent") and indent to the right column.
     * (Does not actually output anything.)
     */
    public void duplicateIndent() {
        indents.push(indents.peek());
    }
}
