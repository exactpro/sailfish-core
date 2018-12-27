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
package com.exactpro.sf.testwebgui.help;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import com.taskadapter.redmineapi.bean.Attachment;
import com.taskadapter.redmineapi.bean.WikiPageDetail;

import net.sf.textile4j.Textile;

public class TextileAdapter {
    private static Textile textile = new Textile();
    private static WikiPageDetail wikiPage;

    public static String process(WikiPageDetail pd) {

        String inputString = pd.getText();
        wikiPage = pd;

        String preprocessed = preProcess(inputString);
        String processed = textile.process(preprocessed);
        String postprocessed = postProcess(processed);

        return postprocessed;
    }

    private static String preProcess(String inputString) {
        String result = replaceItalic(inputString);
        result = replaceLinks(result);
        result = replaceBlockQuotes(result);
        result = removeImages(result);
        result = replaceTags(result);
        result = replaceNestedBold(result);
        return result;
    }

    private static String postProcess(String inputString) {
        String result = replaceAttachments(inputString);
        result = buildTable(result);
        result = replaceLineThrough(result);
        result = processTags(result);
        result = buildSubLists(result);
        result = buildMonoSpaces(result);
        result = replaceImagesTag(result);
        result = removeElements(result);
        result = removeHttpLinks(result);
        result = fixBold(result);
        return result;
    }

    private static String replaceItalic(String inputString) {
        Pattern pattern = Pattern.compile("([\\s\r\n])_(.+)_\\s");
        Matcher matcher = pattern.matcher(inputString);

        while(matcher.find()) {
            inputString = inputString.replace(matcher.group(0),
                    new StringBuilder().append(matcher.group(1)).append(" &lt;em&gt; ").append(matcher.group(2)).append(" &lt;/em&gt; ").toString());
        }
        return inputString;
    }

    private static String replaceNestedBold(String inputString) {
        String result = inputString.replaceAll("\r\n", "\0");
        Pattern pattern = Pattern.compile("\\*([^\\s\\*].+?)\\*([^\\*])");
        Matcher matcher = pattern.matcher(result);

        while(matcher.find()) {
            result = result.replace(matcher.group(0),
                    new StringBuilder().append("<strong>").append(matcher.group(1)).append("</strong>").append(matcher.group(2)).toString());
        }

        return result.replaceAll("\0", "\r\n");
    }

    private static String fixBold(String inputString) {
        String result = inputString.replaceAll("\r\n", "\0");
        Pattern pattern = Pattern.compile("\\*([\\w]+)\\*");
        Matcher matcher = pattern.matcher(result);

        while(matcher.find()) {
            result = result.replace(matcher.group(0), new StringBuilder().append("<strong>").append(matcher.group(1)).append("</strong>").toString());
        }

        pattern = Pattern.compile("<strong>\\s*</strong>\\s*(<ins>.*?</ins>)\\*");
        matcher = pattern.matcher(result);
        while(matcher.find()) {
            result = result.replace(matcher.group(0), new StringBuilder().append("<strong>").append(matcher.group(1)).append("</strong>").toString());
        }

        result = result.replaceAll("#8216;", "‘").replaceAll("&‘", "‘");

        return result.replaceAll("\0", "\r\n");
    }

    private static String buildMonoSpaces(String inputString) {
        String result = inputString.replaceAll("\r\n", "\0");
        Pattern pattern = Pattern.compile("[^a-zA-z_0-9=](@(.*?)@)[^a-zA-z_0-9=]");
        Matcher matcher = pattern.matcher(result);

        while(matcher.find()) {
            result = result.replace(matcher.group(1), new StringBuilder().append("<code>").append(matcher.group(2)).append("</code>").toString());
        }
        return result.replaceAll("\0", "\r\n");
    }

    private static String replaceTags(String inputString) {
        List<String> stringList = Arrays.asList(inputString.split("\\r\\n"));
        boolean isPre = false;

        ListIterator<String> iterator = stringList.listIterator();
        while(iterator.hasNext()) {
            String line = iterator.next();
            if(line.contains("<") && line.indexOf(">", line.indexOf("<")) > -1) {

                if(line.contains("<pre>")) {
                    isPre = true;
                }
                if(isPre) {
                    continue;
                }

                line = line.replaceAll("\\<", "[lt").replaceAll(">", "]gt");
                iterator.set(line);

                if(line.contains("</pre>")) {
                    isPre = false;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for(String s : stringList) {
            sb.append(s).append("\r\n");
        }
        return sb.toString();
    }

    private static String removeImages(String inputString) {
        return removeByRegex(inputString, "!([^\\s\\(=^!]+?)\\s?(\\(([^\\)]+?)\\))?!");
    }

    private static String replaceBlockQuotes(String inputString) {
        List<String> stringList = Arrays.asList(inputString.split("\\r\\n"));
        boolean blockQuoteStarted = false;
        List<Integer> linesToReplace = new ArrayList<>();

        for(int i=0; i<stringList.size(); i++) {
            String cur = stringList.get(i);
            if(cur.startsWith(">")) {
                linesToReplace.add(i);
                stringList.set(i, stringList.get(i).substring(1));
                if(!blockQuoteStarted) blockQuoteStarted = true;
            } else {
                if(blockQuoteStarted) {
                    //do replace
                    for(int line : linesToReplace) {
                        stringList.set(line, "\r\nbq. " + stringList.get(line).replaceAll("\r\n", "") + "&nbsp;");
                    }
                    String last = stringList.get(linesToReplace.get(linesToReplace.size()-1));

                    stringList.set(linesToReplace.get(linesToReplace.size()-1), last + "\r\n");
                    linesToReplace.clear();
                    blockQuoteStarted = false;
                }
            }
        }

        if(blockQuoteStarted) {
            for(int line : linesToReplace) {
                stringList.set(line, "\r\nbq. " + stringList.get(line).replaceAll("\r\n", "") + "&nbsp;");
            }
        }

        StringBuilder sb = new StringBuilder();
        for(String s : stringList) {
            sb.append(s).append("\r\n");
        }

        return sb.toString();
    }

    private static String replaceImagesTag(String inputString) {
        inputString = inputString.replaceAll("&#8211;", "-");
        inputString = inputString.replaceAll("&#215;", "x");
        Pattern pattern = Pattern.compile("\\{\\{img\\s(.+)\\}\\}");
        Matcher matcher = pattern.matcher(inputString);

        int iter = 0;
        while(matcher.find()) {
            String fullTag = inputString.substring(matcher.start(1), matcher.end(1));
            if(fullTag.startsWith("http")) {
                inputString = inputString.replace(matcher.group(0), fullTag);
            } else {
                inputString = inputString.replace(matcher.group(0), findAttachURL(fullTag));
            }
            matcher = pattern.matcher(inputString);
            if(iter++ > 20) break;
        }

        return inputString;
    }

    private static String findAttachURL(String name) {
        for(Attachment attachment : wikiPage.getAttachments()) {
            if(attachment.getFileName().equals(StringEscapeUtils.unescapeHtml4(name))) {
                return attachment.getContentURL();
            }
        }
        return "";
    }

    private static String processTags(String inputString) {
        String result = inputString.replaceAll("</blockquote>(\r\n)*(\t)*<blockquote>", "\r\n");
        result = result.replaceAll("\\[lt","&lt;").replaceAll("\\]gt", "&gt;");
        return result;
    }

    private static String removeElements(String inputString) {
        String result = inputString.replaceAll("\\{\\{.+?\\}\\}", "");
        StringBuilder sb = new StringBuilder(result);

        int position = 0;
        while(sb.indexOf("\r\n", position) > -1) {
            int index = sb.indexOf("\r\n", position);
            Character prev = sb.charAt(index - 1);
            position = index + 4;
            if(prev.equals('>') || prev.equals('\n')) {
                continue;
            }

            sb.replace(index, index + "\r\n".length(), "<br />");

        }

        return sb.toString();
    }

    private static String buildSubLists(String inputString) {

        Pattern p = Pattern.compile("[^\\.]\\*\\s(.+)");
        Matcher m = p.matcher(inputString);

        while(m.find()) {
            inputString = inputString.replace(m.group(0), new StringBuilder().append("<ul><li>").append(m.group(1)).append("</li></ul>").toString());
        }

        String pattern = "<p>**";
        if(inputString.contains(pattern)) {


            StringBuilder sb = new StringBuilder(inputString);

            while (sb.indexOf(pattern) > -1) {
                int start = sb.indexOf(pattern);
                int end = sb.indexOf("</p>", start);

                String subListElem = sb.substring(start + pattern.length(), end);
                String htmlElem = new StringBuilder().append("<ul><li style='list-style-type: none;'><ul><li>").append(subListElem).append("</li></ul></li></ul>").toString();
                sb.replace(start, end + "</p>".length(), htmlElem);
            }
            inputString = sb.toString();
        }

        p = Pattern.compile("</ol>(([\r\n\t]*<p>##(.+?)</p>[\r\n\t]*)+)<ol>");
        m = p.matcher(inputString);

        while(m.find()) {
            String raw = m.group(1).substring(m.group(1).indexOf("<p>##") + "<p>##".length(), m.group(1).lastIndexOf("</p>"));
            String [] subItems = raw.split("</p>[\r\n\t]*<p>##");
            StringBuilder builder = new StringBuilder("<li style='list-style-type: none;'><ol>");
            for(String sub : subItems) {
                builder.append("<li>").append(sub).append("</li>");
            }
            builder.append("</ol></li>");
            inputString = inputString.replace(m.group(0), builder.toString());
        }

        return inputString;
    }

    private static String replaceLineThrough(String inputString) {
        String result = inputString.replaceAll("<del>", "");
        result = result.replaceAll("</del>", "");
        return result;
    }

    private static String buildTable(String inputString) {
        List<String> stringList = new ArrayList<>(Arrays.asList(inputString.split("\\n")));
        StringBuilder table = new StringBuilder();
        boolean isTableOpened = false;
        List<Integer> rowsWithTable = new ArrayList<>();

        for(int i=0;i<stringList.size();i++)
        {
            String line = stringList.get(i);
            if(isTableOpened && line.isEmpty()) {
                rowsWithTable.add(i);
                continue;
            }

            boolean isTable = line.contains(" |");

            if(isTable)
            {
                rowsWithTable.add(i);
                String [] cells = line.substring(line.indexOf("|") + 1, line.lastIndexOf("|")).split("\\|", -1);

                if(!isTableOpened)
                {
                    table.append("<table class='eps-redmine-table'>");
                    isTableOpened = true;
                }

                StringBuilder row = new StringBuilder("<tr>");
                for(String cell: cells)
                {
                    row.append("<td class='eps-true-table'>").append(cell).append("</td>");
                }
                row.append("</tr>");
                table.append(row);
            }
            else
            {
                if(isTableOpened)
                {
                    table.append("</table>");
                    isTableOpened = false;

                    String tagBefore = stringList.get(rowsWithTable.get(0)).substring(0, stringList.get(rowsWithTable.get(0)).indexOf(" |"));
                    int pos = tagBefore.indexOf("<") + 1;
                    String tagAfter = new StringBuilder().append(tagBefore.substring(pos-1, pos)).append("/").append(tagBefore.substring(pos)).toString();

                    for(int j=rowsWithTable.size()-1;j>=0;j--) {
                        stringList.remove((int)rowsWithTable.get(j));
                        i--;
                    }
                    stringList.add(rowsWithTable.get(0), tagBefore.concat(table.toString()).concat(tagAfter));
                    table = new StringBuilder();
                    rowsWithTable.clear();
                    i++;
                }
            }

        }

        StringBuilder sb = new StringBuilder();
        for(String s : stringList) {
            sb.append(s).append("\r\n");
        }

        return sb.toString();

    }

    private static String removeHttpLinks(String inputString) {
        return removeByRegex(inputString, "<a (.*?)(http|https)://(.*?)>(.*?)</a>");
    }

    private static String removeByRegex(String inputString, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        StringBuilder sb = new StringBuilder(inputString);

        Set<String> wrapped = new LinkedHashSet<>();

        while (matcher.find()) {
            wrapped.add(matcher.group());
        }

        for (String str : wrapped) {
            int from = -1;
            int to = -1;
            while ((from = sb.indexOf(str)) != -1) {
                to = from + str.length();
                sb.replace(from, to, "");

            }
        }

        return sb.toString();
    }

    private static String replaceAttachments(String inputString) {

        int start = inputString.indexOf("attachment:");
        if (start < 0) {
            return inputString;
        }

        int fakeSize = "attachment:".length();
        StringBuilder sb = new StringBuilder(inputString);

        while (sb.indexOf("attachment:") > -1) {
            start = sb.indexOf("attachment:");
            int dotPosition = sb.indexOf(".", start + fakeSize);
            StringBuilder title = new StringBuilder(sb.substring(start + fakeSize, dotPosition));

            title.append(".");
            while (Character.isLetter(sb.charAt(dotPosition + 1))) {
                title.append(sb.charAt(dotPosition + 1));
                dotPosition++;
            }

            int increasedLength = 0;

            //title starts with "
            if (title.indexOf("&#8220;") == 0) {
                increasedLength = "&#8220;".length();
                title.replace(0, increasedLength, "");
            }

            sb.replace(start, dotPosition + 1 + increasedLength, "");
        }

        return sb.toString();

    }

    private static String replaceLinks(String rawString) {
        StringBuilder sb = new StringBuilder(rawString);

        try {
            while (sb.indexOf("[[") > -1) {
                int from = sb.indexOf("[[");
                int to = sb.indexOf("]]");

                String rawLink = sb.substring(from + 2, to);
                int dividerIndex = rawLink.indexOf("|");
                String name = StringEscapeUtils.escapeHtml4(rawLink.substring(dividerIndex + 1));
                String link = StringEscapeUtils.escapeHtml4(rawLink.substring(0, dividerIndex + 1).length() > 0 ? rawLink.substring(0, dividerIndex) : name);

                String newLink = "\"" + name + "\":" + link.replaceAll(" ", "_");
                sb.replace(from, to + 2, newLink.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;"));
            }
        } catch (Exception e) {
            System.err.println("Error during link parsing. " + e.getMessage());
        }

        return sb.toString();
    }
}
