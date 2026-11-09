package com.researchoffice.files.internal;

import java.io.IOException;
import java.io.InputStream;

/** Where file bytes live. Local disk now; an S3-compatible store (Cloudflare R2) at deployment. */
interface FileStorage {

    void put(String key, InputStream content) throws IOException;

    InputStream get(String key) throws IOException;
}
