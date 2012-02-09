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

import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import org.onesec.raven.ivr.CodecConfig;

/**
 *
 * @author Mikhail Titov
 */
public class CodecConfigImpl implements CodecConfig {
    private final Codec codec;
    private final Format outputFormat;
    private final Format inputFormat;

    public CodecConfigImpl(Codec codec, Format outputFormat, Format inputFormat) 
            throws ResourceUnavailableException 
    {
        this.codec = codec;
        this.codec.setInputFormat(inputFormat);
        this.codec.setOutputFormat(outputFormat);
        this.codec.open();
        this.outputFormat = outputFormat;
        this.inputFormat = inputFormat;
    }

    public Format getInputFormat() {
        return inputFormat;
    }

    public Codec getCodec() {
        return codec;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }
}
