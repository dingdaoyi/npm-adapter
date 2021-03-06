/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm;

import io.reactivex.Completable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import javax.json.Json;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * A .tgz archive.
 *
 * @since 0.1
 */
public final class TgzArchive {

    /**
     * The archive representation in a form of a base64 string.
     */
    private final String bitstring;

    private final boolean base64Encoded;

    /**
     * Ctor.
     * @param bitstring The archive.
     */
    public TgzArchive(final String bitstring) {
        this(bitstring, true);
    }

    public TgzArchive(String bitstring, boolean base64Encoded) {
        this.bitstring = bitstring;
        this.base64Encoded = base64Encoded;
    }

    /**
     * Save the archive to a file.
     *
     * @param path The path to save .tgz file at.
     * @return Completion or error signal.
     */
    public Completable saveToFile(final Path path) {
        return Completable.fromAction(
                () -> Files.write(path, this.bytes())
        );
    }

    /**
     * Obtain an archive in form of byte array.
     *
     * @return Archive bytes
     */
    public byte[] bytes() {
        return base64Encoded ? Base64.getDecoder().decode(this.bitstring) :
                this.bitstring.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Obtain file by name.
     *
     * @param name The name of a file.
     * @return The file content.
     */
    private String file(final String name) {
        try {
            final TarArchiveInputStream taris = new TarArchiveInputStream(
                    new GzipCompressorInputStream(new ByteArrayInputStream(this.bytes()))
            );
            TarArchiveEntry entry;
            while ((entry = taris.getNextTarEntry()) != null) {
                if (entry.getName().endsWith(name)) {
                    return new BufferedReader(new InputStreamReader(taris))
                            .lines()
                            .collect(Collectors.joining("\n"));
                }
            }
            throw new IllegalStateException(String.format("'%s' file wasn't found", name));
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }

    public Meta meta() {
        return new Meta(
                new NpmPublishJsonToMetaSkelethon(
                        Json.createReader(new StringReader(this.file("package.json")))
                                .readObject()).skeleton()
        );
    }
}
