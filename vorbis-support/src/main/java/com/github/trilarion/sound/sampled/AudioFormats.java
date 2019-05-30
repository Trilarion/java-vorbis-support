/*
 * Copyright (C) 1999,2000 by Matthias Pfisterer
 *               1999 by Florian Bomers <http://www.bomers.de>
 *               2015 Trilarion
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.trilarion.sound.sampled;

import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/**
 *
 */
public final class AudioFormats {

    private static final Logger LOG = Logger.getLogger(AudioFormats.class.getName());

    private static boolean doMatch(int i1, int i2) {
        return i1 == AudioSystem.NOT_SPECIFIED
                || i2 == AudioSystem.NOT_SPECIFIED
                || i1 == i2;
    }

    private static boolean doMatch(float f1, float f2) {
        return f1 == AudioSystem.NOT_SPECIFIED
                || f2 == AudioSystem.NOT_SPECIFIED
                || Math.abs(f1 - f2) < 1.0e-9;
    }

    /**
     * Tests whether 2 AudioFormats have matching formats. A field matches when
     * it is AudioSystem.NOT_SPECIFIED in at least one of the formats or the
     * field is the same in both formats.<br>
     * Exceptions:
     * <ul>
     * <li>Encoding must always be equal for a match.
     * <li> For a match, endianness must be equal if SampleSizeInBits is not
     * AudioSystem.NOT_SPECIFIED and greater than 8bit in both formats.<br>
     * In other words: If SampleSizeInBits is AudioSystem.NOT_SPECIFIED in
     * either format or both formats have a SampleSizeInBits smaller than 8, endianness does
     * not matter. </ul> This is a proposition to be used as
     * AudioFormat.matches. It can therefore be considered as a temporary
     * workaround.
     *
     * @param format1
     * @param format2
     * @return
     */
    // IDEA: create a special "NOT_SPECIFIED" encoding
    // and a AudioFormat.Encoding.matches method.
    public static boolean matches(AudioFormat format1,
                                  AudioFormat format2) {
        //$$fb 19 Dec 99: endian must be checked, too.
        //
        // we do have a problem with redundant elements:
        // e.g.
        // encoding=ALAW || ULAW -> bigEndian and samplesizeinbits don't matter
        // sample size in bits == 8 -> bigEndian doesn't matter
        // sample size in bits > 8 -> PCM is always signed.
        // This is an overall issue in JavaSound, I think.
        // At present, it is not consistently implemented to support these
        // redundancies and implicit definitions
        //
        // As a workaround of this issue I return in the converters
        // all combinations, e.g. for ULAW I return bigEndian and !bigEndian formats.
        /* old version
         */
        // as proposed by florian
        return format1.getEncoding().equals(format2.getEncoding())
                && (format2.getSampleSizeInBits() <= 8
                || format1.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED
                || format2.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED
                || format1.isBigEndian() == format2.isBigEndian())
                && doMatch(format1.getChannels(), format2.getChannels())
                && doMatch(format1.getSampleSizeInBits(), format2.getSampleSizeInBits())
                && doMatch(format1.getFrameSize(), format2.getFrameSize())
                && doMatch(format1.getSampleRate(), format2.getSampleRate())
                && doMatch(format1.getFrameRate(), format2.getFrameRate());
    }
}
