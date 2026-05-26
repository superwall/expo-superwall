import { Asset } from "expo-asset"
import type { LocalResource } from "./SuperwallOptions"

/**
 * Resolves a {@link LocalResource} to a URI string that the native SDK can consume.
 *
 * - `number` (Metro `require()`) is resolved via `expo-asset`, downloading if necessary
 *   so the asset has a `file://` `localUri`.
 * - `string` is returned as-is.
 * - `{ uri }` returns the `uri` field.
 */
async function resolveLocalResource(resource: LocalResource): Promise<string> {
  if (typeof resource === "number") {
    const asset = Asset.fromModule(resource)
    if (!asset.localUri) {
      await asset.downloadAsync()
    }
    return asset.localUri ?? asset.uri
  }
  if (typeof resource === "string") {
    return resource
  }
  return resource.uri
}

/**
 * Resolves a map of local resources to plain `{ id: uri }` strings suitable for serializing
 * across the Expo bridge. Entries that fail to resolve are dropped and logged.
 */
export async function resolveLocalResources(
  input: Record<string, LocalResource> | undefined,
): Promise<Record<string, string> | undefined> {
  if (!input) return undefined
  const ids = Object.keys(input)
  if (ids.length === 0) return undefined

  const resolved = await Promise.all(
    ids.map(async (id) => {
      try {
        const uri = await resolveLocalResource(input[id]!)
        return [id, uri] as const
      } catch (error) {
        console.warn(`[Superwall] Failed to resolve local resource "${id}"`, error)
        return null
      }
    }),
  )

  const entries = resolved.filter((entry): entry is readonly [string, string] => entry !== null)
  return entries.length > 0 ? Object.fromEntries(entries) : undefined
}
