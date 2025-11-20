package com.example.smarthomeapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraFragment : Fragment() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var recordButton: Button
    private lateinit var talkButton: Button

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    // Audio Streaming components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isStreaming = false
    private lateinit var audioBuffer: ByteArray
    private lateinit var streamingThread: Thread

    private val permissionsRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera and audio permissions are required.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.camera_preview)
        recordButton = view.findViewById(R.id.btn_record)
        talkButton = view.findViewById(R.id.btn_talk)

        recordButton.setOnClickListener { recordVideo() }

        talkButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startStreaming()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopStreaming()
                    true
                }
                else -> false
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionsRequest.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, videoCapture)
        } catch (exc: Exception) {
            Log.e("CameraFragment", "Use case binding failed", exc)
        }
    }

    private fun recordVideo() {
        val videoCapture = this.videoCapture ?: return
        recordButton.isEnabled = false
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = "SmartHome-Video-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // For Android 10 and above
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SmartHome-Videos")
            } else { // For older versions
                val videoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/SmartHome-Videos"
                File(videoDir).mkdirs() // Ensure the directory exists
                put(MediaStore.Video.Media.DATA, "$videoDir/$name")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues).build()

        recording = videoCapture.output.prepareRecording(requireContext(), mediaStoreOutputOptions).apply {
            if (PermissionChecker.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    recordButton.text = "Stop Recording"
                    recordButton.isEnabled = true
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val uri = recordEvent.outputResults.outputUri
                        val msg = "Video saved successfully: $uri"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    } else {
                        recording?.close()
                        recording = null
                        Log.e("CameraFragment", "Video recording failed: ${recordEvent.error}")
                    }
                    recordButton.text = "Record"
                    recordButton.isEnabled = true
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startStreaming() {
        if (!isStreaming) {
            isStreaming = true
            Toast.makeText(context, "Talk button held", Toast.LENGTH_SHORT).show()

            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioBuffer = ByteArray(bufferSize)

            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()

            streamingThread = Thread {
                while (isStreaming) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSize > 0) {
                        audioTrack?.write(audioBuffer, 0, readSize)
                    }
                }
            }
            streamingThread.start()
        }
    }

    private fun stopStreaming() {
        if (isStreaming) {
            isStreaming = false
            Toast.makeText(context, "Talk button released", Toast.LENGTH_SHORT).show()

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
    }

    companion object {
        private const val ARG_DEVICE_ID = "device_id"
        fun newInstance(deviceId: String) = CameraFragment().apply {
            arguments = Bundle().apply { putString(ARG_DEVICE_ID, deviceId) }
        }
    }
}
