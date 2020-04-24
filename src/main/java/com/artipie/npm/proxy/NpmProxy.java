/*
 * MIT License
 *
 * Copyright (c) 2020 artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Maybe;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Paths;

/**
 * NPM Proxy.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
 */
public class NpmProxy {
    /**
     * The Vertx instance.
     */
    private final Vertx vertx;

    /**
     * The storage.
     */
    private final NpmProxyStorage storage;

    /**
     * Remote repository client.
     */
    private final NpmRemote remote;

    /**
     * Ctor.
     * @param config NPM Proxy configuration
     * @param vertx Vertx instance
     * @param storage Adapter storage
     */
    public NpmProxy(final NpmProxyConfig config, final Vertx vertx, final Storage storage) {
        this(
            vertx,
            new NpmProxyStorageImpl(new RxStorageWrapper(storage)),
            new NpmRemoteImpl(config, vertx)
        );
    }

    /**
     * Default-scoped ctor (for tests).
     * @param vertx Vertx instance
     * @param storage NPM storage
     * @param remote Remote repository client
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    NpmProxy(final Vertx vertx,
        final NpmProxyStorage storage,
        final NpmRemote remote) {
        this.vertx = vertx;
        this.storage = storage;
        this.remote = remote;
    }

    /**
     * Retrieve package metadata.
     * @param name Package name
     * @return Package metadata (cached or downloaded from remote repository)
     */
    public Maybe<NpmPackage> getPackage(final String name) {
        return this.remote.loadPackage(name).flatMap(
            pkg -> this.storage.save(pkg).andThen(Maybe.just(pkg))
        ).switchIfEmpty(Maybe.defer(() -> this.storage.getPackage(name)));
    }

    /**
     * Retrieve asset.
     * @param path Asset path
     * @return Asset data (cached or downloaded from remote repository)
     */
    public Maybe<NpmAsset> getAsset(final String path) {
        return this.storage.getAsset(path).switchIfEmpty(
            Maybe.defer(
                () -> this.vertx.fileSystem().rxCreateTempFile("npm-asset-", ".tmp")
                    .flatMapMaybe(
                        tmp -> this.remote.loadAsset(path, Paths.get(tmp)).flatMap(
                            asset -> this.storage.save(asset)
                                .andThen(Maybe.defer(() -> this.storage.getAsset(path)))
                                .doOnTerminate(() -> this.vertx.fileSystem().rxDelete(tmp))
                        )
                    )
            )
        );
    }

    /**
     * Close NPM Proxy adapter and underlying remote client.
     */
    public void close() {
        this.remote.close();
    }

}
