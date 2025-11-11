import SuperwallKit

extension IntegrationAttribute {
  static func fromString(_ string: String) -> IntegrationAttribute? {
    switch string {
    case "adjustId": return .adjustId
    case "amplitudeDeviceId": return .amplitudeDeviceId
    case "amplitudeUserId": return .amplitudeUserId
    case "appsflyerId": return .appsflyerId
    case "brazeAliasName": return .brazeAliasName
    case "brazeAliasLabel": return .brazeAliasLabel
    case "onesignalId": return .onesignalId
    case "fbAnonId": return .fbAnonId
    case "firebaseAppInstanceId": return .firebaseAppInstanceId
    case "iterableUserId": return .iterableUserId
    case "iterableCampaignId": return .iterableCampaignId
    case "iterableTemplateId": return .iterableTemplateId
    case "mixpanelDistinctId": return .mixpanelDistinctId
    case "mparticleId": return .mparticleId
    case "clevertapId": return .clevertapId
    case "airshipChannelId": return .airshipChannelId
    case "kochavaDeviceId": return .kochavaDeviceId
    case "tenjinId": return .tenjinId
    case "posthogUserId": return .posthogUserId
    case "customerioId": return .customerioId
    default: return nil
    }
  }
}
