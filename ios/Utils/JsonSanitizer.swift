//
//  JsonSanitizer.swift
//  SuperwallKit
//
//  Created for preventing silent JSON serialization failures
//

import Foundation
import os.log

/// Validates and sanitizes a dictionary before sending via Expo's sendEvent
/// - Removes nil values and nested Optional wrappers
/// - Logs warnings in debug builds for problematic values
/// - Returns a clean [String: Any] dictionary safe for JSON serialization
func sanitizeJsonDictionary(_ input: [String: Any?]) -> [String: Any] {
  var sanitized: [String: Any] = [:]

  for (key, value) in input {
    // Skip nil values
    guard let unwrappedValue = value else {
      #if DEBUG
      os_log(.debug, log: .default, "JsonSanitizer: Skipping nil value for key '%{public}@'", key)
      #endif
      continue
    }

    // Unwrap SuperwallKit's SwiftyJSON wrapper
    let typeName = String(describing: type(of: unwrappedValue))
    if typeName == "JSON" {
      // Use reflection to access .object property
      let mirror = Mirror(reflecting: unwrappedValue)
      if let objectProperty = mirror.children.first(where: { $0.label == "object" })?.value {
        sanitized[key] = objectProperty
        continue
      }
    }

    // Recursively sanitize nested dictionaries
    if let nestedDict = unwrappedValue as? [String: Any?] {
      sanitized[key] = sanitizeJsonDictionary(nestedDict)
      continue
    }

    // Sanitize arrays
    if let array = unwrappedValue as? [Any?] {
      sanitized[key] = sanitizeJsonArray(array)
      continue
    }

    // Validate that the value is JSON-serializable
    if isJsonSerializable(unwrappedValue) {
      sanitized[key] = unwrappedValue
    } else {
      #if DEBUG
      os_log(.error, log: .default,
        "JsonSanitizer: Non-serializable value for key '%{public}@': %{public}@. Converting to string.",
        key, String(describing: type(of: unwrappedValue)))
      // Don't crash - just log and convert to string
      #endif
      // Convert to string representation as fallback
      sanitized[key] = String(describing: unwrappedValue)
    }
  }

  return sanitized
}

/// Sanitizes an array by removing nil values and validating elements
private func sanitizeJsonArray(_ input: [Any?]) -> [Any] {
  return input.compactMap { element -> Any? in
    guard let unwrapped = element else { return nil }

    if let nestedDict = unwrapped as? [String: Any?] {
      return sanitizeJsonDictionary(nestedDict)
    }

    if let nestedArray = unwrapped as? [Any?] {
      return sanitizeJsonArray(nestedArray)
    }

    if isJsonSerializable(unwrapped) {
      return unwrapped
    }

    #if DEBUG
    os_log(.error, log: .default,
      "JsonSanitizer: Non-serializable array element: %{public}@",
      String(describing: type(of: unwrapped)))
    #endif

    return String(describing: unwrapped)
  }
}

/// Checks if a value is JSON-serializable (String, Number, Bool, Dictionary, Array)
private func isJsonSerializable(_ value: Any) -> Bool {
  // Handle SuperwallKit's SwiftyJSON wrapper
  let typeName = String(describing: type(of: value))
  if typeName == "JSON" {
    // Use reflection to access .object property
    let mirror = Mirror(reflecting: value)
    if let objectProperty = mirror.children.first(where: { $0.label == "object" })?.value {
      return isJsonSerializable(objectProperty)
    }
  }

  // Check basic JSON types
  if value is String || value is NSNumber || value is Bool {
    return true
  }

  if value is Int || value is Double || value is Float || value is Int64 {
    return true
  }

  // Check collection types
  if value is [String: Any] || value is [Any] {
    return true
  }

  // NSNull is valid in JSON
  if value is NSNull {
    return true
  }

  // Try JSONSerialization as final check
  return JSONSerialization.isValidJSONObject([value])
}

// MARK: - Safe Event Validation

/// Validates and sends an event with sanitized JSON data
/// - Parameters:
///   - data: The data dictionary to validate
///   - eventName: The event name for debugging
/// - Returns: A sanitized dictionary safe for JSON serialization
func validateEventData(_ data: [String: Any?], eventName: String) -> [String: Any] {
  let sanitized = sanitizeJsonDictionary(data)

  #if DEBUG
  // Verify that the sanitized data is actually JSON-serializable
  if !JSONSerialization.isValidJSONObject(sanitized) {
    os_log(.fault, log: .default,
      "JsonSanitizer: Failed to produce valid JSON for event '%{public}@'", eventName)
    // Don't crash - the sanitizer should have handled everything
  }
  #endif

  return sanitized
}

/// Validates and sends an event with sanitized JSON data (non-optional overload)
/// - Parameters:
  ///   - data: The data dictionary to validate
///   - eventName: The event name for debugging
/// - Returns: A sanitized dictionary safe for JSON serialization
func validateEventData(_ data: [String: Any], eventName: String) -> [String: Any] {
  // Convert to optional dictionary for consistent handling
  let optionalDict: [String: Any?] = data.mapValues { $0 as Any? }
  return validateEventData(optionalDict, eventName: eventName)
}
