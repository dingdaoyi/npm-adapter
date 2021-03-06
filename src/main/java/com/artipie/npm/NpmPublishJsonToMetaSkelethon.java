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

import com.artipie.npm.misc.DateTimeNowStr;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Bind {@code npm publish} generated json to an instance on {@link Meta}.
 *
 * @since 0.1
 */
final class NpmPublishJsonToMetaSkelethon {

    /**
     * The name json filed.
     */
    private static final String NAME = "name";

    /**
     * {@code npm publish} generated json to bind.
     */
    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param json The json to bind.
     */
    NpmPublishJsonToMetaSkelethon(final JsonObject json) {
        this.json = json;
    }

    /**
     * Bind the npm.
     * @return The skeleton for meta.json file
     */
    public JsonObject skeleton() {
        final String now = new DateTimeNowStr().value();
        final JsonObjectBuilder builder = Json.createObjectBuilder()
            .add(
                NpmPublishJsonToMetaSkelethon.NAME,
                this.json.getString(NpmPublishJsonToMetaSkelethon.NAME)
            )
            .add(
                "time",
                Json.createObjectBuilder()
                    .add("created", now)
                    .add("modified", now)
                    .build()
            )
            .add("users", Json.createObjectBuilder().build())
            .add("versions", Json.createObjectBuilder().build());
        this.addIfContains("_id", builder);
        this.addIfContains("readme", builder);
        return builder.build();
    }

    /**
     * Add key to builder if json contains this key.
     * @param key Key to add
     * @param builder Json builder
     */
    private void addIfContains(final String key, final JsonObjectBuilder builder) {
        if (this.json.containsKey(key)) {
            builder.add(key, this.json.getString(key));
        }
    }
}
