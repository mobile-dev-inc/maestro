
import Foundation

func objcTry<T>(_ block: @escaping () -> T) throws -> T {
    var result: T?

    let exception = tryBlock {
        result = block()
    }

    if let exception = exception {
        throw exception
    }

    return result!
}
