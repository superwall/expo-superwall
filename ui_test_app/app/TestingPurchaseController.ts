import * as Superwall from "expo-superwall/compat"
import { Alert } from "react-native"
import { PurchaseResult, PurchaseResultFailed, PurchaseResultPurchased, PurchaseResultRestored, RestorationResult, SubscriptionStatus } from "expo-superwall/compat"
import { Entitlement } from "expo-superwall/compat/lib/Entitlement"

export class TestingPurchaseController {
  rejectPurchase: boolean = true
  restorePurchase: boolean = true
  shouldShowDialog: boolean = false

  constructor() {
  }

  async configureAndSyncSubscriptionStatus(): Promise<void> {
    const inactiveStatus: SubscriptionStatus = { status: 'INACTIVE' }
    await Superwall.setSubscriptionStatus(inactiveStatus)
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
      Alert.alert(
        title,
        message,
        [
          {
            text: 'OK',
            onPress: () => resolve()
          }
        ]
      )
    })
  }

  async purchaseFromAppStore(productId: string): Promise<PurchaseResult> {
    if (this.rejectPurchase) {
      return new PurchaseResultFailed('Purchase was rejected in TestingPurchaseController')
    } else {
      return new PurchaseResultPurchased()
    }
  }

  async purchaseFromGooglePlay(
    productId: string, 
    basePlanId?: string, 
    offerId?: string
  ): Promise<PurchaseResult> {
    if (this.rejectPurchase) {
      return new PurchaseResultFailed('Purchase was rejected in TestingPurchaseController')
    } else {
      const activeStatus: SubscriptionStatus = {
        status: 'ACTIVE',
        entitlements: [
          new Entitlement('test_entitlement')
        ]
      }
      
      await Superwall.setSubscriptionStatus(activeStatus)

      return new PurchaseResultPurchased()
    }
  }

  async restorePurchases(): Promise<RestorationResult> {
    if (this.shouldShowDialog) {
      await this.showResultDialog(
        this.restorePurchase ? "Restore Success" : "Restore Failed",
        this.restorePurchase
          ? "Purchases were restored successfully."
          : "Failed to restore purchases."
      )
    }

    if (this.restorePurchase) {
      const activeStatus: SubscriptionStatus = {
        status: 'ACTIVE',
        entitlements: [
          new Entitlement('test_entitlement')
        ]
      }
      
      await Superwall.setSubscriptionStatus(activeStatus)
      
      return new PurchaseResultRestored()
    } else {
      return new PurchaseResultFailed('Restore failed in TestingPurchaseController')
    }
  }
} 