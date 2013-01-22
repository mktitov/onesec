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
package org.onesec.raven.codec;

import com.ibm.media.codec.audio.AudioCodec;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public class DtmfFilter extends AudioCodec {
    private final static int NUMBER_OF_BUTTONS = 65;
    private final static int COEFF_NUMBER = 18;
    private final static short CONSTANTS[] = {27860, 26745, 25529, 24216, 19747, 16384, 12773, 8967, 21319, 
            29769, 32706, 32210, 31778, 31226, -1009, -12772, -22811, -30555};
    private final static char[][] DTMF_CODES = new char[][]{
            {'1','2','3','A'},
            {'4','5','6','B'},
            {'7','8','9','C'},
            {'*','0','#','D'}
    };
    
    private final int SAMPLES = 102;
    private final int coeffs[] = new int[COEFF_NUMBER];
    private final int powerThreshold = 328;
    private final int dialTonesToOhersTones = 16;
    private final int dialTonesToOhersDialTones = 6;    
    private final int frameSize;
    
    private char pDialButtons[] = new char[NUMBER_OF_BUTTONS];    
    private short indexForDialButtons = 0;    
    private short pArraySamples[];
    private short  internalArray[];
    private int frame_count;
    private char prevDialButton;
    private boolean permissionFlag;

    private final DtmfListener listener;

    public DtmfFilter(DtmfListener listener) {
        this.frameSize = 160;
        this.listener = listener;
        this.supportedInputFormats = new AudioFormat[]{
            new AudioFormat(
                AudioFormat.LINEAR,
                8000,
                16,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            ),
        };
        this.supportedOutputFormats = new AudioFormat[]{
            new AudioFormat(
                AudioFormat.LINEAR,
                8000,
                16,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            ),
        };
        
    }

    @Override
    public int process(Buffer paramBuffer1, Buffer paramBuffer2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static short norm_l(int L_var1) {
        if (L_var1 == 0)
            return 0;
        else if (L_var1 == (int)0xffffffff)
            return 31;
        if (L_var1 < 0)
            L_var1 = ~L_var1;
        short var_out = 0;
        for(; L_var1 < (int)0x40000000; var_out++)
            L_var1 <<= 1;
        return var_out;
    }
    
    private char dtmfDetection(final short short_array_samples[]) {
        if (!checkPower(short_array_samples))
            return ' ';
        fillInternalArray(short_array_samples);
        filter();
        int row = getMinIndex(coeffs, 0, 4);
        int column = getMinIndex(coeffs, 4, 8);
        return checkAndAdjustCoeffs(row, column)? DTMF_CODES[row][column-4] : ' ';
    }
    
    private boolean checkAndAdjustCoeffs(int row, int column) {
        int sum = 0;
        for (int i = 0; i < 10; i++) 
            sum += coeffs[i];
        sum -= coeffs[row]+coeffs[column];
        sum >>= 3;
        if (sum == 0) 
            sum = 1;

        if (   coeffs[row] / sum < dialTonesToOhersDialTones 
            || coeffs[column] / sum < dialTonesToOhersDialTones
            || coeffs[row] < (coeffs[column] >> 2)
            || coeffs[column] < ((coeffs[row] >> 1) - (coeffs[row] >> 3))) 
        {
            return false;
        }

        for (int i = 0; i < COEFF_NUMBER; i++) 
            if (coeffs[i] == 0) 
                coeffs[i] = 1;

        for (int i = 10; i < COEFF_NUMBER; i++) 
            if (coeffs[row]/coeffs[i] < dialTonesToOhersTones || coeffs[column]/coeffs[i] < dialTonesToOhersTones) 
                return false;

        for (int i = 0; i < 10; i++) 
            if (coeffs[i] != coeffs[column] && coeffs[i] != coeffs[row]) {
                if (coeffs[row] / coeffs[i] < dialTonesToOhersDialTones) 
                    return false;
                if (column != 4) {
                    if (coeffs[column] / coeffs[i] < dialTonesToOhersDialTones) 
                        return false;
                } else if (coeffs[column] / coeffs[i] < (dialTonesToOhersDialTones / 3)) 
                    return false;
            }
        return true;
    }
    
    private void fillInternalArray(final short[] samples) {
        short normVal;
        int dial = 32;
        for (int i = 0; i < SAMPLES; i++) {
            coeffs[0] = samples[i];
            if (coeffs[0]!=0 && dial > (normVal=norm_l(coeffs[0]))) 
                dial = normVal;
        }
        dial -= 16;
        for (int i = 0; i < SAMPLES; i++) {
            coeffs[0] = samples[i];
            internalArray[i] = (short) (coeffs[0] << dial);
        }
    }
    
    private boolean checkPower(final short[] samples) {
        int sum = 0;
        for (int i = 0; i < SAMPLES; i++) 
            sum += Math.abs(samples[i]);
        sum /= SAMPLES;
        if (sum < powerThreshold) 
            return false;
        return true;
    }
    
    private static int getMinIndex(int[] arr, int from, int to) {
        int ind = 0; int temp = arr[from];
        for (int i = from+1; i < to; i++) 
            if (temp < arr[i]) {
                ind = i;
                temp = arr[i];
            }
        return ind;
    }
    
    private static int mpy48sr(short o16, int o32) {  
        int t1 = (((char)o32 * o16) + 0x4000) >> 15;
        int t2 = (short)(o32 >> 16) * o16;
        return (int)((t2 << 1) + t1);
    }

    private static void goertzelFilter(short k0, short k1, short samples[], int magn[], int cnt, int index) {
        int t0, t1;
        int vk1_0 = 0, vk2_0 = 0, vk1_1 = 0, vk2_1 = 0;

        for (int i = 0; i < cnt; ++i) {
            t0 = mpy48sr(k0, vk1_0 << 1) - vk2_0 + samples[i];
            t1 = mpy48sr(k1, vk1_1 << 1) - vk2_1 + samples[i];
            vk2_0 = vk1_0;
            vk2_1 = vk1_1;
            vk1_0 = t0;
            vk1_1 = t1;
        }
        vk1_0 >>= 10; vk1_1 >>= 10; vk2_0 >>= 10; vk2_1 >>= 10;
        t0 = mpy48sr(k0, vk1_0 << 1);
        t1 = mpy48sr(k1, vk1_1 << 1);
        //TODO: delete (short)
        t0 = (short) t0 * (short) vk2_0;
        t1 = (short) t1 * (short) vk2_1;
        t0 = (short) vk1_0 * (short) vk1_0 + (short) vk2_0 * (short) vk2_0 - t0;
        t1 = (short) vk1_1 * (short) vk1_1 + (short) vk2_1 * (short) vk2_1 - t1;
        magn[index] = t0;
        magn[index + 1] = t1;
    }

    private void filter() {
        goertzelFilter(CONSTANTS[0], CONSTANTS[1], internalArray, coeffs, SAMPLES, 0);
        goertzelFilter(CONSTANTS[2], CONSTANTS[3], internalArray, coeffs, SAMPLES, 2);
        goertzelFilter(CONSTANTS[4], CONSTANTS[5], internalArray, coeffs, SAMPLES, 4);
        goertzelFilter(CONSTANTS[6], CONSTANTS[7], internalArray, coeffs, SAMPLES, 6);
        goertzelFilter(CONSTANTS[8], CONSTANTS[9], internalArray, coeffs, SAMPLES, 8);
        goertzelFilter(CONSTANTS[10], CONSTANTS[11], internalArray, coeffs, SAMPLES, 10);
        goertzelFilter(CONSTANTS[12], CONSTANTS[13], internalArray, coeffs, SAMPLES, 12);
        goertzelFilter(CONSTANTS[14], CONSTANTS[15], internalArray, coeffs, SAMPLES, 14);
        goertzelFilter(CONSTANTS[16], CONSTANTS[17], internalArray, coeffs, SAMPLES, 16);
    }
}
