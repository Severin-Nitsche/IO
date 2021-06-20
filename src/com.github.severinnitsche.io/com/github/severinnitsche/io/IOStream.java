package com.github.severinnitsche.io;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
* Sequential I/O entity to use with a SyncedIOStream
*/
public class IOStream implements IStream, OStream {

  private SyncedIOStream stream;
  long position;

  IOStream(SyncedIOStream stream) {
    this.stream = stream;
    this.position = 0;
  }

  @Override
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

  @Override
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

  @Override
  public Ghost ghost() {
    return stream.ghost(this);
  }

  @Override
  public void flush() throws IOException {
    stream.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    stream.release(this,position);
  }

  /**
  * 🎭 Alias for close
  * @throws IOException as specified for {@link close()}
  */
  public void release() throws IOException {
    close();
  }
}
