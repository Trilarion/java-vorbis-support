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
class Time0 extends FuncTime {

    private static final Logger LOG = Logger.getLogger(Time0.class.getName());

    @Override
    void pack(Object i, Buffer opb) {
    }

    @Override
    Object unpack(Info vi, Buffer opb) {
        return "";
    }

    @Override
    Object look(DspState vd, InfoMode mi, Object i) {
        return "";
    }

    @Override
    void free_info(Object i) {
    }

    @Override
    void free_look(Object i) {
    }

    @Override
    int inverse(Block vb, Object i, float[] in, float[] out) {
        return 0;
    }
}
