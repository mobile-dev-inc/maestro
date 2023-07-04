//
//  Errors.swift
//  maestro-driver-iosUITests
//
//  Created by Amanjeet Singh on 04/07/23.
//

import Foundation

enum ViewHierarchyErrorType: Codable { case MaxDepthExceededError, UnknownError }

struct ViewHierarchyError: Error, Codable {
    let message: String
    let errorType: ViewHierarchyErrorType
}
