package com.qiaovc.record.record.recorder

import com.qiaovc.record.Utils
import com.qiaovc.record.record.AudioEncoder
import com.qiaovc.record.record.PCMReader
import com.qiaovc.record.record.RecordConfig
import com.qiaovc.record.record.encoder.EncoderListener
import com.qiaovc.record.record.encoder.IEncoder
import com.qiaovc.record.record.format.AacFormat
import com.qiaovc.record.record.format.AmrNbFormat
import com.qiaovc.record.record.format.AmrWbFormat
import com.qiaovc.record.record.format.FlacFormat
import com.qiaovc.record.record.format.Format
import com.qiaovc.record.record.format.OpusFormat
import com.qiaovc.record.record.format.PcmFormat
import com.qiaovc.record.record.format.WaveFormat
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean


class RecordThread(
    private val config: RecordConfig,
    private val recorderListener: OnAudioRecordListener
) : EncoderListener {
    private var mPcmReader: PCMReader? = null
    private var mEncoder: IEncoder? = null

    // Signals whether a recording is in progress (true) or not (false).
    private val mIsRecording = AtomicBoolean(false)

    // Signals whether a recording is paused (true) or not (false).
    private val mIsPaused = AtomicBoolean(false)
    private val mIsPausedSem = Semaphore(0)
    private var mHasBeenCanceled = false

    private val mExecutorService = Executors.newSingleThreadExecutor()

    override fun onEncoderFailure(ex: Exception) {
        recorderListener.onFailure(ex)
    }

    override fun onEncoderStream(bytes: ByteArray) {
        recorderListener.onAudioChunk(bytes)
    }

    fun isRecording(): Boolean {
        return mEncoder != null && mIsRecording.get()
    }

    fun isPaused(): Boolean {
        return mEncoder != null && mIsPaused.get()
    }

    fun pauseRecording() {
        if (isRecording()) {
            pauseState()
        }
    }

    fun resumeRecording() {
        if (isPaused()) {
            recordState()
        }
    }

    fun stopRecording() {
        if (isRecording()) {
            mIsRecording.set(false)
            mIsPaused.set(false)
            mIsPausedSem.release()
        }
    }

    fun cancelRecording() {
        if (isRecording()) {
            mHasBeenCanceled = true
            stopRecording()
        } else {
            Utils.deleteFile(config.path)
        }
    }

    fun getAmplitude(): Double = mPcmReader?.getAmplitude() ?: -160.0

    fun startRecording() {
        mExecutorService.execute {
            try {
                val format = selectFormat()
                val (encoder, adjustedFormat) = format.getEncoder(config, this)

                mPcmReader = PCMReader(config, adjustedFormat)
                mPcmReader!!.start()

                mEncoder = encoder
                mEncoder!!.startEncoding()

                recordState()

                while (isRecording()) {
                    if (isPaused()) {
                        mIsPausedSem.acquire()
                        // Check again if recording has been stopped
                        if (!isRecording()) break
                    }

                    val buffer = mPcmReader!!.read()
                    if (buffer.isNotEmpty()) {
                        mEncoder!!.encode(buffer)
                    }
                }
            } catch (ex: Exception) {
                recorderListener.onFailure(ex)
            } finally {
                stopAndRelease()
            }
        }
    }

    private fun stopAndRelease() {
        mPcmReader?.stop()
        mPcmReader?.release()
        mPcmReader = null

        mEncoder?.stopEncoding()
        mEncoder = null

        if (mHasBeenCanceled) {
            Utils.deleteFile(config.path)
        }

        recorderListener.onStop()
    }

    private fun selectFormat(): Format {
        when (config.encoder) {
            AudioEncoder.aacLc, AudioEncoder.aacEld, AudioEncoder.aacHe -> return AacFormat()
            AudioEncoder.amrNb -> return AmrNbFormat()
            AudioEncoder.amrWb -> return AmrWbFormat()
            AudioEncoder.flac -> return FlacFormat()
            AudioEncoder.pcm16bits -> return PcmFormat()
            AudioEncoder.opus -> return OpusFormat()
            AudioEncoder.wav -> return WaveFormat()
        }
        throw Exception("Unknown format: " + config.encoder)
    }

    private fun pauseState() {
        mIsRecording.set(true)
        mIsPaused.set(true)

        recorderListener.onPause()
    }

    private fun recordState() {
        mIsRecording.set(true)
        mIsPaused.set(false)

        mIsPausedSem.release()

        recorderListener.onRecord()
    }
}