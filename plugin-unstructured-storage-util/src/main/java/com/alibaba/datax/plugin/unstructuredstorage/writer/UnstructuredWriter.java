package com.alibaba.datax.plugin.unstructuredstorage.writer;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface UnstructuredWriter extends Closeable {

    void writeOneRecord(List<String> splittedRows) throws IOException;

    void writeHeader(List<String> header) throws IOException;

    void appendCommit() throws IOException;

    void flush() throws IOException;

    @Override
    void close() throws IOException;

}
