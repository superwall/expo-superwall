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
  s.dependency "SuperwallKit", '4.15.3'


  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = "**/*.{h,m,mm,swift,hpp,cpp}"
end
