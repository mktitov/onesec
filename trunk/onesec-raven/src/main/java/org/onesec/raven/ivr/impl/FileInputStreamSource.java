/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class FileInputStreamSource implements InputStreamSource {
    
    private final File file;
    private final Node owner;

    public FileInputStreamSource(File file, Node owner) {
        this.file = file;
        this.owner = owner;
    }

    public InputStream getInputStream() {
        try {
            return FileUtils.openInputStream(file);
        } catch (IOException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
