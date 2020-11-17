package com.github.faucamp.simplertmp.io

import com.github.faucamp.simplertmp.Util
import com.github.faucamp.simplertmp.packets.RtmpHeader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * Chunk stream channel information
 *
 * @author francois, leo
 */
internal class ChunkStreamInfo {

  companion object {
    const val RTMP_CID_PROTOCOL_CONTROL: Byte = 0x02
    const val RTMP_CID_OVER_CONNECTION: Byte = 0x03
    const val RTMP_CID_OVER_CONNECTION2: Byte = 0x04
    const val RTMP_CID_OVER_STREAM: Byte = 0x05
    const val RTMP_CID_VIDEO: Byte = 0x06
    const val RTMP_CID_AUDIO: Byte = 0x07
    private var sessionBeginTimestamp: Long = 0

    /** Sets the session beginning timestamp for all chunks  */
    @JvmStatic
    fun markSessionTimestampTx() {
      sessionBeginTimestamp = System.nanoTime() / 1000000
    }
  }

  private var prevHeaderRx: RtmpHeader? = null
  /** @return the previous header that was transmitted on this channel
   */
  /** Sets the previous header that was transmitted on this channel  */
  var prevHeaderTx: RtmpHeader? = null
  private var realLastTimestamp = System.nanoTime() / 1000000 // Do not use wall time!
  private val baos = ByteArrayOutputStream(1024 * 128)

  val storedPacketInputStream: ByteArrayInputStream
    get() {
      val bis = ByteArrayInputStream(baos.toByteArray())
      baos.reset()
      return bis
    }
  /** @return the previous header that was received on this channel, or `null` if no previous header was received
   */
  fun prevHeaderRx(): RtmpHeader? {
    return prevHeaderRx
  }

  /** Sets the previous header that was received on this channel, or `null` if no previous header was sent  */
  fun setPrevHeaderRx(previousHeader: RtmpHeader?) {
    prevHeaderRx = previousHeader
  }

  fun canReusePrevHeaderTx(forMessageType: RtmpHeader.MessageType): Boolean {
    return prevHeaderTx != null && prevHeaderTx!!.messageType == forMessageType
  }

  /** Utility method for calculating & synchronizing transmitted timestamps  */
  fun markAbsoluteTimestampTx(): Long {
    return System.nanoTime() / 1000000 - sessionBeginTimestamp
  }

  /** Utility method for calculating & synchronizing transmitted timestamp deltas  */
  fun markDeltaTimestampTx(): Long {
    val currentTimestamp = System.nanoTime() / 1000000
    val diffTimestamp = currentTimestamp - realLastTimestamp
    realLastTimestamp = currentTimestamp
    return diffTimestamp
  }

  /** @return `true` if all packet data has been stored, or `false` if not
   */
  @Throws(IOException::class)
  fun storePacketChunk(`in`: InputStream?, chunkSize: Int): Boolean {
    val remainingBytes = prevHeaderRx!!.packetLength - baos.size()
    val chunk = ByteArray(min(remainingBytes, chunkSize))
    Util.readBytesUntilFull(`in`, chunk)
    baos.write(chunk)
    return baos.size() == prevHeaderRx!!.packetLength
  }

  /** Clears all currently-stored packet chunks (used when an ABORT packet is received)  */
  fun clearStoredChunks() {
    baos.reset()
  }
}