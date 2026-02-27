/**
 * Removes keys with undefined values before crossing the Expo bridge.
 * Expo SDK 54 can throw when undefined values are present in JS objects.
 */
export function filterUndefined<T extends Record<string, any>>(obj: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(obj).filter(([_, value]) => value !== undefined),
  ) as Partial<T>
}
