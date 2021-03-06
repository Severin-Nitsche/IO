package com.github.severinnitsche.io;

/**
* The base âĄī¸ interface for input operations
*/
public interface IStream extends java.io.Closeable {
  /**
  * Fork a đą ghost from this streams root
  * @return đ A ghost
  */
  Ghost ghost();
  /**
  * đ§ Reads the next byte of data
  * @return đ The âšī¸ information
  * @throws java.io.IOException when the đ underlying stream đ throws an Exception
  */
  int read() throws java.io.IOException;
}
