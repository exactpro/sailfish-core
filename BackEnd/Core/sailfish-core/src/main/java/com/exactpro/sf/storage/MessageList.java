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
package com.exactpro.sf.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.impl.FileMessage;
import com.exactpro.sf.storage.impl.JSONSerializer;

public class MessageList extends FileBackedList<FileMessage> {
    public MessageList(String path, IWorkspaceDispatcher dispatcher) {
        super(path, new FileMessageSerializer(), dispatcher);
    }

    @Override
    public void clear() {
        size = 0;

        try {
            FileUtils.cleanDirectory(path);
        } catch(IOException e) {
            throw new EPSCommonException("Failed to clean directory", e);
        }
    }

    @Override
    protected void changeSize(int diff) {
        size += diff;
    }

    private static class FileMessageSerializer implements ISerializer<FileMessage> {
        private final ISerializer<FileMessage> fileSerializer;

        public FileMessageSerializer() {
            this.fileSerializer = JSONSerializer.of(FileMessage.class);
        }

        @Override
        public void serialize(FileMessage object, OutputStream output) throws Exception {
            throw new UnsupportedOperationException("Need file to update the last-modified time of it");
        }

        @Override
        public void serialize(FileMessage object, File output) throws Exception {
            this.fileSerializer.serialize(object, output);
            output.setLastModified(object.getTimestamp().getTime());
        }

        @Override
        public FileMessage deserialize(InputStream input) throws Exception {
            throw new UnsupportedOperationException("Need file to execute lazy loading");
        }

        @Override
        public FileMessage deserialize(File input) throws Exception {
            return new FileMessage(this.fileSerializer, input);
        }
    }
}
