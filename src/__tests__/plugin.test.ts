import fs from "node:fs"
import os from "node:os"
import path from "node:path"

// The config plugin's Xcode project edit, run on a real Expo-generated project.
jest.mock("expo/config-plugins", () => ({
  withPodfileProperties: jest.fn(),
  withXcodeProject: jest.fn(),
}))

const xcode = require("xcode")
const { addSuperwallSwiftPackage, PACKAGE_VERSION } = require("../../plugin/withSuperwall")

const fixture = path.join(
  __dirname,
  "../../ui_test_app/ios/SuperwallUITestApp.xcodeproj/project.pbxproj",
)

const count = (text: string, needle: string) => text.split(needle).length - 1

describe("config plugin", () => {
  it("links the SuperwallKit package into the app target, once", () => {
    const project = xcode.project(fixture).parseSync()
    addSuperwallSwiftPackage(project)
    addSuperwallSwiftPackage(project)
    const written: string = project.writeSync()

    expect(count(written, "isa = XCRemoteSwiftPackageReference;")).toBe(1)
    expect(written).toContain('repositoryURL = "https://github.com/superwall/Superwall-iOS";')
    expect(written).toMatch(
      new RegExp(
        `requirement = \\{\\s*kind = exactVersion;\\s*version = ${PACKAGE_VERSION.replace(/\./g, "\\.")};`,
      ),
    )
    expect(count(written, "isa = XCSwiftPackageProductDependency;")).toBe(1)
    expect(written).toContain("productName = SuperwallKit;")
    expect(written).toMatch(/packageReferences = \(\s*\w+ \/\* XCRemoteSwiftPackageReference/)
    expect(written).toMatch(/packageProductDependencies = \(\s*\w+ \/\* SuperwallKit \*\//)
    expect(
      count(written, "/* SuperwallKit in Frameworks */ = {isa = PBXBuildFile; productRef"),
    ).toBe(1)
    // Listed once in the Frameworks build phase, besides its own definition.
    expect(count(written, "/* SuperwallKit in Frameworks */,")).toBe(1)

    // What was written is a project the parser reads back, and editing it again changes nothing.
    const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "superwall-plugin-")), "p.pbxproj")
    fs.writeFileSync(file, written)
    const reparsed = xcode.project(file).parseSync()
    addSuperwallSwiftPackage(reparsed)
    expect(reparsed.writeSync()).toBe(written)
  })

  it("moves an existing pin to the given version", () => {
    const project = xcode.project(fixture).parseSync()
    addSuperwallSwiftPackage(project, "4.19.0")
    addSuperwallSwiftPackage(project, "4.20.0")
    const written: string = project.writeSync()

    expect(count(written, "isa = XCRemoteSwiftPackageReference;")).toBe(1)
    expect(written).toContain("version = 4.20.0;")
    expect(written).not.toContain("version = 4.19.0;")
  })
})
