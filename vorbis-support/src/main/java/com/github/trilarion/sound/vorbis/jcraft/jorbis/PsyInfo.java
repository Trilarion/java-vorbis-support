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

// psychoacoustic setup
import java.util.logging.Logger;

/**
 *
 */
class PsyInfo {

    private static final Logger LOG = Logger.getLogger(PsyInfo.class.getName());

    int athp;
    int decayp;
    int smoothp;
    int noisefitp;
    int noisefit_subblock;
    float noisefit_threshdB;

    float ath_att;

    int tonemaskp;
    float[] toneatt_125Hz = new float[5];
    float[] toneatt_250Hz = new float[5];
    float[] toneatt_500Hz = new float[5];
    float[] toneatt_1000Hz = new float[5];
    float[] toneatt_2000Hz = new float[5];
    float[] toneatt_4000Hz = new float[5];
    float[] toneatt_8000Hz = new float[5];

    int peakattp;
    float[] peakatt_125Hz = new float[5];
    float[] peakatt_250Hz = new float[5];
    float[] peakatt_500Hz = new float[5];
    float[] peakatt_1000Hz = new float[5];
    float[] peakatt_2000Hz = new float[5];
    float[] peakatt_4000Hz = new float[5];
    float[] peakatt_8000Hz = new float[5];

    int noisemaskp;
    float[] noiseatt_125Hz = new float[5];
    float[] noiseatt_250Hz = new float[5];
    float[] noiseatt_500Hz = new float[5];
    float[] noiseatt_1000Hz = new float[5];
    float[] noiseatt_2000Hz = new float[5];
    float[] noiseatt_4000Hz = new float[5];
    float[] noiseatt_8000Hz = new float[5];

    float max_curve_dB;

    float attack_coeff;
    float decay_coeff;

    void free() {
    }
}
