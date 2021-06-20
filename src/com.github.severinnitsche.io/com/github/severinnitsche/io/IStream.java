package com.github.severinnitsche.io;

/**
* The base ⚡️ interface for input operations
*/
public interface IStream extends java.io.Closeable {
  /**
  * Fork a 😱 ghost from this streams root
  * @return 👉 A ghost
  */
  Ghost ghost();
  /**
  * 🧐 Reads the next byte of data
  * @return 👉 The ℹ️ information
  * @throws java.io.IOException when the 🛌 underlying stream 🏈 throws an Exception
  */
  int read() throws java.io.IOException;
}
