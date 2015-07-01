/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven;

import java.io.InputStream;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.InputStreamSource;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class AudioFileToInputSourceConverter extends AbstractConverter<AudioFile, InputStreamSource> {

    public InputStreamSource convert(final AudioFile value, Class realTargetType, String format) {
        return new InputStreamSource() {
            public InputStream getInputStream() {
                try {
                    return value.getAudioFile().getDataStream();
                } catch (Exception ex) {
                    throw new TypeConverterException(ex);
                }
            }
        };
    }

    public Class getSourceType() {
        return AudioFile.class;
    }

    public Class getTargetType() {
        return InputStreamSource.class;
    }
}
