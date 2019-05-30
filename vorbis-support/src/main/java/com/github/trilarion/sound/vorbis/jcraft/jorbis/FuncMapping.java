/*
 * Copyright (C) 2000 ymnk<ymnk@jcraft.com>
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
package com.github.trilarion.sound.vorbis.jcraft.jorbis;

import java.util.logging.Logger;
import com.github.trilarion.sound.vorbis.jcraft.jogg.Buffer;

/**
 *
 */
abstract class FuncMapping {

    private static final Logger LOG = Logger.getLogger(FuncMapping.class.getName());

    public static FuncMapping[] mapping_P = {new Mapping0()};

    abstract void pack(Info info, Object imap, Buffer buffer);

    abstract Object unpack(Info info, Buffer buffer);

    abstract Object look(DspState vd, InfoMode vm, Object m);

    abstract void free_info(Object imap);

    abstract void free_look(Object imap);

    abstract int inverse(Block vd, Object lm);
}
