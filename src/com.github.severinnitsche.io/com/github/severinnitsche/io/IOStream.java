package com.github.severinnitsche.io;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class IOStream {

  private SyncedIOStream stream;
  long position;

  IOStream(SyncedIOStream stream) {
    this.stream = stream;
    this.position = 0;
  }

  public int read() throws IOException {
    return stream.read(this,position++);
  }

  public String readLine() throws IOException {
    var out = new ByteArrayOutputStream();
    int b = 0;
    while((b = read()) != '\n' && b != -1)
      out.write((byte)b);
    return out.toString();
  }

  public void write(byte b) throws IOException {
    stream.write(this,b);
  }

  public void write(byte[] bytes) throws IOException {
    for(byte b : bytes)
      write(b);
  }

  public void write(String s) throws IOException {
    write(s.getBytes());
  }

  public void writeLine(String s) throws IOException {
    write(s);
    write("\n");
  }

  public void writef(String f,Object...args) throws IOException {
    write(String.format(f,args));
  }

  public Ghost ghost() {
    return stream.ghost(this);
  }

  public void flush() throws IOException {
    stream.flush();
  }

  public void release() throws IOException {
    flush();
    stream.release(this,position);
  }
}
