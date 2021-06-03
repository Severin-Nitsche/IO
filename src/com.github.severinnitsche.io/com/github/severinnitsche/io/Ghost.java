package com.github.severinnitsche.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ghost {

  IOStream stream;
  long offset;
  SyncedIOStream sync;
  long position;

  AtomicBoolean active;

  public Ghost(IOStream stream, boolean active, long offset, SyncedIOStream sync) {
    this.stream = stream;
    this.sync = sync;
    this.position = 0;
    this.offset = offset;
    this.active = new AtomicBoolean(active);
  }

  public int ghost() throws IOException {
    while(!active.get()); //block until activation
    return sync.ghost(offset+position++);
  }

  public String ghostLine() throws IOException {
    var out = new ByteArrayOutputStream();
    int b = 0;
    while((b = ghost()) != '\n' && b != -1)
      out.write((byte)b);
    return out.toString();
  }

  public void release() {
    sync.release(this);
  }
}
