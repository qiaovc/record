import Foundation
import AVFoundation

public enum RecorderError: Error {
  case error(message: String, details: String?)
}

public enum RecordState: Int {
  case pause = 0
  case record = 1
  case stop = 2
}

public protocol AudioRecordingDelegate {
  func stop(completionHandler: @escaping (String?) -> ())
  func cancel() throws
  func getAmplitude() -> Float
  func pause()
  func resume() throws
  func dispose()
}

public protocol AudioRecordingFileDelegate: AudioRecordingDelegate {
  func initRecorder(config: RecordConfig, path: String) throws
  func start(config: RecordConfig, path: String) throws
}

public protocol AudioRecordingStreamDelegate: AudioRecordingDelegate {
  // func initRecorder(config: RecordConfig, recordEventHandler: RecordStreamHandler) throws
  func start(config: RecordConfig, recordEventHandler: RecordStreamHandler) throws
}
