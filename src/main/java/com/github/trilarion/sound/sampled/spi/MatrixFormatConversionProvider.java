/*
 * Copyright (C) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@gmx.de>
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
package com.github.trilarion.sound.sampled.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import com.github.trilarion.sound.sampled.AudioFormats;

/**
 * Base class for arbitrary formatConversionProviders.
 *
 * @author Matthias Pfisterer
 */
public abstract class MatrixFormatConversionProvider
        extends SimpleFormatConversionProvider {

    private static final Logger LOG = Logger.getLogger(MatrixFormatConversionProvider.class.getName());

    /*
     *	keys: source AudioFormat
     *	values: collection of possible target encodings
     *
     *	Note that accessing values with get() is not appropriate,
     *	since the equals() method in AudioFormat is not overloaded.
     *	The hashtable is just used as a convenient storage
     *	organization.
     */
    private final Map<AudioFormat, List<AudioFormat.Encoding>> m_targetEncodingsFromSourceFormat;

    /*
     *	keys: source AudioFormat
     *	values: a Map that contains a mapping from target encodings
     *	(keys) to a collection of target formats (values).
     *
     *	Note that accessing values with get() is not appropriate,
     *	since the equals() method in AudioFormat is not overloaded.
     *	The hashtable is just used as a convenient storage
     *	organization.
     */
    private final Map<AudioFormat, Map<AudioFormat.Encoding, Collection<AudioFormat>>> m_targetFormatsFromSourceFormat;

    /**
     *
     * @param sourceFormats
     * @param targetFormats
     * @param abConversionPossible
     */
    protected MatrixFormatConversionProvider(
            List<AudioFormat> sourceFormats,
            List<AudioFormat> targetFormats,
            boolean[][] abConversionPossible) {
        super(sourceFormats,
                targetFormats);
        m_targetEncodingsFromSourceFormat = new HashMap<>();
        m_targetFormatsFromSourceFormat = new HashMap<>();

        for (int nSourceFormat = 0;
                nSourceFormat < sourceFormats.size();
                nSourceFormat++) {
            AudioFormat sourceFormat = sourceFormats.get(nSourceFormat);
            List<AudioFormat.Encoding> supportedTargetEncodings = new ArrayList<>();
            m_targetEncodingsFromSourceFormat.put(sourceFormat, supportedTargetEncodings);
            Map<AudioFormat.Encoding, Collection<AudioFormat>> targetFormatsFromTargetEncodings = new HashMap<>();

            m_targetFormatsFromSourceFormat.put(sourceFormat, targetFormatsFromTargetEncodings);
            for (int nTargetFormat = 0;
                    nTargetFormat < targetFormats.size();
                    nTargetFormat++) {
                AudioFormat targetFormat = targetFormats.get(nTargetFormat);
                if (abConversionPossible[nSourceFormat][nTargetFormat]) {
                    AudioFormat.Encoding targetEncoding = targetFormat.getEncoding();
                    supportedTargetEncodings.add(targetEncoding);
                    Collection<AudioFormat> supportedTargetFormats = targetFormatsFromTargetEncodings.get(targetEncoding);
                    if (supportedTargetFormats == null) {
                        supportedTargetFormats = new ArrayList<>();
                        targetFormatsFromTargetEncodings.put(targetEncoding, supportedTargetFormats);
                    }
                    supportedTargetFormats.add(targetFormat);
                }
            }
        }
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        for (Entry<AudioFormat, List<AudioFormat.Encoding>> entry : m_targetEncodingsFromSourceFormat.entrySet()) {
            AudioFormat format = entry.getKey();
            if (AudioFormats.matches(format, sourceFormat)) {
                List<AudioFormat.Encoding> targetEncodings = entry.getValue();
                return targetEncodings.toArray(EMPTY_ENCODING_ARRAY);
            }
        }
        return EMPTY_ENCODING_ARRAY;
    }

    // TODO: this should work on the array returned by getTargetEncodings(AudioFormat)
/*
     public boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat)
     {
     return isAllowedSourceFormat(sourceFormat) &&
     isTargetEncodingSupported(targetEncoding);
     }
     */
    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        for (Entry<AudioFormat, Map<AudioFormat.Encoding, Collection<AudioFormat>>> entry : m_targetFormatsFromSourceFormat.entrySet()) {
            AudioFormat format = entry.getKey();
            if (AudioFormats.matches(format, sourceFormat)) {
                Map<AudioFormat.Encoding, Collection<AudioFormat>> targetEncodings = entry.getValue();
                Collection<AudioFormat> targetFormats = targetEncodings.get(targetEncoding);
                if (targetFormats != null) {
                    return targetFormats.toArray(EMPTY_FORMAT_ARRAY);
                }
                return EMPTY_FORMAT_ARRAY;
            }
        }
        return EMPTY_FORMAT_ARRAY;
    }

}
