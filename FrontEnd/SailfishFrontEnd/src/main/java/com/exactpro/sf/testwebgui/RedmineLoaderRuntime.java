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
package com.exactpro.sf.testwebgui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.taskadapter.redmineapi.bean.Attachment;
import com.taskadapter.redmineapi.bean.AttachmentFactory;
import com.taskadapter.redmineapi.bean.User;
import com.taskadapter.redmineapi.bean.UserFactory;
import com.taskadapter.redmineapi.bean.WikiPageDetail;

public class RedmineLoaderRuntime {

	private static final Logger logger = LoggerFactory.getLogger(RedmineLoader.class);

	private static final String XML_FILE_NAME = "RedminePages.xml";

	// Runtime cache
	private static List<WikiPageDetail> wikiPages;

	private static void loadWikiPagesFromXML() throws IOException {
		List<WikiPageDetail> result = null;
		FileInputStream fis = null;
		try {
			File file = SFLocalContext.getDefault().getWorkspaceDispatcher().getFile(FolderType.ROOT, "help", XML_FILE_NAME);
			fis = new FileInputStream(file);
			result = readXml(fis);
		} catch (Exception e) {
			logger.error("Error while parsing wiki pages from xml. {}", e.getMessage());
			e.printStackTrace();
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		wikiPages = result;
	}

    private static List<WikiPageDetail> readXml(FileInputStream fis) throws IOException, SAXException, ParserConfigurationException, ParseException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fis);

        List<WikiPageDetail> result = new ArrayList<WikiPageDetail>();

        NodeList nodeList = doc.getElementsByTagName("com.taskadapter.redmineapi.bean.WikiPageDetail");
        for(int i=0;i<nodeList.getLength();i++) {
            Node pageNode = nodeList.item(i);

            WikiPageDetail wikiPageDetail = new WikiPageDetail();

            for(int j=0;j<pageNode.getChildNodes().getLength();j++) {
                Node paramNode = pageNode.getChildNodes().item(j);
                if(paramNode.getNodeName().equals("#text")) {
                    continue;
                }
                setPageParam(paramNode, wikiPageDetail);
            }
            result.add(wikiPageDetail);
        }
        return result;
    }

    private static void setPageParam(Node paramNode, WikiPageDetail wikiPageDetail) throws ParseException {
        if(paramNode.getNodeName().equals("title")) {
            wikiPageDetail.setTitle(paramNode.getTextContent());
        } else if(paramNode.getNodeName().equals("text")) {
            wikiPageDetail.setText(paramNode.getTextContent());
        } else if(paramNode.getNodeName().equals("version")) {
            wikiPageDetail.setVersion(Integer.valueOf(paramNode.getTextContent()));
        } else if(paramNode.getNodeName().equals("updatedOn")) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            Date d = format.parse(paramNode.getTextContent());
            wikiPageDetail.setUpdatedOn(d);
        } else if(paramNode.getNodeName().equals("createdOn")) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            Date d = format.parse(paramNode.getTextContent());
            wikiPageDetail.setCreatedOn(d);
        } else if(paramNode.getNodeName().equals("user")) {
            User user = parseUser(paramNode.getChildNodes());
            wikiPageDetail.setUser(user);
        } else if(paramNode.getNodeName().equals("attachments")) {
            List<Attachment> attachments = parseAttachments(paramNode.getChildNodes());
            wikiPageDetail.setAttachments(attachments);
        }
    }

    private static List<Attachment> parseAttachments(NodeList childNodes) throws ParseException {
        List<Attachment> attachments = new ArrayList<Attachment>();

        for(int i=0;i<childNodes.getLength();i++) {
            if(childNodes.item(i).getNodeName().equals("#text")){
                continue;
            }

            Node attachNode = childNodes.item(i);
            Attachment attach = null;
            for(int j=0;j<attachNode.getChildNodes().getLength();j++) {
                Node paramNode = attachNode.getChildNodes().item(j);
                if(paramNode.getNodeName().equals("id")) {
                    attach = AttachmentFactory.create(Integer.valueOf(paramNode.getTextContent()));
                    break;
                }
            }

            if(attach == null) {
                attach = AttachmentFactory.create();
            }

            for(int j=0;j<attachNode.getChildNodes().getLength();j++) {
                Node paramNode = attachNode.getChildNodes().item(j);
                String paramName = paramNode.getNodeName();
                if(paramName.equals("fileName")) {
                    attach.setFileName(paramNode.getTextContent());
                } else if(paramName.equals("fileSize")) {
                    attach.setFileSize(Long.valueOf(paramNode.getTextContent()));
                } else if(paramName.equals("contentType")) {
                    attach.setContentType(paramNode.getTextContent());
                } else if(paramName.equals("contentURL")) {
                    attach.setContentURL(paramNode.getTextContent());
                } else if(paramName.equals("description")) {
                    attach.setDescription(paramNode.getTextContent());
                } else if(paramName.equals("createdOn")) {
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                    Date d = format.parse(paramNode.getTextContent());
                    attach.setCreatedOn(d);
                } else if(paramName.equals("author")) {
                    User author = parseUser(paramNode.getChildNodes());
                    attach.setAuthor(author);
                }
            }
            attachments.add(attach);
        }
        return attachments;
    }

    private static User parseUser(NodeList userParams) {
        User user = null;

        for(int i=0;i<userParams.getLength();i++) {
            Node paramNode = userParams.item(i);
            String paramName = paramNode.getNodeName();
            if(paramName.equals("id")) {
                user = UserFactory.create(Integer.valueOf(paramNode.getTextContent()));
                break;
            }
        }

        if(user == null) {
            user = UserFactory.create();
        }

        for(int i=0;i<userParams.getLength();i++) {
            Node paramNode = userParams.item(i);
            String paramName = paramNode.getNodeName();
            if(paramName.equals("firstName")) {
                user.setFirstName(paramNode.getTextContent());
            } else if(paramName.equals("lastName")) {
                user.setLastName(paramNode.getTextContent());
            }
        }
        return user;
    }

    public static WikiPageDetail getWikiPage(String title) {

		synchronized (RedmineLoaderRuntime.class) {
			if (wikiPages == null) {
				try {
					loadWikiPagesFromXML();
				} catch (IOException e) {
					logger.error("Error getting wiki pages list from xml. {}", e.getMessage());
					e.printStackTrace();
				}

				if (wikiPages == null) {
					logger.error("WikiPages is not defined.");
					return new WikiPageDetail();
				}
			}
		}

        if(title.trim().length() == 0) {
            title = getRootPage();
        }

        for(WikiPageDetail pageDetail : wikiPages) {
            if(isPagesNamesEquals(pageDetail.getTitle(), title)) {
                return pageDetail;
            }
        }

        logger.warn("Can not find wiki page with title {}", title);
        return new WikiPageDetail();
    }

    private static boolean isPagesNamesEquals(String name1, String name2) {
        if(name1.equalsIgnoreCase(name2)) {
            return true;
        }

        name1 = name1.replaceAll("[\\./,]", "");
        name2 = name2.replaceAll("[\\./,]", "");

        if(name1.equalsIgnoreCase(name2)) {
            return true;
        }

        //fixes for "3. Индикатор номера тест-кейса ... " page
        name1 = name1.replaceAll("тест-", "");
        name2 = name2.replaceAll("тест-", "");

        return name1.equalsIgnoreCase(name2);
    }


    private static String getRootPage() {
        for(WikiPageDetail pd : wikiPages) {
            String title = pd.getTitle();
            if(title.endsWith("_root")) {
                String rootPage = title.substring(0, title.indexOf("_root"));
                pd.setTitle(rootPage);
                return rootPage;
            }
        }
        throw new IllegalStateException("Can not find redmine root page.");
    }

}
