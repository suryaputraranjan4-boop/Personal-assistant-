package com.example.audio

import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

object AudioUtils {

    /**
     * Converts raw 16-bit PCM audio samples into a standard RIFF/WAV format byte array.
     * If the audio already has a RIFF WAV header, returns it unmodified.
     */
    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        if (pcmData.isEmpty()) return ByteArray(0)

        // Check if already has "RIFF" header
        if (pcmData.size > 12 &&
            pcmData[0] == 'R'.code.toByte() &&
            pcmData[1] == 'I'.code.toByte() &&
            pcmData[2] == 'F'.code.toByte() &&
            pcmData[3] == 'F'.code.toByte()
        ) {
            return pcmData
        }

        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)

        val header = ByteArray(44)
        // RIFF chunk descriptor
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // WAVE header
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // SubChunk1Size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        // SampleRate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        // BlockAlign
        header[32] = (channels * (bitsPerSample / 8)).toByte()
        header[33] = 0
        // BitsPerSample
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val output = ByteArrayOutputStream(44 + totalAudioLen)
        output.write(header)
        output.write(pcmData)
        return output.toByteArray()
    }

    /**
     * Extracts sample rate from mimeType if present (e.g. "audio/pcm;rate=24000").
     */
    fun extractSampleRateFromMime(mimeType: String, defaultRate: Int = 24000): Int {
        val rateRegex = Regex("rate=(\\d+)")
        val match = rateRegex.find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultRate
    }

    /**
     * Calculates normalized RMS amplitude (0.0f to 1.0f) from a short PCM buffer.
     */
    fun calculateRmsAmplitude(buffer: ShortArray, readSize: Int): Float {
        if (readSize <= 0) return 0f
        var sum = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        val rms = sqrt(sum / readSize)
        // Normalize against max short value ~ 32767
        val normalized = (rms / 32767.0).toFloat() * 3.5f // boost gain for UI visualizer
        return normalized.coerceIn(0f, 1f)
    }
}
