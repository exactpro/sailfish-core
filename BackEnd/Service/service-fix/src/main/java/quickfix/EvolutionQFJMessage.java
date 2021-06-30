/*******************************************************************************
 *   Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package quickfix;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quickfixj.CharsetSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.MsgType;

public final class EvolutionQFJMessage extends Message {
    private static final Logger logger = LoggerFactory.getLogger(EvolutionQFJMessage.class);
    private static final String SOH = "\001";
    private static final String KEY_VALUE_DELIMITER = "=";
    private static final long serialVersionUID = 8055963317288188272L;

    private final String body;
    public static final int[] MANDATORY_TAGS = new int[] { BeginString.FIELD, BodyLength.FIELD, MsgType.FIELD };

    /**
     * Converts the byte representation of the FIX protocol into a lightweight object
     * Note: The message must conform to the FIX protocol. Header must strictly contain tags 8, 9, 35, trailer tag 10. SOH field separator
     * @param array property name to convert
     * @throws InvalidMessage - throws an exception when sending an invalid message
     */
    public EvolutionQFJMessage(byte[] array) throws InvalidMessage {
        String messageData = CharsetSupport.getCharsetInstance().decode(ByteBuffer.wrap(array)).toString();
        int indexOf35tag = messageData.indexOf(MsgType.FIELD + KEY_VALUE_DELIMITER);
        if (indexOf35tag == -1) {
            throw new InvalidMessage("Not valid message: " + messageData + ", missing tag " + MsgType.FIELD);
        }
        int indexOfSOHBefore35tag = messageData.indexOf(SOH, indexOf35tag);
        String header = messageData.substring(0, indexOfSOHBefore35tag);
        String trailer = messageData.substring(messageData.length() - 7);
        body = messageData.substring(indexOfSOHBefore35tag + 1, messageData.length() - 7);
        parse(header, true);
        checkHeader(header);
        parse(trailer, false);
        checkTrailer(trailer);
    }

    private void parse(@NotNull String fixString, boolean isHeader) throws InvalidMessage {
        String[] pairs = fixString.split(SOH);
        for (String pair : pairs) {
            String[] keyValue = pair.split(KEY_VALUE_DELIMITER);
            try {
                Integer tag = Integer.valueOf(keyValue[0]);

                String value = keyValue[1];
                if (isHeader) {
                    parseHeader(tag, value);
                } else {
                    parseTrailer(tag, value);
                }
            } catch (NumberFormatException e) {
                throw new InvalidMessage("Bad tag format: " + keyValue[0] + " ." + e.getMessage());
            }
        }
    }

    private void parseTrailer(@NotNull Integer tag, String value) throws InvalidMessage {
        if (tag == CheckSum.FIELD) {
            getTrailer().setString(CheckSum.FIELD, value);
        } else {
            throw new InvalidMessage("Trailer contains invalid tag " + tag + ". Valid tag value: 10.");
        }
    }

    private void parseHeader(@NotNull Integer tag, String value) throws InvalidMessage {
        switch (tag) {
        case BeginString.FIELD:
        case MsgType.FIELD:
        case BodyLength.FIELD:
            getHeader().setString(tag, value);
            break;
        default:
            throw new InvalidMessage("Header contains invalid tag " + tag + ". Valid tag values: 8, 9, 35.");
        }
    }

    private void checkHeader(String header) throws InvalidMessage {
        for (int tag : MANDATORY_TAGS) {
            try {
                getHeader().getString(tag);
            } catch (FieldNotFound fieldNotFound) {
                throw new InvalidMessage("Not valid header: " + header + ", missing " + tag + " tag. Valid tag values: 8, 9, 35.");
            }
        }
    }

    private void checkTrailer(String trailer) throws InvalidMessage {
        try {
            getTrailer().getString(CheckSum.FIELD);
        } catch (FieldNotFound fieldNotFound) {
            throw new InvalidMessage("Not valid trailer: " + trailer + ", missing " + CheckSum.FIELD + " tag. Valid tag value: 10");
        }
    }

    private boolean checkNext(@NotNull Iterator<Field<?>> iterator, int tag){
        if (!iterator.hasNext()) {
            return false;
        }
        return iterator.next().getTag() == tag;
    }

    @Override
    protected void calculateString(@NotNull StringBuilder buffer, int[] excludedFields, int[] postFields) {
        buffer.append(body);
    }

    @Override
    public int bodyLength() {
        return getHeader().calculateLength() + body.length() + getTrailer().calculateLength();
    }

    @Override
    int calculateChecksum() {
        return MessageUtils.checksum(body);
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pushBack(StringField field) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void fromString(String messageData, DataDictionary sessionDictionary,
            DataDictionary applicationDictionary, boolean doValidation) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void fromString(String messageData, DataDictionary dd, boolean doValidation) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Object clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getFieldOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFields(FieldMap fieldMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroups(FieldMap fieldMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setString(int field, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBytes(int field, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBoolean(int field, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setChar(int field, char value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInt(int field, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDouble(int field, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDouble(int field, double value, int padding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDecimal(int field, BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDecimal(int field, BigDecimal value, int padding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeStamp(int field, Timestamp value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeStamp(int field, Timestamp value, boolean includeMilliseconds, boolean includeMicroseconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeStamp(int field, Timestamp value, boolean includeMilliseconds, boolean includeMicroseconds, boolean includeNanoseconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeOnly(int field, Timestamp value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeOnly(int field, Timestamp value, boolean includeMillseconds, boolean includeMicroseconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcTimeOnly(int field, Timestamp value, boolean includeMillseconds, boolean includeMicroseconds, boolean includeNanoseconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUtcDateOnly(int field, Timestamp value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public char getChar(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getDecimal(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getUtcTimeStamp(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getUtcTimeOnly(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getUtcDateOnly(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(int key, Field<?> field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(StringField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(BooleanField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(CharField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(IntField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(DoubleField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(DecimalField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(UtcTimeStampField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(UtcTimeOnlyField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(UtcDateOnlyField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(BytesField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytesField getField(BytesField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringField getField(StringField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BooleanField getField(BooleanField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharField getField(CharField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntField getField(IntField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DoubleField getField(DoubleField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DecimalField getField(DecimalField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UtcTimeStampField getField(UtcTimeStampField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UtcTimeOnlyField getField(UtcTimeOnlyField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UtcDateOnlyField getField(UtcDateOnlyField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSetField(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSetField(Field<?> field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeField(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Field<?>> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getGroupCount(int tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Integer> groupKeyIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Integer, List<Group>> getGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addGroup(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addGroupRef(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> getGroups(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Group getGroup(int num, Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Group getGroup(int num, int groupTag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceGroup(int num, Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGroup(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGroup(int num, int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGroup(int num, Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGroup(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasGroup(int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasGroup(int num, int field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasGroup(int num, Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasGroup(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setField(StringField field, boolean duplicateTagsAllowed) {
        throw new UnsupportedOperationException();
    }
}

