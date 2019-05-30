/*
 * Copyright (C) 1999 - 2004 by Matthias Pfisterer
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
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import com.github.trilarion.sound.sampled.AudioFormats;

/**
 * This is a base class for FormatConversionProviders that can convert from each
 * source encoding/format to each target encoding/format. If this is not the
 * case, use TEncodingFormatConversionProvider.
 *
 * <p>
 * Overriding classes must provide a constructor that calls the protected
 * constructor of this class and override
 * <code>AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream)</code>.
 * The latter method should be able to handle the case that all fields are
 * NOT_SPECIFIED and provide appropriate default values.
 *
 * @author Matthias Pfisterer
 */
// todo:
// - declare a constant ALL_BUT_SAME_VALUE (==-2) or so that can be used in format lists
// - consistent implementation of replacing NOT_SPECIFIED when not given in conversion
public abstract class SimpleFormatConversionProvider extends TFormatConversionProvider {

    private static final Logger LOG = Logger.getLogger(SimpleFormatConversionProvider.class.getName());

    private static void collectEncodings(Collection<AudioFormat> formats, Collection<AudioFormat.Encoding> encodings) {
        for (AudioFormat format : formats) {
            encodings.add(format.getEncoding());
        }
    }

    private final Collection<AudioFormat.Encoding> m_sourceEncodings;
    private final Collection<AudioFormat.Encoding> m_targetEncodings;
    private final Collection<AudioFormat> m_sourceFormats;
    private final Collection<AudioFormat> m_targetFormats;

    /**
     *
     * @param sourceFormats
     * @param targetFormats
     */
    protected SimpleFormatConversionProvider(
            Collection<AudioFormat> sourceFormats,
            Collection<AudioFormat> targetFormats) {
        m_sourceEncodings = new ArrayList<>();
        m_targetEncodings = new ArrayList<>();
        if (sourceFormats == null) {
            sourceFormats = new ArrayList<>();
        }
        if (targetFormats == null) {
            targetFormats = new ArrayList<>();
        }
        m_sourceFormats = sourceFormats;
        m_targetFormats = targetFormats;
        collectEncodings(m_sourceFormats, m_sourceEncodings);
        collectEncodings(m_targetFormats, m_targetEncodings);
    }

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return m_sourceEncodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return m_targetEncodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    // overwritten of FormatConversionProvider
    @Override
    public boolean isSourceEncodingSupported(AudioFormat.Encoding sourceEncoding) {
        return m_sourceEncodings.contains(sourceEncoding);
    }

    // overwritten of FormatConversionProvider
    @Override
    public boolean isTargetEncodingSupported(AudioFormat.Encoding targetEncoding) {
        return m_targetEncodings.contains(targetEncoding);
    }

    /**
     * This implementation assumes that the converter can convert from each of
     * its source encodings to each of its target encodings. If this is not the
     * case, the converter has to override this method.
     *
     * @param sourceFormat
     * @return
     */
    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (isAllowedSourceFormat(sourceFormat)) {
            return getTargetEncodings();
        } else {
            return EMPTY_ENCODING_ARRAY;
        }
    }

    /**
     * This implementation assumes that the converter can convert from each of
     * its source formats to each of its target formats. If this is not the
     * case, the converter has to override this method.
     *
     * @param targetEncoding
     * @param sourceFormat
     * @return
     */
    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (isConversionSupported(targetEncoding, sourceFormat)) {
            return m_targetFormats.toArray(EMPTY_FORMAT_ARRAY);
        } else {
            return EMPTY_FORMAT_ARRAY;
        }
    }

    /**
     *
     * @param sourceFormat
     * @return
     */
    protected boolean isAllowedSourceFormat(AudioFormat sourceFormat) {
        for (AudioFormat format : m_sourceFormats) {
            if (AudioFormats.matches(format, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

}
