/*
 * Copyright (C) 2015 Trilarion
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
package com.github.trilarion.sound.util;

import java.util.logging.Logger;

/**
 *
 */
public class SoundException extends Exception {

    /**
     * Only a constructor with a string argument to force users to give
     * meaningful messages.
     *
     * @param s
     */
    public SoundException(String s) {
        super("Vorbis exception " + s);
    }

    private static final Logger LOG = Logger.getLogger(SoundException.class.getName());
}
