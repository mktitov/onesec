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

package org.onesec.raven.sms;

/**
 *
 * @author Mikhail Titov
 */
public class SmsUtil {
    
    private SmsUtil() { }
    
    private static final String replArTo[][] = {
        {"\\^", "\u001b\u0014"},
        {"\\{", "\u001b\u0028"},
        {"\\}", "\u001b\u0029"},
        {"\\\\", "\u001b\u002f"},
        {"\\[", "\u001b\u003c"},
        {"~", "\u001b\u003d"},
        {"\\]", "\u001b\u003e"},
        {"\\|", "\u001b\u0040"}
// ,"\u00a4","\u001b\u0065"
    };
    
    private static final String replArFrom[][] = {
        {"\u001b\u0014", "^"},
        {"\u001b\\\u0028", "{"},
        {"\u001b\\\u0029", "}"},
        {"\u001b\u002f", "\\"},
        {"\u001b\u003c", "["},
        {"\u001b\u003d", "~"},
        {"\u001b\u003e", "]"},
        {"\u001b\u0040", "|"}};
    
    public static String toFromGSM_0338(String in, String[][] mx) throws Exception{
        String out = in;
        for (int i = 0; i < mx.length; i++) {
            String[] ra = mx[i];
            out = out.replaceAll(ra[0], ra[1]);
        }
        return out;
    }

    public static String fromGSM_0338(String in) throws Exception {
        return toFromGSM_0338(in, replArFrom);
    }

    public static String toGSM_0338(String in) throws Exception {
        return toFromGSM_0338(in, replArTo);
    }
}
