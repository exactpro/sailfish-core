/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.testwebgui.download;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class PagedFileViewer implements Closeable {

    private static final int BUFF = 8192;

    private static final String SEPARATOR = System.lineSeparator();

    private static final int SEPARATOR_LENGTH = SEPARATOR.length();

    private final int lines;

    private final int width;

    private final RandomAccessFile rac;

    private final FileChannel channel;

    private final CharsetDecoder decoder = Charset.forName("UTF8").newDecoder();

    private long currentPosition;

    private long nextPagePosition;

    private boolean notFound;

    private boolean findInCurrentPage;

    private int lineCounter;

    public PagedFileViewer(File file, int lines, int width) throws IOException {
        this.lines = lines;
        this.width = width;
        this.currentPosition = 0l;
        this.nextPagePosition = 0l;
        this.rac = new RandomAccessFile(file, "r");
        this.channel = rac.getChannel();
        this.notFound = false;
        this.findInCurrentPage = true;
    }

    public String readNextPage() throws IOException {
        return readLines(false);
    }

    public String readPrevPage() throws IOException {
        return readLines(true);
    }

    public String readFirstPage() throws IOException {

        nextPagePosition = 0l;

        return readLines(false);
    }

    public String readLastPage() throws IOException {

        currentPosition = channel.size() - 1;

        return readLines(true);
    }

    private String readLines(boolean reverse) throws IOException {

        int bufferSize = BUFF;
        lineCounter = 0;

        if (reverse) {
            nextPagePosition = currentPosition;
            if (currentPosition - BUFF < 0) {
                bufferSize = (int) currentPosition;
            }
            channel.position(currentPosition - bufferSize);
        } else {
            currentPosition = nextPagePosition;
            channel.position(currentPosition);
            if (currentPosition + BUFF > channel.size() - 1) {
                bufferSize = (int) (channel.size() - 1 - currentPosition);
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        if (channel.read(buffer) != -1) {
            buffer.position(0);
            CharBuffer charBuffer;

            try {
                charBuffer = decoder.decode(buffer);
            } catch (CharacterCodingException e) {
                throw new IOException("Problem with character decoding", e);
            }

            replaceASCIIcontrols(charBuffer);
            StringBuilder builder = new StringBuilder();
            charBuffer.position(0);
            int fakeSeparators;

            if (reverse) {
                StringBuilder reverseBuilder = new StringBuilder(charBuffer);
                reverseBuilder.reverse();
                charBuffer.clear();
                charBuffer = CharBuffer.wrap(reverseBuilder);
                reverseBuilder = null;
                fakeSeparators = createLines(charBuffer, builder, lines + 1);
                if (lineCounter < lines) {
                    nextPagePosition = 0l;
                    return readLines(false);
                }
                currentPosition = currentPosition - builder.length() + fakeSeparators;
            } else {
                fakeSeparators = createLines(charBuffer, builder, lines);
                if (lineCounter < lines) {
                    for (int i = 0; i < lines - lineCounter; i++) {
                        builder.append(SEPARATOR);
                    }
                }
                nextPagePosition = currentPosition + builder.length() - fakeSeparators;
            }

            return reverse ? StringEscapeUtils.escapeHtml4(builder.reverse().toString()) : StringEscapeUtils.escapeHtml4(builder.toString());
        }

        return StringUtils.EMPTY;
    }

    public String find(String searchString, boolean reverse) throws IOException {

        long tempPageIndex = currentPosition;
        searchString = StringEscapeUtils.escapeHtml4(searchString);

        if (findInCurrentPage) {
            nextPagePosition = currentPosition;
            findInCurrentPage = false;
        }

        while (reverse ? !isPrevPageNotAvailable() : !isNextPageNotAvailable()) {

            String page = reverse ? readPrevPage() : readNextPage();

            if (page.contains(searchString)) {
                notFound = false;
                StringBuilder inserter = new StringBuilder(page);
                int index = 0;
                while (index < inserter.length()) {
                    int phraseIndex = inserter.indexOf(searchString, index);
                    if (phraseIndex != -1) {
                        inserter.insert(phraseIndex, "<span class='highlight'>");
                        int endTagIndex = inserter.indexOf(searchString, index) + searchString.length();
                        inserter.insert(endTagIndex, "</span>");
                        index = endTagIndex;
                    } else {
                        index = inserter.length();
                    }
                }
                return inserter.toString();
            }
        }

        notFound = true;
        findInCurrentPage = true;
        nextPagePosition = tempPageIndex;

        return readNextPage();
    }

    private int createLines(CharBuffer buffer, StringBuilder out, int lines) {
        int fakeSeparators = 0;
        if (lines > 0 && buffer.remaining() > 0) {
            int index = StringUtils.indexOf(buffer, SEPARATOR);
            if (index < 0 || index > width) {
                index = buffer.remaining() < width ? buffer.remaining() : width;
                out.append(buffer.subSequence(0, index));
                out.append(SEPARATOR);
                fakeSeparators++;
                buffer.position(buffer.position() + index);
                lineCounter++;
            } else {
                out.append(buffer.subSequence(0, index + SEPARATOR_LENGTH));
                buffer.position(buffer.position() + index + SEPARATOR_LENGTH);
                lineCounter++;
            }
            fakeSeparators += createLines(buffer, out, lines - 1);
        }
        return fakeSeparators;
    }

    private void replaceASCIIcontrols(CharBuffer charBuffer) {
        char[] chars = charBuffer.array();
        for (int i = 0; i < chars.length; i++) {
            int ch = chars[i];
            if ((ch < 32) && (ch != 9) && (ch != 10) && (ch != 13)) {
                chars[i] = (char) (0x2400 + ch);
            }
        }
    }

    public boolean isPrevPageNotAvailable() {
        return currentPosition == 0l;
    }

    public boolean isNextPageNotAvailable() throws IOException {
        return nextPagePosition >= channel.size() - 1;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public void setFindInCurrentPage(boolean findInCurrentPage) {
        this.findInCurrentPage = findInCurrentPage;
    }

    @Override
    public void close() throws IOException {
        rac.close();
    }
}
