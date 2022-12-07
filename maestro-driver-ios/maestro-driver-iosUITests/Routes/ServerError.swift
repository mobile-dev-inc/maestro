enum ServerError: Error {
    case ApplicationSnapshotFailure
    case SnapshotSerializeFailure
    case RunningAppRequestSerializeFailure
    case RunningAppResponseSerializeFailure
}
