/**
 * Removes keys with undefined values before crossing the Expo bridge.
 * Expo SDK 54 can throw when undefined values are present in JS objects.
 */
export function filterUndefined<T extends Record<string, any>>(obj: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(obj).filter(([_, value]) => value !== undefined),
  ) as Partial<T>
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (value === null || typeof value !== "object") return false
  const prototype = Object.getPrototypeOf(value)
  return prototype === Object.prototype || prototype === null
}

/**
 * Like {@link filterUndefined}, but also removes undefined values from nested plain objects
 * and arrays. Class instances and other non-plain values are left as they are.
 */
export function filterUndefinedDeep<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.filter((item) => item !== undefined).map(filterUndefinedDeep) as T
  }
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([_, child]) => child !== undefined)
        .map(([key, child]) => [key, filterUndefinedDeep(child)]),
    ) as T
  }
  return value
}
