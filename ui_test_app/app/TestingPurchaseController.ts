import * as Superwall from "expo-superwall"
import { Alert } from "react-native"
import { PurchaseResult, RestorationResult, SubscriptionStatusActive, SubscriptionStatusInactive } from "expo-superwall"

export class TestingPurchaseController {
  rejectPurchase: boolean = true
  restorePurchase: boolean = true
  shouldShowDialog: boolean = false

  constructor() {
  }

  async configureAndSyncSubscriptionStatus(): Promise<void> {
    const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
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
      return {
        type: 'failed',
        error: 'Purchase was rejected in TestingPurchaseController'
      }
    } else {
      return {
        type: 'purchased'
      }
    }
  }

  async purchaseFromGooglePlay(
    productId: string, 
    basePlanId?: string, 
    offerId?: string
  ): Promise<PurchaseResult> {
    if (this.rejectPurchase) {
      return {
        type: 'failed',
        error: 'Purchase was rejected in TestingPurchaseController'
      }
    } else {
      const activeStatus: SubscriptionStatusActive = {
        type: 'active',
        entitlements: [
          { id: 'test_entitlement' }
        ]
      }
      
      await Superwall.setSubscriptionStatus(activeStatus)

      return {
        type: 'purchased'
      }
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
      const activeStatus: SubscriptionStatusActive = {
        type: 'active',
        entitlements: [
          { id: 'test_entitlement' }
        ]
      }
      
      await Superwall.setSubscriptionStatus(activeStatus)
      
      return {
        type: 'restored'
      }
    } else {
      return {
        type: 'failed',
        error: 'Restore failed in TestingPurchaseController'
      }
    }
  }
} 