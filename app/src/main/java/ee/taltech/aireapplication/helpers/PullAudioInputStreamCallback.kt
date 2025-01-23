package ee.taltech.aireapplication.helpers

abstract class PullAudioInputStreamCallback
{
    /**
     * Reads data from audio input stream into the data buffer. The maximal number of bytes to be read is determined by the size of dataBuffer.
     * If there is no data immediately available, read() blocks until the next data becomes available.
     * Note: The dataBuffer returned by read() should not contain any audio header.
     * @param dataBuffer The byte array to store the read data.
     * @return The number of bytes filled, or 0 in case the stream hits its end and there is no more data available.
     */
    abstract fun read(dataBuffer: ByteArray): Int
    abstract fun close()
}