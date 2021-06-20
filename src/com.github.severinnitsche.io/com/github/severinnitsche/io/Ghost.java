package com.github.severinnitsche.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Parallel reading entity for the use with a SyncedIOStream
*/
public class Ghost {

  IStream stream;
  long offset;
  SyncedIOStream sync;
  long position;

  AtomicBoolean active;

  Ghost(IStream stream, boolean active, long offset, SyncedIOStream sync) {
    this.stream = stream;
    this.sync = sync;
    this.position = 0;
    this.offset = offset;
    this.active = new AtomicBoolean(active);
  }

  /**
  * Read the next byte of information (blocking)
  * @return The next byte not consumed by this ghost
  * @throws IOException when the underlying stream throws an exception
  */
  public int ghost() throws IOException {
    while(!active.get()); //block until activation
    return sync.ghost(offset+position++);
  }

  /**
  * Bulk read method
  * @return The next line
  * @throws IOException when the underlying stream throws an exception
  */
  public String ghostLine() throws IOException {
    var out = new ByteArrayOutputStream();
    int b = 0;
    while((b = ghost()) != '\n' && b != -1)
      out.write((byte)b);
    return out.toString();
  }

  /**
  * Notify the underlying system of the release of this ghost to allow for resources to be freed
  */
  public void release() {
    sync.release(this);
  }
}
