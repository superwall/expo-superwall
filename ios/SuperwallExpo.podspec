require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

# expo-superwall supports Expo SDK 53+, and ExpoModulesCore's minimum iOS/tvOS
# deployment target differs per SDK (15.1 on SDK 53, 16.4 on SDK 56+). We have
# to land between two hard constraints:
#   * Lower than ExpoModulesCore's target → Swift refuses to import it
#     ("module 'ExpoModulesCore' has a minimum deployment target of iOS 16.4").
#   * Higher than the host app's Podfile platform → CocoaPods refuses to
#     install ("required a higher minimum deployment target").
# So we mirror the ExpoModulesCore that's actually installed, never dropping
# below our own 15.1 floor. One podspec stays correct across every supported
# SDK (and any future bump) without forcing a value on anyone.
expo_modules_core_podspec = begin
  resolved = `node --print "require.resolve('expo-modules-core/package.json')" 2>/dev/null`.strip
  [
    (resolved.empty? ? nil : File.join(File.dirname(resolved), 'ExpoModulesCore.podspec')),
    File.join(__dir__, '..', '..', 'expo-modules-core', 'ExpoModulesCore.podspec'),
  ].compact.find { |path| File.exist?(path) }
rescue StandardError
  # `node` may be unavailable (e.g. resolving from a tarball); fall back to the
  # 15.1 floor below.
  nil
end

expo_modules_core_contents = expo_modules_core_podspec ? File.read(expo_modules_core_podspec) : ''

deployment_target = lambda do |platform, floor|
  found = expo_modules_core_contents[/:#{platform}\s*=>\s*(['"])([\d.]+)\1/, 2]
  [floor, found].compact.max_by { |version| Gem::Version.new(version) }
end

# SuperwallKit comes from CocoaPods by default, or from Swift Package Manager when the app opts
# in. Only the Swift package has 4.19 and later (they aren't on CocoaPods trunk), so the sources
# wrap the API that needs it in `#if SUPERWALL_SPM`.
#
# Opting in: add the `expo-superwall` config plugin to the app config and prebuild. It records
# `expo-superwall.spm` in Podfile.properties.json, read here, and makes the app target link the
# package product: this pod is a static library, which can't link it itself. Apps without
# prebuild can set SUPERWALL_IOS_SPM=1 when running `pod install` and add the package to their
# app target in Xcode. SUPERWALL_IOS_SPM=0 forces CocoaPods.
superwall_spm_version = '4.19.1'

superwall_uses_spm = begin
  case ENV['SUPERWALL_IOS_SPM']
  when '0' then false
  when '1' then true
  else
    properties_path = File.join(Pod::Config.instance.installation_root.to_s, 'Podfile.properties.json')
    File.exist?(properties_path) && JSON.parse(File.read(properties_path))['expo-superwall.spm'] == 'true'
  end
rescue StandardError
  false
end
# React Native's helper, which adds the package to the Pods project after install. It's there
# whenever the Podfile loads `react_native_pods` (React Native 0.75 and later).
superwall_uses_spm &&= respond_to?(:spm_dependency, true)

Pod::Spec.new do |s|
  s.name           = 'SuperwallExpo'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']
  s.platforms      = {
    :ios => deployment_target.call(:ios, '15.1'),
    :tvos => deployment_target.call(:tvos, '15.1')
  }
  s.swift_version  = '5.4'
  s.source         = { git: 'https://github.com/superwall/expo-superwall' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Swift/Objective-C compatibility
  pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  if superwall_uses_spm
    spm_dependency(
      s,
      url: 'https://github.com/superwall/Superwall-iOS',
      requirement: { kind: 'exactVersion', version: superwall_spm_version },
      products: ['SuperwallKit']
    )
    pod_target_xcconfig['SWIFT_ACTIVE_COMPILATION_CONDITIONS'] = '$(inherited) SUPERWALL_SPM'
  else
    s.dependency "SuperwallKit", '4.18.3'
  end

  s.pod_target_xcconfig = pod_target_xcconfig

  s.source_files = "**/*.{h,m,mm,swift,hpp,cpp}"
end
