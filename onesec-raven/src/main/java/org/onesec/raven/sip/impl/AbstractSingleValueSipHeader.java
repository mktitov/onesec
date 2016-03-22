/*
 * Copyright 2016 Mikhail Titov.
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
package org.onesec.raven.sip.impl;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSingleValueSipHeader<T> extends AbstractSipHeader<T>{
    protected final T value;

    public AbstractSingleValueSipHeader(final String name, List<String> stringValues) {
        super(name);
        this.value = createValue(stringValues);
    }
    
    protected abstract T createValue(List<String> stringValues);

    @Override
    public T getFirstValue() {
        return value;
    }

    @Override
    public List<T> getValues() {
        return Arrays.asList(value);
    }

    @Override
    public ByteBuf writeTo(ByteBuf buf) {
        buf.writeBytes(getBytesOfName()).writeByte(':').writeByte(' ');
        buf.writeBytes(getBytesOfValue(value));
        return buf;
    }
}
