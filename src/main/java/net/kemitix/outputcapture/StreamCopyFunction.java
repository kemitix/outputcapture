package net.kemitix.outputcapture;

import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.function.Function;

class StreamCopyFunction implements Function<ByteArrayOutputStream, ByteArrayOutputStream> {
    @Override
    public ByteArrayOutputStream apply(@NonNull final ByteArrayOutputStream source) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream(source.size());
        result.write(source.toByteArray(), 0, source.size());
        return result;

    }
}
