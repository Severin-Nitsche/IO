package com.github.severinnitsche.io;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
* Sequential I/O entity to use with a SyncedIOStream
*/
public class IOStream {

  private SyncedIOStream stream;
  long position;

  IOStream(SyncedIOStream stream) {
    this.stream = stream;
    this.position = 0;
  }

  /**
  * read the next byte of data
  * @return The information
  * @throws IOException when the underlying stream throws an exception
  */
  public int read() throws IOException {
    return stream.read(this,position++);
  }

  /**
  * Bulk read method
  * @return The next line
  * @throws IOException when the underlying stream throws an exception
  */
  public String readLine() throws IOException {
    var out = new ByteArrayOutputStream();
    int b = 0;
    while((b = read()) != '\n' && b != -1)
      out.write((byte)b);
    return out.toString();
  }

  /**
  * Write a singular byte of data
  * @param b The byte to be written
  * @throws IOException when the underlying stream throws an exception
  */
  public void write(byte b) throws IOException {
    stream.write(this,b);
  }

  /**
  * Bulk write method
  * @param bytes The bytes to be written
  * @throws IOException when the underlying stream throws an exception
  */
  public void write(byte[] bytes) throws IOException {
    for(byte b : bytes)
      write(b);
  }

  /**
  * Bulk write method
  * @param s The string to be written
  * @throws IOException when the underlying stream throws an exception
  */
  public void write(String s) throws IOException {
    write(s.getBytes());
  }

  /**
  * Bulk write method
  * @param s The line to be written (uses \n as line separator)
  * @throws IOException when the underlying stream throws an exception
  */
  public void writeLine(String s) throws IOException {
    write(s);
    write("\n");
  }

  /**
  * Bulk write method to mimic printf
  * @param f The format String
  * @param args The format arguments
  * @throws IOException when the underlying stream throws an exception
  */
  public void writef(String f,Object...args) throws IOException {
    write(String.format(f,args));
  }

  /**
  * Fork a ghost from this Streams root
  * @return A ghost
  */
  public Ghost ghost() {
    return stream.ghost(this);
  }

  /**
  * Call for the underlying I/O Stream to be flushed
  * @throws IOException when the underlying stream throws an exception
  */
  public void flush() throws IOException {
    stream.flush();
  }

  /**
  * Flushes the stream and marks its memory consumption as to be freed
  * @throws IOException when the underlying stream throws an exception
  */
  public void release() throws IOException {
    flush();
    stream.release(this,position);
  }
}
