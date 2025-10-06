import Superwall, {
  type PurchaseResult,
  PurchaseResultFailed,
  PurchaseResultPurchased,
  RestorationResult,
  type SubscriptionStatus,
} from "expo-superwall/compat"
import { Entitlement } from "expo-superwall/compat/lib/Entitlement"
import { Alert } from "react-native"

export class TestingPurchaseController {
  rejectPurchase = true
  restorePurchase = true
  shouldShowDialog = false

  async configureAndSyncSubscriptionStatus(): Promise<void> {
    const inactiveStatus: SubscriptionStatus = { status: "INACTIVE" }
    await Superwall.shared.setSubscriptionStatus(inactiveStatus)
  }

  toggleRejectPurchase(): void {
    this.rejectPurchase = !this.rejectPurchase
  }

  toggleRestorePurchase(): void {
    this.restorePurchase = !this.restorePurchase
  }

  toggleShowDialog(): void {
    this.shouldShowDialog = !this.shouldShowDialog
  }

  private showResultDialog(title: string, message: string): Promise<void> {
    return new Promise((resolve) => {
      Alert.alert(title, message, [
        {
          text: "OK",
          onPress: () => resolve(),
        },
      ])
    })
  }

  async purchaseFromAppStore(productId: string): Promise<PurchaseResult> {
    if (this.rejectPurchase) {
      return new PurchaseResultFailed("Purchase was rejected in TestingPurchaseController")
    }
    return new PurchaseResultPurchased()
  }

  async purchaseFromGooglePlay(
    productId: string,
    basePlanId?: string,
    offerId?: string,
  ): Promise<PurchaseResult> {
    if (this.rejectPurchase) {
      return new PurchaseResultFailed("Purchase was rejected in TestingPurchaseController")
    }
    const activeStatus: SubscriptionStatus = {
      status: "ACTIVE",
      entitlements: [new Entitlement("test_entitlement")],
    }

    await Superwall.shared.setSubscriptionStatus(activeStatus)

    return new PurchaseResultPurchased()
  }

  async restorePurchases(): Promise<RestorationResult> {
    if (this.shouldShowDialog) {
      await this.showResultDialog(
        this.restorePurchase ? "Restore Success" : "Restore Failed",
        this.restorePurchase
          ? "Purchases were restored successfully."
          : "Failed to restore purchases.",
      )
    }

    if (this.restorePurchase) {
      const activeStatus: SubscriptionStatus = {
        status: "ACTIVE",
        entitlements: [new Entitlement("test_entitlement")],
      }

      await Superwall.shared.setSubscriptionStatus(activeStatus)

      return RestorationResult.restored()
    }

    return RestorationResult.failed(new Error("Restore failed in TestingPurchaseController"))
  }
}
