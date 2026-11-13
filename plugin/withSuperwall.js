// Expo config plugin for expo-superwall.
//
// On iOS it switches the native SuperwallKit SDK from CocoaPods to Swift Package Manager.
// Two things have to happen for that, and this plugin does both:
//
//   1. `ios/Podfile.properties.json` gets `expo-superwall.spm`, which
//      `ios/SuperwallExpo.podspec` reads to depend on the Swift package instead of the pod.
//   2. The app target links the SuperwallKit package product. CocoaPods builds this module as
//      a static library, and a static library doesn't link its dependencies, so the product
//      (and its resource bundle) only reaches the app if the app target depends on it too.
//
// Keep the version in step with `superwall_spm_version` in `ios/SuperwallExpo.podspec`.

// Normally resolved from this package's own dependencies. When the plugin is used from a
// checkout of this repo without them installed (the example app), fall back to the app's.
const { withPodfileProperties, withXcodeProject } = (() => {
  try {
    return require("expo/config-plugins")
  } catch {
    return require(require.resolve("expo/config-plugins", { paths: [process.cwd()] }))
  }
})()

const PACKAGE_URL = "https://github.com/superwall/Superwall-iOS"
const PACKAGE_NAME = "Superwall-iOS"
const PRODUCT_NAME = "SuperwallKit"
const PACKAGE_VERSION = "4.19.1"
const PODFILE_PROPERTY = "expo-superwall.spm"

const unquote = (value) => (typeof value === "string" ? value.replace(/^"|"$/g, "") : value)

/**
 * Adds the SuperwallKit Swift package to an `xcode` project and links its product into the
 * application target. Safe to run more than once: it reuses what's already there and only
 * updates the pinned version.
 */
function addSuperwallSwiftPackage(project, version = PACKAGE_VERSION) {
  const objects = project.hash.project.objects
  objects.XCRemoteSwiftPackageReference = objects.XCRemoteSwiftPackageReference || {}
  objects.XCSwiftPackageProductDependency = objects.XCSwiftPackageProductDependency || {}
  objects.PBXBuildFile = objects.PBXBuildFile || {}

  const packageComment = `XCRemoteSwiftPackageReference "${PACKAGE_NAME}"`
  const requirement = { kind: "exactVersion", version }

  // The package reference.
  const packageReferences = objects.XCRemoteSwiftPackageReference
  let packageUuid = Object.keys(packageReferences).find(
    (key) =>
      !key.endsWith("_comment") &&
      unquote(packageReferences[key].repositoryURL).replace(/\.git$/, "") === PACKAGE_URL,
  )
  if (packageUuid) {
    packageReferences[packageUuid].requirement = requirement
  } else {
    packageUuid = project.generateUuid()
    packageReferences[packageUuid] = {
      isa: "XCRemoteSwiftPackageReference",
      repositoryURL: `"${PACKAGE_URL}"`,
      requirement,
    }
    packageReferences[`${packageUuid}_comment`] = packageComment
  }

  const pbxProject = project.getFirstProject().firstProject
  pbxProject.packageReferences = pbxProject.packageReferences || []
  if (!pbxProject.packageReferences.some((reference) => reference.value === packageUuid)) {
    pbxProject.packageReferences.push({ value: packageUuid, comment: packageComment })
  }

  // The product the app target depends on.
  const productDependencies = objects.XCSwiftPackageProductDependency
  let productUuid = Object.keys(productDependencies).find(
    (key) =>
      !key.endsWith("_comment") &&
      productDependencies[key].package === packageUuid &&
      unquote(productDependencies[key].productName) === PRODUCT_NAME,
  )
  if (!productUuid) {
    productUuid = project.generateUuid()
    productDependencies[productUuid] = {
      isa: "XCSwiftPackageProductDependency",
      package: packageUuid,
      package_comment: packageComment,
      productName: PRODUCT_NAME,
    }
    productDependencies[`${productUuid}_comment`] = PRODUCT_NAME
  }

  const nativeTargets = project.pbxNativeTargetSection()
  const targetUuid = Object.keys(nativeTargets).find(
    (key) =>
      !key.endsWith("_comment") &&
      unquote(nativeTargets[key].productType) === "com.apple.product-type.application",
  )
  if (!targetUuid) {
    throw new Error("[expo-superwall] Couldn't find the application target in the Xcode project.")
  }
  const target = nativeTargets[targetUuid]
  target.packageProductDependencies = target.packageProductDependencies || []
  if (!target.packageProductDependencies.some((dependency) => dependency.value === productUuid)) {
    target.packageProductDependencies.push({ value: productUuid, comment: PRODUCT_NAME })
  }

  // Linking it: a build file in the target's Frameworks phase.
  const buildFileComment = `${PRODUCT_NAME} in Frameworks`
  let buildFileUuid = Object.keys(objects.PBXBuildFile).find(
    (key) => !key.endsWith("_comment") && objects.PBXBuildFile[key].productRef === productUuid,
  )
  if (!buildFileUuid) {
    buildFileUuid = project.generateUuid()
    objects.PBXBuildFile[buildFileUuid] = {
      isa: "PBXBuildFile",
      productRef: productUuid,
      productRef_comment: PRODUCT_NAME,
    }
    objects.PBXBuildFile[`${buildFileUuid}_comment`] = buildFileComment
  }
  const frameworksPhase = project.pbxFrameworksBuildPhaseObj(targetUuid)
  if (!frameworksPhase) {
    throw new Error("[expo-superwall] The application target has no Frameworks build phase.")
  }
  frameworksPhase.files = frameworksPhase.files || []
  if (!frameworksPhase.files.some((file) => file.value === buildFileUuid)) {
    frameworksPhase.files.push({ value: buildFileUuid, comment: buildFileComment })
  }

  return project
}

/**
 * @param config - The Expo config.
 * @param props - `{ ios: { swiftPackageManager: false } }` keeps SuperwallKit on CocoaPods.
 */
function withSuperwall(config, props = {}) {
  const useSwiftPackageManager = props?.ios?.swiftPackageManager !== false

  config = withPodfileProperties(config, (config) => {
    if (useSwiftPackageManager) {
      config.modResults[PODFILE_PROPERTY] = "true"
    } else {
      delete config.modResults[PODFILE_PROPERTY]
    }
    return config
  })

  if (useSwiftPackageManager) {
    config = withXcodeProject(config, (config) => {
      addSuperwallSwiftPackage(config.modResults)
      return config
    })
  }

  return config
}

module.exports = withSuperwall
module.exports.addSuperwallSwiftPackage = addSuperwallSwiftPackage
module.exports.PACKAGE_VERSION = PACKAGE_VERSION
