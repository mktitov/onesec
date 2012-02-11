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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.InputStreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class ContainerParserDataSourceTest {
    
    private CodecManager codecManager;
    private static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    
    @Before
    public void prepare() throws IOException {
        codecManager = new CodecManagerImpl(logger);
    }
    
    @Test
    public void test() throws Exception {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        parser.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(parser, "target/parsed_file.wav");
        TimeUnit.SECONDS.sleep(3);
        controller.stop();
        TimeUnit.SECONDS.sleep(3);
    }
}
