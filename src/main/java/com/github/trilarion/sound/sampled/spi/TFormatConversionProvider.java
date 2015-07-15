/*
 * Copyright (C) 1999, 2000 by Matthias Pfisterer
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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;
import com.github.trilarion.sound.sampled.AudioFormats;

/**
 * Base class for all conversion providers of Tritonus.
 *
 * @author Matthias Pfisterer
 */
public abstract class TFormatConversionProvider
        extends FormatConversionProvider {

    private static final Logger LOG = Logger.getLogger(TFormatConversionProvider.class.getName());

    /**
     *
     */
    protected static final AudioFormat.Encoding[] EMPTY_ENCODING_ARRAY = new AudioFormat.Encoding[0];

    /**
     *
     */
    protected static final AudioFormat[] EMPTY_FORMAT_ARRAY = new AudioFormat[0];

    // $$fb2000-10-04: use AudioSystem.NOT_SPECIFIED for all fields.
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream audioInputStream) {
        AudioFormat sourceFormat = audioInputStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                targetEncoding,
                AudioSystem.NOT_SPECIFIED, // sample rate
                AudioSystem.NOT_SPECIFIED, // sample size in bits
                AudioSystem.NOT_SPECIFIED, // channels
                AudioSystem.NOT_SPECIFIED, // frame size
                AudioSystem.NOT_SPECIFIED, // frame rate
                sourceFormat.isBigEndian());  // big endian
        LOG.log(Level.FINE, "TFormatConversionProvider.getAudioInputStream(AudioFormat.Encoding, AudioInputStream):");
        LOG.log(Level.FINE, "trying to convert to {0}", targetFormat);
        return getAudioInputStream(targetFormat, audioInputStream);
    }

    /**
     * WARNING: this method uses
     * <code>getTargetFormats(AudioFormat.Encoding, AudioFormat)</code> which
     * may create infinite loops if the latter is overwritten.
     * <p>
     * This method is overwritten here to make use of
     * org.tritonus.share.sampled.AudioFormats.matches and is considered
     * temporary until AudioFormat.matches is corrected in the JavaSound API.
     *
     * @return
     */
    /* $$mp: if we decide to use getMatchingFormat(), this method should be
     implemented by simply calling getMatchingFormat() and comparing the
     result against null.
     */
    @Override
    public boolean isConversionSupported(
            AudioFormat targetFormat,
            AudioFormat sourceFormat) {
        LOG.log(Level.FINE, ">TFormatConversionProvider.isConversionSupported(AudioFormat, AudioFormat):");
        LOG.log(Level.FINE, "class: {0}", getClass().getName());
        LOG.log(Level.FINE, "checking if conversion possible");
        LOG.log(Level.FINE, "from: {0}", sourceFormat);
        LOG.log(Level.FINE, "to: {0}", targetFormat);
        AudioFormat[] aTargetFormats = getTargetFormats(targetFormat.getEncoding(), sourceFormat);
        for (AudioFormat aTargetFormat : aTargetFormats) {
            LOG.log(Level.FINE, "checking against possible target format: {0}", aTargetFormat);
            if (aTargetFormat != null && AudioFormats.matches(aTargetFormat, targetFormat)) {
                LOG.log(Level.FINE, "<result=true");
                return true;
            }
        }
        LOG.log(Level.FINE, "<result=false");
        return false;
    }
}
