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
package com.exactpro.sf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Util {
	
	public static String readStream(InputStream s) throws IOException {
		final int n = 0x100;
		String o = "";
		Reader r = new InputStreamReader(s);
		char[] buf = new char[n];
		CharBuffer cb = CharBuffer.wrap(buf);
		int c = 0;
		while ((c = r.read(buf)) >= 0) {
			cb.rewind();
			o += cb.toString().substring(0, c);
		}
		
		return o;
	}
	
	public static String getTextContent(Element el, String child) {
		NodeList list = el.getElementsByTagName(child);
		return list.getLength() > 0 ? 
				list.item(0).getTextContent() : 
				null;
	}
	
	
	
	/**
	 * Packs given set of services into ZIP and writes it to given stream.
	 * This method <b>will close</b> the output stream.
	 * @param out Output stream
	 * @param builder Document builder
	 * @param transformer Transformer
	 * @param svcs Set of services to zip
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void zipServices(OutputStream out, DocumentBuilder builder, Transformer transformer, Service... svcs) throws IOException, TransformerException {
		ZipOutputStream zip = new ZipOutputStream(out);
		for (Service s : svcs) {
			ZipEntry en = new ZipEntry(s.getName() + ".xml");
			zip.putNextEntry(en);
			s.write(zip, builder, transformer);
			zip.closeEntry();
		}
		zip.close();
	}
	
}
