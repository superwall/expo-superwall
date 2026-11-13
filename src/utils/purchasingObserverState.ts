import type { PurchasingObserverState } from "../SuperwallExpoModule.types"
import { filterUndefined } from "./filterUndefined"

/**
 * Serializes a {@link PurchasingObserverState} for the native bridge. Drops `undefined` at both
 * levels, which the Android bridge can't convert.
 */
export function purchasingObserverStateToJson(
  state: PurchasingObserverState,
): PurchasingObserverState {
  if (state.type === "purchaseResult" && state.purchases) {
    return filterUndefined({
      ...state,
      purchases: state.purchases.map(
        (purchase) => filterUndefined(purchase) as (typeof state.purchases)[number],
      ),
    }) as PurchasingObserverState
  }
  return filterUndefined(state) as PurchasingObserverState
}
