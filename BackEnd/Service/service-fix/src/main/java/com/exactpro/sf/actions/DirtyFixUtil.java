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
package com.exactpro.sf.actions;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.TypeConverter;
import com.exactpro.sf.services.fix.FixUtil;
import org.slf4j.Logger;
import quickfix.*;
import quickfix.DataDictionary.GroupInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@MatrixUtils
public class DirtyFixUtil {

	static final Charset charset = Charset.forName("ISO-8859-1");

	/**
	 * @deprecated Message content has more priority than dirty tags
	 */
	@Deprecated
	public static final String DIRTY = "Dirty";
	/**
     * @deprecated Message content has more priority than dirty tags
     */
    @Deprecated
	public static final String DIRTY_BEGIN_STRING = "DirtyBeginString";
    /**
     * @deprecated Message content has more priority than dirty tags
     */
    @Deprecated
	public static final String DIRTY_CHECK_SUM = "DirtyCheckSum";
    /**
     * @deprecated Message content has more priority than dirty tags
     */
    @Deprecated
	public static final String DIRTY_BODY_LENGTH = "DirtyBodyLength";
    /**
     * @deprecated Message content has more priority than dirty tags
     */
    @Deprecated
	public static final String DIRTY_MSG_SEQ_NUM = "DirtyMsgSeqNum";

    /**
     * @deprecated message may contain array of values
     */
	@Deprecated
	public static final String DOUBLE_TAG = "DoubleTag";
	/**
     * @deprecated message may contain array of values
     */
    @Deprecated
	public static final String DUPLICATE_TAG = "DuplicateTag";
	/*
	 * Valid logon structure
	 * Required tags
	 *  8 = <fix protocol name>=beginString
	 *  35 = MsgType
	 *  49 = SenderCompID
	 *  56 = TargetCompID
	 *  34 = MsgSqNum
	 *  52 = SendingTime
	 *  98 = EncryptMethod
	 *  108 = HeartBtInt
	 *  1137 = DefaultApplVerID
	 */


	private DirtyFixUtil() {
		// hide constructor
	}

	/**
	 * Note: original map is modified!!
	 * @param inputData
	 * @return
	 */
	static Map<String, String> extractMutateMap(Map<?, ?> inputData)
	{
		Map<String, String> mutateMap = new HashMap<>();

		if (inputData.containsKey(DIRTY_BEGIN_STRING)) {
			mutateMap.put(DIRTY_BEGIN_STRING, inputData.remove(DIRTY_BEGIN_STRING).toString());
		}
		if (inputData.containsKey(DOUBLE_TAG)) {
			mutateMap.put(DOUBLE_TAG, inputData.remove(DOUBLE_TAG).toString());
		}
		if (inputData.containsKey(DUPLICATE_TAG)) {
			mutateMap.put(DUPLICATE_TAG, inputData.remove(DUPLICATE_TAG).toString());
		}
		if (inputData.containsKey(DIRTY_CHECK_SUM)) {
			mutateMap.put(DIRTY_CHECK_SUM, inputData.remove(DIRTY_CHECK_SUM).toString());
		}
		if (inputData.containsKey(DIRTY_BODY_LENGTH)) {
			mutateMap.put(DIRTY_BODY_LENGTH, inputData.remove(DIRTY_BODY_LENGTH).toString());
		}
		if (inputData.containsKey(DIRTY_MSG_SEQ_NUM)) {
			mutateMap.put(DIRTY_MSG_SEQ_NUM, inputData.remove(DIRTY_MSG_SEQ_NUM).toString());
		}

		return mutateMap;
	}

	/**
     * Note: original map is modified!!
     * @param inputData
     * @return
     */
    public static Map<String, String> extractMutateMap(IMessage message)
    {
        HashMap<String, String> mutateMap = new HashMap<>();

        if (message.isFieldSet(DIRTY_BEGIN_STRING)) {
            mutateMap.put(DIRTY_BEGIN_STRING, message.removeField(DIRTY_BEGIN_STRING).toString());
        }
        if (message.isFieldSet(DOUBLE_TAG)) {
            mutateMap.put(DOUBLE_TAG, message.removeField(DOUBLE_TAG).toString());
        }
        if (message.isFieldSet(DUPLICATE_TAG)) {
            mutateMap.put(DUPLICATE_TAG, message.removeField(DUPLICATE_TAG).toString());
        }
        if (message.isFieldSet(DIRTY_CHECK_SUM)) {
            mutateMap.put(DIRTY_CHECK_SUM, message.removeField(DIRTY_CHECK_SUM).toString());
        }
        if (message.isFieldSet(DIRTY_BODY_LENGTH)) {
            mutateMap.put(DIRTY_BODY_LENGTH, message.removeField(DIRTY_BODY_LENGTH).toString());
        }
        if (message.isFieldSet(DIRTY_MSG_SEQ_NUM)) {
            mutateMap.put(DIRTY_MSG_SEQ_NUM, message.removeField(DIRTY_MSG_SEQ_NUM).toString());
        }

        return mutateMap;
    }

	static Message createMessage(Logger logger, HashMap<?, ?> inputData, String beginString,
								 String msgType, Pair<String, String> dictionaries) throws InterruptedException {
		Message message = new Message();
		if (beginString != null) {
			message.getHeader().setField(new StringField(8, beginString));
		}
		if (msgType != null) {
			message.getHeader().setField(new StringField(35, msgType));
		}

		enrichMessage(logger, inputData, message, beginString, msgType, dictionaries);
		return message;
	}

	static void enrichMessage(Logger logger, HashMap<?, ?> inputData, Message message, String beginString,
							  String msgType, Pair<String, String> dictionaries) throws InterruptedException {
		DataDictionary admDict = null;
		DataDictionary appDict = null;
		try {
			if (dictionaries != null) {
				String applicationDictionary = dictionaries.getFirst();
				InputStream applDictStream = new ByteArrayInputStream(applicationDictionary.getBytes(StandardCharsets.UTF_8));
				appDict = new DataDictionary(applDictStream);
				if (dictionaries.getSecond() != null) {
					InputStream admDictStream = new ByteArrayInputStream(dictionaries.getSecond().getBytes(StandardCharsets.UTF_8));
					admDict = new DataDictionary(admDictStream);
				}
			} else {
				admDict = getAdminDictionary(logger, beginString);
				appDict = getApplicationDictionary(logger, beginString);
			}
		} catch(Exception e) {
            logger.warn("Can't convert MessageDictionary to DataDictionary. Reason: {}", e.getMessage());
			admDict = getAdminDictionary(logger, beginString);
			appDict = getApplicationDictionary(logger, beginString);
		}
		if(admDict == null) {
			admDict = appDict;
		}


		for (Map.Entry<?, ?> entry : inputData.entrySet()) {
			final String tagName = entry.getKey().toString();
			final Object tagValue = entry.getValue();

			int tagInt = appDict.getFieldTag(tagName);
			String tagValueString = tagValue.toString();

            logger.debug("Work with element : {}={}", tagName, tagValueString);

            if (isComponent(tagValue)) {
				logger.debug(" -> HashMap have - do it!");
                message = DirtyFixUtil.getFromGroup(appDict, tagName, (List<?>) tagValue, message, msgType, null, null, null);
			} else {
				if (tagInt == -1) { // if field not found by key, it should be tag number or a reference (HashMap)
					try {
						tagInt = Integer.parseInt(tagName);
					} catch (NumberFormatException ex) {
						throw new EPSCommonException("Tag " + tagName + " not found in dictionary and not a tag number.");
					}
				}

				if (admDict.isHeaderField(tagInt)) {
					message.getHeader().setField(new StringField(tagInt, tagValueString));
				} else {
					if (appDict.hasFieldValue(tagInt)) // determine if a field has enumerated values
					{
						try {
                            Class<?> cls = DirtyFixUtil.class.getClassLoader().loadClass("com.exactpro.quickfix.field." + tagName);
							Object constant = TypeConverter.findConstantValue(cls, tagValueString, null);
							if (constant != null) {
								tagValueString = constant.toString();
							}
						} catch (ClassNotFoundException e) {
							// ignore exception - no such enum
						} catch (AMLException e) {
							// ignore exception
							logger.error(e.getMessage(), e);
						}
					}
					if(!message.getHeader().isSetField(tagInt)) {
						message.setField(new StringField(tagInt, tagValueString));
					}
				}
			}
		}
	}


	public static String mutate(Logger logger, String message, Map<?, ?> mutateMap) {

		FIXPacket fpacket = new FIXPacket( "","" );
		fpacket.fillPacketFromString(message);

		// used in 'positive' tests
		Object value = mutateMap.get("BeginString");
		String beginString = (value == null || "".equals(value)) ? "FIXT.1.1" : value.toString();

		// used in 'negative' test
		String dirtyBeginString = null;
		if (mutateMap.containsKey(DIRTY_BEGIN_STRING)) {
			dirtyBeginString = mutateMap.get(DIRTY_BEGIN_STRING).toString();
		}

		if (dirtyBeginString != null) {
			if ("no".equals(dirtyBeginString)) {
				fpacket.delTag("8");
                logger.debug("<DirtyBeginString>='{}' - don't send tag 8", dirtyBeginString);
			} else {
				fpacket.setTag("8", dirtyBeginString);
                logger.debug("<DirtyBeginString>='{}'", dirtyBeginString);
			}
		} else {
			fpacket.setTag("8", beginString);
            logger.debug("<BeginString>='{}'", beginString);
		}

		// DoubleTag & DuplicateTag
		String doubleTagString = null;
		if (mutateMap.containsKey(DOUBLE_TAG)) {
			doubleTagString = mutateMap.get(DOUBLE_TAG).toString();
		}
		String duplicateTagString = null;
		if (mutateMap.containsKey(DUPLICATE_TAG)) {
			duplicateTagString = mutateMap.get(DUPLICATE_TAG).toString();
		}

		if (duplicateTagString != null || doubleTagString != null) {
			// concat strings:
			String dupicateTags = "";
			dupicateTags += doubleTagString != null ? doubleTagString + ";" : "";
			dupicateTags += duplicateTagString != null ? duplicateTagString : "";

			for (String record : dupicateTags.split(";")) {
				if (!record.isEmpty()) {
					String[] chunk = record.split("=", 2);
					fpacket.setNewTag(chunk[0], chunk[1]);
				}
			}
		}

		// DirtyCheckSum
		String dirtyCheckSum = null;
		if (mutateMap.containsKey(DIRTY_CHECK_SUM)) {
			dirtyCheckSum = mutateMap.get(DIRTY_CHECK_SUM).toString();
		}

		// DirtyBodyLength
		String dirtyBodyLength = null;
		if (mutateMap.containsKey(DIRTY_BODY_LENGTH)) {
			dirtyBodyLength = mutateMap.get(DIRTY_BODY_LENGTH).toString();
		}

		String result = new String(fpacket.getInDirtyBytes(dirtyBodyLength, dirtyCheckSum), 0, fpacket.RealLen, charset);

		logger.info("Encoded message: [ {} ];", result);

		return result;

	}


    protected static Message getFromGroup(DataDictionary appDict, String tagName, List<?> tagValue, Message newMessage,
            String mtype, Group parentGroup, MessageComponent parentComponent, GroupInfo infParent) {
        // work with inner hashmap (check for have other hashmaps)
        for (Object map : tagValue) {

            int tempftag = 0;
            Group localGroup = null;
            MessageComponent localComponent = null;
            tempftag = appDict.getFieldTag(tagName);
            GroupInfo inf = null;
            if (tempftag != -1) {
                FieldType fieldType = appDict.getFieldTypeEnum(tempftag);
                if (fieldType != FieldType.NumInGroup) {
                    throw new EPSCommonException("Field " + tempftag + " must have type NUMINGROUP in FIX dictionary.");
                }
                inf = appDict.getGroup(mtype, appDict.getFieldTag(tagName));
                if (inf == null) {
                    if (infParent == null) {
                        throw new EPSCommonException("Group " + tagName + " is not in a message " + mtype + ".");
                    }
                    inf = infParent.getDataDictionary().getGroup(mtype, appDict.getFieldTag(tagName));
                    if (inf == null) {
                        throw new EPSCommonException("Message " + mtype + " does not contain group " + tagName);
                    }
                }
                localGroup = new Group(appDict.getFieldTag(tagName), inf.getDelimiterField());
                // appDict = inf.getDataDictionary();
            } else {
                localComponent = new LocalMessageComponent();
                inf = infParent;
            }

            for (Object tag : ((HashMap<?, ?>) map).keySet()) {
                // have hashmap in
                Object tempGetObject = null;
                int iTagNum = -1;
                String iTagName = null;

                iTagName = tag.toString();
                /*
                 * if (iTagName.contains(":")) { iTagName = iTagName.split(":",
                 * 2)[0]; }
                 */
                iTagNum = appDict.getFieldTag(iTagName);
                tempGetObject = ((Map<?, ?>) map).get(tag);
                if (isComponent(tempGetObject)) {
                    // go recursive
                    newMessage = getFromGroup(appDict, iTagName, (List<?>) tempGetObject, newMessage, mtype, localGroup,
                            localComponent, inf);
                } else {
                    String value = tempGetObject.toString();
                    if (appDict.hasFieldValue(iTagNum)) // determine if a field
                                                        // has enumerated values
                    {
                        try {
                            Class<?> fcls = DirtyFixUtil.class.getClassLoader()
                                    .loadClass("com.exactpro.quickfix.field." + iTagName);
                            Object constant = TypeConverter.findConstantValue(fcls, value, null);
                            if (constant != null) {
                                value = constant.toString();
                            }
                        } catch (ClassNotFoundException e) {
                            // ignore exception - no such enum
                        } catch (AMLException e) {
                            // ignore exception
                        }
                    }
                    StringField field = new StringField(iTagNum, value);
                    if (localGroup != null) {
                        localGroup.setField(field);
                    } else if (localComponent != null) {
                        localComponent.setField(field);
                    } else {
                        newMessage.setField(field);
                    }
                }
            }

            if (localGroup != null) {
                FieldMap target = (parentComponent != null) ? parentComponent : newMessage;
                target.addGroup(localGroup);
            }

            if (localComponent != null) {
                FieldMap target = (parentGroup != null) ? parentGroup : newMessage;

                Iterator<Integer> git = localComponent.groupKeyIterator();
                while (git.hasNext()) {
                    List<Group> grps = localComponent.getGroups(git.next());
                    for (Group grp : grps) {
                        target.addGroup(grp);
                    }
                }
                Iterator<Field<?>> fit = localComponent.iterator();
                while (fit.hasNext()) {
                    Field<?> fld = fit.next();
                    target.setField(new StringField(fld.getTag(), fld.getObject().toString()));
                }
            }
        }

        return newMessage;
    }

	public static DataDictionary getApplicationDictionary(Logger logger, String beginString) throws InterruptedException {
		try {
			return FixUtil.getDictionary(beginString, true);
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
			logger.info("Can't get application dictionary : {}", e);
			throw new EPSCommonException("Can not find dictionary for BeginString: " + beginString, e);
		}
	}

	public static DataDictionary getAdminDictionary(Logger logger, String beginString) throws InterruptedException {
		try {
			return FixUtil.getDictionary(beginString, false);
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
			logger.info("Can't get admin dictionary : {}", e);
			throw new EPSCommonException("Can not find dictionary for BeginString: " + beginString, e);
		}
	}

	private static boolean isComponent(Object value) {
        return value instanceof List && /*((List<?>) value).size() == 1 &&*/ ((List<?>) value).get(0) instanceof HashMap;
    }

	/*private static HashMap<?, ?> exstractComponent(Object value) {

	    return (HashMap<?, ?>) ((List<?>) value).get(0);
    }*/

	static class LocalMessageComponent extends MessageComponent
	{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		protected int[] getGroupFields()
		{
			Iterator<Integer> it = groupKeyIterator();
			int size = 0;
			while (it.hasNext()) {
				size++;
				it.next();
			}
			int[] groupFields = new int[size];

			it = groupKeyIterator();
			int i=0;
			while (it.hasNext()) {
				groupFields[i] = it.next();
			}

			return groupFields;
		}

		@Override
		protected int[] getFields() {
			Iterator<Field<?>> it = iterator();
			int size = 0;
			while (it.hasNext()) {
				size++;
				it.next();
			}
			int[] localFields = new int[size];
			it = iterator();
			int i=0;
			while (it.hasNext()) {
				localFields[i] = it.next().getTag();
			}

			return localFields;
		}
	}

}
