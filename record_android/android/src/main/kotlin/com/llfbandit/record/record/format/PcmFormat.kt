package com.qiaovc.record.record.format

import android.media.MediaFormat
import com.qiaovc.record.record.RecordConfig
import com.qiaovc.record.record.container.IContainerWriter
import com.qiaovc.record.record.container.RawContainer

class PcmFormat : Format() {
    override val mimeTypeAudio: String = MediaFormat.MIMETYPE_AUDIO_RAW
    override val passthrough: Boolean = true

    override fun getMediaFormat(config: RecordConfig): MediaFormat {
        val bitsPerSample = 16
        val frameSize = config.numChannels * bitsPerSample / 8

        val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mimeTypeAudio)
            setInteger(MediaFormat.KEY_SAMPLE_RATE, config.sampleRate)
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, config.numChannels)
            setInteger(KEY_X_FRAME_SIZE_IN_BYTES, frameSize)
        }

        return format
    }


    override fun getContainer(path: String?): IContainerWriter {
        return RawContainer(path)
    }
}