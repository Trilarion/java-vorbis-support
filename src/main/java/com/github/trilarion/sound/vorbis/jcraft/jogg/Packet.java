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
package com.github.trilarion.sound.vorbis.jcraft.jogg;

import java.util.logging.Logger;

/**
 *
 */
public class Packet {

    private static final Logger LOG = Logger.getLogger(Packet.class.getName());

    /**
     *
     */
    public byte[] packet_base;

    /**
     *
     */
    public int packet;

    /**
     *
     */
    public int bytes;

    /**
     *
     */
    public int b_o_s;

    /**
     *
     */
    public int e_o_s;

    /**
     *
     */
    public long granulepos;

    /**
     * sequence number for decode; the framing knows where there's a hole in the
     * data, but we need coupling so that the codec (which is in a seperate
     * abstraction layer) also knows about the gap
     */
    public long packetno;

}
