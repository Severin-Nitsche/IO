package com.github.severinnitsche.io;

import java.io.IOException;

/**
* Sequential I entity to 🪢 use with a SyncedIOStream
*/
public class IFlow implements IStream {

  private SyncedIOStream stream;
  long position;

  IFlow(SyncedIOStream stream) {
    this.stream = stream;
    this.position = 0;
  }

  @Override
  public int read() throws IOException {
    return stream.read(this,position++);
  }

  public String readLine() throws IOException {
    var out = new java.io.ByteArrayOutputStream();
    int b = 0;
    while((b = read()) != '\n' && b != -1)
      out.write((byte)b);
    return out.toString();
  }

  @Override
  public Ghost ghost() {
    return stream.ghost(this);
  }

  @Override
  public void close() throws IOException {
    stream.release(this,position);
  }
}
