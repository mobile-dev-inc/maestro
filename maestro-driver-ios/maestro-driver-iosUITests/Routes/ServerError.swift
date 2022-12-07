enum ServerError: Error {
    case ApplicationSnapshotFailure
    case SnapshotSerializeFailure
    case GetRunningAppRequestSerializeFailure
    case GetRunningAppResponseSerializeFailure
}
