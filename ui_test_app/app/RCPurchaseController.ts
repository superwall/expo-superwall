import * as Superwall from "expo-superwall"
import { Platform } from "react-native"

// Type definitions for RevenueCat (these would ideally come from react-native-purchases)
interface CustomerInfo {
  entitlements: {
    active: { [key: string]: any }
  }
  activeSubscriptions: string[]
  allPurchaseDates: { [key: string]: string | null }
}

interface StoreProduct {
  identifier: string
  productCategory?: ProductCategory
  subscriptionOptions?: SubscriptionOption[]
  defaultOption?: SubscriptionOption
}

interface SubscriptionOption {
  id: string
}

enum ProductCategory {
  subscription = 'subscription',
  nonSubscription = 'nonSubscription'
}

enum PurchasesErrorCode {
  paymentPendingError = 'paymentPendingError',
  purchaseCancelledError = 'purchaseCancelledError'
}

// Purchase Result types
interface PurchaseResultPurchased {
  type: 'purchased'
}

interface PurchaseResultFailed {
  type: 'failed'
  error: string
}

interface PurchaseResultPending {
  type: 'pending'
}

interface PurchaseResultCancelled {
  type: 'cancelled'
}

type PurchaseResult = PurchaseResultPurchased | PurchaseResultFailed | PurchaseResultPending | PurchaseResultCancelled

interface RestorationResultRestored {
  type: 'restored'
}

interface RestorationResultFailed {
  type: 'failed'
  error: string
}

type RestorationResult = RestorationResultRestored | RestorationResultFailed

interface Entitlement {
  id: string
}

interface SubscriptionStatusActive {
  type: 'active'
  entitlements: Entitlement[]
}

interface SubscriptionStatusInactive {
  type: 'inactive'
}

// Mock RevenueCat SDK interface
class MockPurchases {
  static async setLogLevel(level: string): Promise<void> {
    console.log(`Setting RevenueCat log level to: ${level}`)
  }

  static async configure(configuration: any): Promise<void> {
    console.log('Configuring RevenueCat with:', configuration)
  }

  static addCustomerInfoUpdateListener(callback: (customerInfo: CustomerInfo) => void): void {
    console.log('Adding customer info update listener')
    // In a real implementation, this would set up the listener
  }

  static async getProducts(productIds: string[], options?: any): Promise<StoreProduct[]> {
    console.log('Getting products:', productIds)
    // Mock products
    return productIds.map(id => ({
      identifier: id,
      productCategory: ProductCategory.subscription,
      subscriptionOptions: [{
        id: `${id}:base`
      }],
      defaultOption: {
        id: `${id}:base`
      }
    }))
  }

  static async purchaseStoreProduct(product: StoreProduct): Promise<CustomerInfo> {
    console.log('Purchasing store product:', product.identifier)
    return {
      entitlements: { active: { 'test_entitlement': {} } },
      activeSubscriptions: ['test_subscription'],
      allPurchaseDates: { 'test_product': new Date().toISOString() }
    }
  }

  static async purchaseSubscriptionOption(option: SubscriptionOption): Promise<CustomerInfo> {
    console.log('Purchasing subscription option:', option.id)
    return {
      entitlements: { active: { 'test_entitlement': {} } },
      activeSubscriptions: ['test_subscription'],
      allPurchaseDates: { 'test_product': new Date().toISOString() }
    }
  }

  static async restorePurchases(): Promise<CustomerInfo> {
    console.log('Restoring purchases')
    return {
      entitlements: { active: { 'test_entitlement': {} } },
      activeSubscriptions: ['test_subscription'],
      allPurchaseDates: { 'test_product': new Date().toISOString() }
    }
  }
}

export class RCPurchaseController {
  
  // Configure and sync subscription status
  async configureAndSyncSubscriptionStatus(): Promise<void> {
    // Configure RevenueCat
    await MockPurchases.setLogLevel('debug')
    
    const configuration = Platform.OS === 'ios'
      ? { apiKey: 'appl_PpUWCgFONlxwztRfNgEdvyGHiAG' }
      : { apiKey: 'goog_DCSOujJzRNnPmxdgjOwdOOjwilC' }
    
    await MockPurchases.configure(configuration)

    // Listen for changes
    MockPurchases.addCustomerInfoUpdateListener(async (customerInfo) => {
      // Gets called whenever new CustomerInfo is available
      const entitlements = Object.keys(customerInfo.entitlements.active)
        .map((id) => ({ id }))

      const hasActiveEntitlementOrSubscription = this.hasActiveEntitlementOrSubscription(customerInfo)

      if (hasActiveEntitlementOrSubscription) {
        const activeStatus: SubscriptionStatusActive = {
          type: 'active',
          entitlements: entitlements
        }
        await Superwall.setSubscriptionStatus(activeStatus)
      } else {
        const inactiveStatus: SubscriptionStatusInactive = { type: 'inactive' }
        await Superwall.setSubscriptionStatus(inactiveStatus)
      }
    })
  }

  // Handle Purchases for App Store
  async purchaseFromAppStore(productId: string): Promise<PurchaseResult> {
    try {
      // Find products matching productId from RevenueCat
      const products = await this.getAllProducts([productId])

      // Get first product for product ID
      const storeProduct = products[0]

      if (!storeProduct) {
        return {
          type: 'failed',
          error: `Failed to find store product for ${productId}`
        }
      }

      return await this.purchaseStoreProduct(storeProduct)
    } catch (error) {
      return {
        type: 'failed',
        error: `Purchase failed: ${error}`
      }
    }
  }

  // Handle Purchases for Google Play
  async purchaseFromGooglePlay(
    productId: string, 
    basePlanId?: string, 
    offerId?: string
  ): Promise<PurchaseResult> {
    try {
      // Find products matching productId from RevenueCat
      const products = await this.getAllProducts([productId])

      // Choose the product which matches the given base plan
      const storeProductId = `${productId}:${basePlanId}`

      // Try to find matching product
      let matchingProduct = products.find(product => product.identifier === storeProductId)
      const storeProduct = matchingProduct || products[0]

      if (!storeProduct) {
        return {
          type: 'failed',
          error: 'Product not found'
        }
      }

      switch (storeProduct.productCategory) {
        case ProductCategory.subscription:
          const subscriptionOption = await this.fetchGooglePlaySubscriptionOption(
            storeProduct, basePlanId, offerId
          )
          if (!subscriptionOption) {
            return {
              type: 'failed',
              error: 'Valid subscription option not found for product.'
            }
          }
          return await this.purchaseSubscriptionOption(subscriptionOption)
        case ProductCategory.nonSubscription:
          return await this.purchaseStoreProduct(storeProduct)
        default:
          return {
            type: 'failed',
            error: 'Unable to determine product category'
          }
      }
    } catch (error) {
      return {
        type: 'failed',
        error: `Purchase failed: ${error}`
      }
    }
  }

  private async fetchGooglePlaySubscriptionOption(
    storeProduct: StoreProduct,
    basePlanId?: string,
    offerId?: string
  ): Promise<SubscriptionOption | null> {
    const subscriptionOptions = storeProduct.subscriptionOptions

    if (subscriptionOptions && subscriptionOptions.length > 0) {
      // Concatenate base + offer ID
      const subscriptionOptionId = this.buildSubscriptionOptionId(basePlanId, offerId)

      // Find first subscription option that matches the subscription option ID or use the default offer
      let subscriptionOption = subscriptionOptions.find(option => option.id === subscriptionOptionId)

      // If no matching subscription option is found, use the default option
      subscriptionOption = subscriptionOption || storeProduct.defaultOption

      return subscriptionOption || null
    }

    return null
  }

  private async purchaseSubscriptionOption(subscriptionOption: SubscriptionOption): Promise<PurchaseResult> {
    const performPurchase = async (): Promise<CustomerInfo> => {
      return await MockPurchases.purchaseSubscriptionOption(subscriptionOption)
    }

    return await this.handleSharedPurchase(performPurchase)
  }

  private async purchaseStoreProduct(storeProduct: StoreProduct): Promise<PurchaseResult> {
    const performPurchase = async (): Promise<CustomerInfo> => {
      return await MockPurchases.purchaseStoreProduct(storeProduct)
    }

    return await this.handleSharedPurchase(performPurchase)
  }

  // Shared purchase handler
  private async handleSharedPurchase(
    performPurchase: () => Promise<CustomerInfo>
  ): Promise<PurchaseResult> {
    try {
      // Perform the purchase using the function provided
      const customerInfo = await performPurchase()

      // Handle the results
      if (this.hasActiveEntitlementOrSubscription(customerInfo)) {
        return { type: 'purchased' }
      } else {
        return {
          type: 'failed',
          error: 'No active subscriptions found.'
        }
      }
    } catch (error: any) {
      // Handle different error types
      if (error.code === PurchasesErrorCode.paymentPendingError) {
        return { type: 'pending' }
      } else if (error.code === PurchasesErrorCode.purchaseCancelledError) {
        return { type: 'cancelled' }
      } else {
        return {
          type: 'failed',
          error: error.message || 'Purchase failed in RCPurchaseController'
        }
      }
    }
  }

  // Handle Restores
  async restorePurchases(): Promise<RestorationResult> {
    try {
      await MockPurchases.restorePurchases()
      return { type: 'restored' }
    } catch (error: any) {
      return {
        type: 'failed',
        error: error.message || 'Restore failed in RCPurchaseController'
      }
    }
  }

  // Helper methods
  private buildSubscriptionOptionId(basePlanId?: string, offerId?: string): string {
    let result = ''

    if (basePlanId) {
      result += basePlanId
    }

    if (offerId) {
      if (basePlanId) {
        result += ':'
      }
      result += offerId
    }

    return result
  }

  private hasActiveEntitlementOrSubscription(customerInfo: CustomerInfo): boolean {
    return (
      customerInfo.activeSubscriptions.length > 0 || 
      Object.keys(customerInfo.entitlements.active).length > 0
    )
  }

  private async getAllProducts(productIdentifiers: string[]): Promise<StoreProduct[]> {
    const subscriptionProducts = await MockPurchases.getProducts(
      productIdentifiers, 
      { productCategory: ProductCategory.subscription }
    )
    const nonSubscriptionProducts = await MockPurchases.getProducts(
      productIdentifiers, 
      { productCategory: ProductCategory.nonSubscription }
    )
    return [...subscriptionProducts, ...nonSubscriptionProducts]
  }
} 