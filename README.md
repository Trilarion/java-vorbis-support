# java-vorbis-support

Combination and continuation of JOrbis, JavaSPI and Tritonus-Share to provide Ogg/Vorbis playback capabilities for Java using the Sound SPI.

## Download, License, Feedback

- [Homepage](https://github.com/Trilarion/java-vorbis-support) of java-vorbis-support
- Requires Java 9 or later
- Download [vorbis-support-1.2.0.jar](https://github.com/Trilarion/java-vorbis-support/releases/download/v1.2.0/vorbis-support-1.2.0.jar) (released 4th June 2019) and/or [source code](https://github.com/Trilarion/java-vorbis-support/releases/tag/v1.2.0)
- License is [LGPLv3+](http://www.gnu.org/licenses/lgpl-3.0.txt)
- Support/Feedback: Create an [issue](https://github.com/Trilarion/java-vorbis-support/issues)

On Maven Central:
- [java-vorbis-support 1.2.0](https://search.maven.org/#artifactdetails%7Ccom.github.trilarion%7Cvorbis-support%7C1.2.0%7Cjar) licensed under LGPLv3+
- [vorbis-support 1.1.0](https://search.maven.org/#artifactdetails%7Ccom.github.trilarion%7Cvorbis-support%7C1.1.0%7Cjar) licensed under LGPLv3+
- [vorbis-support 1.0.0](https://search.maven.org/#artifactdetails%7Ccom.github.trilarion%7Cvorbis-support%7C1.0.0%7Cjar) licensed under LGPLv2+

## Introduction

Ogg/Vorbis is a widely used free audio format featuring high compression ratios and there are libraries who enable support for Ogg in Java. Among them are [JOrbis](http://www.jcraft.com/jorbis/)
by JCraft - a pure Java Ogg/Vorbis decoder and [Ogg Vorbis SPI](http://www.javazoom.net/vorbisspi/vorbisspi.html) by JavaZoom which registers JOrbis as a service for the Java Sound API. The later also relies partly on the [Tritonus share](http://www.tritonus.org/) library. All three projects are inactive for several years now.

The reference implementation for Ogg/Vorbis is [libvorbis](http://xiph.org/vorbis/) written in C.

Vorbis support is the all-in-one combination and possibly continuation of JOrbis, JavaSPI and Tritonus-Share.

Alternatives are [Paul's SoundSystem](http://www.paulscode.com/forum/index.php?topic=4.0) (full support of Java Sound, JOAL, LWJGL gaming libraries, wrappers around JOrbis, J-OGG),
[Vorbis-Java](http://downloads.xiph.org/releases/vorbis-java/) (Java encoder and decoder at xiph.org), [EasyOgg](http://www.cokeandcode.com/index.html?page=libs) (wrapper around JOrbis)
and [J-OGG](http://www.j-ogg.de/) (independent ogg/vorbis decode). These projects are mostly inactive for years now.

## Why this project?

All three libraries, Jorbis, Vorbis SPI and Tritonus Share are almost always bundled together. Together they constitute a complete plattform independent Ogg/Vorbis support for the Java Sound API. Fortunately they share the same open source license (LGPL). Combining them together makes distribution and handling easier, can reduce the size of the download and makes testing and debugging easier. Last but not least, increasing the efficiency by optimizing the code will be a bit easier.

However since these libraries already exist, we do not need to take care of backwards compatibility, since there is always the fallback to the original libraries. Therefore to be able to use newer features of the Java language, the required Java version currently is 7 or later.

## Example

This library used the Services Provider Interface to enable Ogg/Vorbis playback under the hood without changing any of the code on the client's library side. Just put this library in the classpath
and access your sound resources (SourceDataLine or Clip) as you would without Ogg/Vorbis support. See the Java Tutorial on [Playing Back Audio](http://docs.oracle.com/javase/tutorial/sound/playing.html) for more information. And here is also an example that would play an entire ogg file.

    try {
        AudioInputStream in = AudioSystem.getAudioInputStream(new File("xyz.ogg");
        if (in != null) {
            AudioFormat baseFormat = in.getFormat();

            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
            16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);

            AudioInputStream dataIn = AudioSystem.getAudioInputStream(targetFormat, in);

            byte[] buffer = new byte[4096];

            // get a line from a mixer in the system with the wanted format
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            if (line != null) {
                line.open();

                line.start();
                int nBytesRead = 0, nBytesWritten = 0;
                while (nBytesRead != -1) {
                    nBytesRead = dataIn.read(buffer, 0, buffer.length);
                    if (nBytesRead != -1) {
                        nBytesWritten = line.write(buffer, 0, nBytesRead);
                    }
                }

                line.drain();
                line.stop();
                line.close();

                dataIn.close();
            }

            in.close();
            // playback finished
        }
    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
        // failed
    }

Run an example with

    ./gradlew run :examples:run

## Alternatives

[VorbisJava](https://github.com/Gagravarr/VorbisJava) by Gagravarr.
