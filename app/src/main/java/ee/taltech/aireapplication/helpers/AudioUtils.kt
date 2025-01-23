package ee.taltech.aireapplication.helpers

import java.io.FileOutputStream


object AudioUtils {

    // https://github.com/vishwakneelamegam/android-wav/blob/master/audioRecorder2/app/src/main/java/com/sailors/audiorecorder/wavClass.java

    fun wavHeader(
        totalAudioLen: Long,
        channels: Int = 1,
        sampleRate: Long = 16000,
        bpp: Byte = 16
    ): ByteArray? {

        val totalDataLen = totalAudioLen + 36
        val byteRate: Long = bpp * sampleRate * channels / 8

        try {
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte() // RIFF/WAVE header
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xffL).toByte()
            header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
            header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
            header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 4 bytes: size of 'fmt ' chunk
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // format = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xffL).toByte()
            header[25] = ((sampleRate shr 8) and 0xffL).toByte()
            header[26] = ((sampleRate shr 16) and 0xffL).toByte()
            header[27] = ((sampleRate shr 24) and 0xffL).toByte()
            header[28] = (byteRate and 0xffL).toByte()
            header[29] = ((byteRate shr 8) and 0xffL).toByte()
            header[30] = ((byteRate shr 16) and 0xffL).toByte()
            header[31] = ((byteRate shr 24) and 0xffL).toByte()
            header[32] = (2 * 16 / 8).toByte() // block align
            header[33] = 0
            header[34] = bpp // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xffL).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

            return header
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}