/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.compress.utils.IOUtils;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

public class ZstandardCodec extends Codec {

    static class Option extends CodecFactory {
        private final int compressionLevel;

        Option(int compressionLevel) {
          this.compressionLevel = compressionLevel;
        }

        @Override
        protected Codec createInstance() {
          return new ZstandardCodec(compressionLevel);
        }
    }

    private final int compressionLevel;
    private ByteArrayOutputStream outputBuffer;

    public ZstandardCodec(int compressionLevel) {
      this.compressionLevel =
          Math.max(Math.min(compressionLevel, Zstd.maxCompressionLevel()),
                   Zstd.minCompressionLevel());
    }

    @Override
    public String getName() {
        return DataFileConstants.ZSTANDARD_CODEC;
    }

    @Override
    public ByteBuffer compress(ByteBuffer data) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(data.remaining());
        try (OutputStream outputStream = new ZstdOutputStream(baos, compressionLevel)) {
           outputStream.write(data.array(), computeOffset(data), data.remaining());
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }

    @Override
    public ByteBuffer decompress(ByteBuffer compressedData) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(compressedData.remaining());
        InputStream bytesIn = new ByteArrayInputStream(
          compressedData.array(),
          computeOffset(compressedData),
          compressedData.remaining());
        try (InputStream ios = new ZstdInputStream(bytesIn)) {
            IOUtils.copy(ios, baos);
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }

    // get and initialize the output buffer for use.
    private ByteArrayOutputStream getOutputBuffer(int suggestedLength) {
      if (outputBuffer == null) {
        outputBuffer = new ByteArrayOutputStream(suggestedLength);
      }
      outputBuffer.reset();
      return outputBuffer;
    }

    @Override
    public int hashCode() {
      return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj)
            || (obj != null && obj.getClass() == this.getClass());
    }

    @Override
    public String toString() {
      return getName() + "[" + compressionLevel + "]";
    }
}
