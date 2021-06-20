package com.github.severinnitsche.io;

/**
* The base ⚡️ interface for output operations
*/
public interface OStream extends java.io.Closeable, java.io.Flushable {
  /**
  * ✍️ Writes a singular byte of data
  * @param b The byte to be ✍️ written
  * @throws java.io.IOException When the 🛌 underlying stream 🏈 throws an exception
  */
  void write(byte b) throws java.io.IOException;
}
