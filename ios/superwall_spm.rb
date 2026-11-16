# Links the SuperwallKit Swift package into the app.
#
# `ios/SuperwallExpo.podspec` declares the package with React Native's `spm_dependency`. That
# helper (react-native/scripts/cocoapods/spm.rb) adds the package product to this pod's target
# in the Pods project and nothing else. The pod builds as a static library, which doesn't link
# its dependencies, so the app target has to depend on the product too for SuperwallKit and its
# resource bundle to be in the app. This file does that on every `pod install`, so apps need no
# Podfile or Xcode changes: it wraps CocoaPods' post-integrate step, the point at which CocoaPods
# has finished writing the app's Xcode project.
#
# Everything here is idempotent, because CocoaPods can load a podspec more than once and the
# app's project, unlike the Pods project, is kept between installs.
module SuperwallSPM
  POD_NAME = 'SuperwallExpo'.freeze
  PACKAGE_URL = 'https://github.com/superwall/Superwall-iOS'.freeze
  PRODUCT_NAME = 'SuperwallKit'.freeze

  class << self
    # Called by the podspec with the pinned version. Returns the package requirement.
    def activate!(version)
      @requirement = { kind: 'exactVersion', version: version }
    end

    # Links the package product into every app target that uses the SuperwallExpo pod.
    def apply_user_project(installer)
      return if @requirement.nil?

      installer.aggregate_targets.each do |aggregate_target|
        next unless aggregate_target.pod_targets.any? { |target| target.pod_name == POD_NAME }

        # Nil when the Podfile doesn't integrate targets; there's no app project to edit then.
        project = aggregate_target.user_project
        next if project.nil?

        changed = false
        aggregate_target.user_targets.each do |user_target|
          # Extensions and tests don't link the pod's code themselves.
          next unless user_target.respond_to?(:symbol_type) && user_target.symbol_type == :application

          changed = true if link_package(project, user_target)
        end
        # CocoaPods has already saved the project by now.
        project.save if changed
      end
    end

    # TEMPORARY PROBE. Reports through a failing build whether Xcode copies SuperwallKit's
    # resource bundle into the app when only the pod depends on the package.
    PROBE_PHASE_NAME = '[expo-superwall] Probe SuperwallKit resources'.freeze
    PROBE_SCRIPT = <<~'SCRIPT'.freeze
      APP="${TARGET_BUILD_DIR}/${UNLOCALIZED_RESOURCES_FOLDER_PATH}"
      IN_APP="$(cd "$APP" 2>/dev/null && ls -d *uperwall*.bundle 2>/dev/null | tr '\n' ',')"
      BUILT="$(cd "${BUILT_PRODUCTS_DIR}" 2>/dev/null && ls -d *uperwall*.bundle *uperwall*.o *uperscript* 2>/dev/null | tr '\n' ',')"
      UNINSTALLED="$(cd "${OBJROOT}/UninstalledProducts/${PLATFORM_NAME}" 2>/dev/null && ls -d *uperwall* 2>/dev/null | tr '\n' ',')"
      FRAMEWORKS="$(cd "${TARGET_BUILD_DIR}/${FRAMEWORKS_FOLDER_PATH}" 2>/dev/null && ls 2>/dev/null | grep -i 'super' | tr '\n' ',')"
      echo "error: SUPERWALL_PROBE in_app=[${IN_APP}] built=[${BUILT}] uninstalled=[${UNINSTALLED}] frameworks=[${FRAMEWORKS}] sandbox=${ENABLE_USER_SCRIPT_SANDBOXING}"
      exit 1
    SCRIPT

    def link_package(project, target)
      phase = target.shell_script_build_phases.find { |p| p.name == PROBE_PHASE_NAME }
      return false if phase && phase.shell_script == PROBE_SCRIPT

      phase ||= target.new_shell_script_build_phase(PROBE_PHASE_NAME)
      phase.shell_path = '/bin/sh'
      phase.shell_script = PROBE_SCRIPT
      phase.always_out_of_date = '1'
      true
    end

    private

    def same_repository?(url)
      url.to_s.sub(/\.git\z/, '').casecmp?(PACKAGE_URL)
    end
  end
end

# Wrap the installer's post-integrate step, once. The method is private in CocoaPods and
# `alias_method` keeps that, so both visibilities are checked before wrapping again.
if defined?(Pod::Installer)
  installer_class = Pod::Installer
  hooked = %i[method_defined? private_method_defined?].any? do |check|
    installer_class.send(check, :superwall_spm_original_run_podfile_post_integrate_hooks)
  end
  # CocoaPods 1.10 and later; every React Native version with `spm_dependency` needs newer.
  hookable = %i[method_defined? private_method_defined?].any? do |check|
    installer_class.send(check, :run_podfile_post_integrate_hooks)
  end

  if hookable && !hooked
    was_private = installer_class.private_method_defined?(:run_podfile_post_integrate_hooks)
    installer_class.class_eval do
      alias_method :superwall_spm_original_run_podfile_post_integrate_hooks,
                   :run_podfile_post_integrate_hooks

      define_method(:run_podfile_post_integrate_hooks) do
        begin
          SuperwallSPM.apply_user_project(self)
        rescue StandardError => e
          # Never fail the install over this: say what to do by hand instead.
          Pod::UI.warn "[expo-superwall] Couldn't link the SuperwallKit Swift package into the " \
                       "app target (#{e.class}: #{e.message}). If the app fails to link, add " \
                       "the package #{SuperwallSPM::PACKAGE_URL} to the app target in Xcode " \
                       "with the #{SuperwallSPM::PRODUCT_NAME} product."
        end
        superwall_spm_original_run_podfile_post_integrate_hooks
      end
    end
    installer_class.send(:private, :run_podfile_post_integrate_hooks) if was_private
  end
end
