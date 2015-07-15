/*
 * Copyright (C) 2004 - 2008 JavaZOOM : vorbisspi@javazoom.net
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
package com.github.trilarion.sound.vorbis.sampled;

import java.util.Map;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;

/**
 * @author JavaZOOM
 */
public class VorbisAudioFileFormat extends AudioFileFormat {

    private static final Logger LOG = Logger.getLogger(VorbisAudioFileFormat.class.getName());

    /**
     * Contructor.
     *
     * @param type
     * @param audioFormat
     * @param nLengthInFrames
     * @param nLengthInBytes
     * @param properties
     */
    public VorbisAudioFileFormat(Type type, AudioFormat audioFormat, int nLengthInFrames, int nLengthInBytes, Map<String, Object> properties) {
        super(type, nLengthInBytes, audioFormat, nLengthInFrames); // TODO set properties too
    }
}
