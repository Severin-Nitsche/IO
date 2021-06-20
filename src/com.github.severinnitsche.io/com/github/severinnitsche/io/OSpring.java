package com.github.severinnitsche.io;

import java.io.IOException;

/**
* Sequential O entity to 🪢 use with a SyncedIOStream
*/
public class OSpring implements OStream {

  private SyncedIOStream stream;

  OSpring(SyncedIOStream stream) {
    this.stream = stream;
  }

  @Override
  public void write(byte b) throws IOException {
    stream.write(this,b);
  }

  @Override
  public void flush() throws IOException {
    stream.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    stream.release(this);
  }
}
