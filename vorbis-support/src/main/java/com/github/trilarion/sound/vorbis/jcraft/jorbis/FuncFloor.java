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
abstract class FuncFloor {

    private static final Logger LOG = Logger.getLogger(FuncFloor.class.getName());

    public static FuncFloor[] floor_P = {new Floor0(), new Floor1()};

    abstract void pack(Object i, Buffer opb);

    abstract Object unpack(Info vi, Buffer opb);

    abstract Object look(DspState vd, InfoMode mi, Object i);

    abstract void free_info(Object i);

    abstract void free_look(Object i);

    abstract void free_state(Object vs);

    abstract int forward(Block vb, Object i, float[] in, float[] out, Object vs);

    abstract Object inverse1(Block vb, Object i, Object memo);

    abstract int inverse2(Block vb, Object i, Object memo, float[] out);
}
