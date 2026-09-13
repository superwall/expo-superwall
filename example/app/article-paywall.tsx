import { BlurView } from "expo-blur"
import type { PresentationResult } from "expo-superwall"
import {
  PaywallView,
  PresentationResultPaywall,
  SuperwallLoaded,
  SuperwallLoading,
  SuperwallProvider,
  useSuperwall,
  useSuperwallEvents,
} from "expo-superwall"
import { useCallback, useEffect, useRef, useState } from "react"
import {
  ActivityIndicator,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native"

const IOS_API_KEY = "pk_e361c8a9662281f4249f2fa11d1a63854615fa80e15e7a4d"
const ANDROID_API_KEY = "pk_6d16c4c892b1e792490ab8bfe831f1ad96e7c18aee7a5257"

// These platform-specific placements match the campaigns configured for the checked-in
// example API keys. In your app, use the placement names from your own dashboard.
const INLINE_PLACEMENT = Platform.select({ ios: "test", android: "campaign_trigger" }) ?? "test"
const ALL_PLANS_PLACEMENT = Platform.select({ ios: "pro", android: "premium" }) ?? "pro"
const SHOW_ALL_PLANS_ACTION = "showFromInline"

type EmbedMode = "inline" | "overlay"
type GateDecision = "checking" | "gated" | "unlocked"

function presentationResultType(result: PresentationResult): string {
  const nativeType = (result as PresentationResult & { type?: unknown }).type
  if (typeof nativeType === "string") return nativeType
  return result.constructor.name.replace("PresentationResult", "")
}

function ArticlePaywallDemo() {
  const [mode, setMode] = useState<EmbedMode>("inline")
  const [gateDecision, setGateDecision] = useState<GateDecision>("checking")
  const [gateReason, setGateReason] = useState("Checking the remote placement…")
  const [inlineStatus, setInlineStatus] = useState("Waiting for the remote decision…")
  const [fullScreenPlacement, setFullScreenPlacement] = useState<string | null>(null)
  const [fullScreenStatus, setFullScreenStatus] = useState("idle")
  const [fullScreenError, setFullScreenError] = useState<string | null>(null)
  const evaluationGeneration = useRef(0)

  const getPresentationResult = useSuperwall((store) => store.getPresentationResult)

  const refreshGateDecision = useCallback(async () => {
    evaluationGeneration.current += 1
    const generation = evaluationGeneration.current
    setGateDecision("checking")
    setGateReason("Checking the remote placement…")

    try {
      const result = await getPresentationResult(INLINE_PLACEMENT)
      if (generation !== evaluationGeneration.current) return

      const resultType = presentationResultType(result)
      const shouldPresent = result instanceof PresentationResultPaywall || resultType === "Paywall"

      setGateDecision(shouldPresent ? "gated" : "unlocked")
      setGateReason(`Presentation result: ${resultType}`)
      setInlineStatus(shouldPresent ? "Loading inline paywall…" : "Article unlocked remotely")
    } catch (error) {
      if (generation !== evaluationGeneration.current) return

      const message = error instanceof Error ? error.message : String(error)
      // If a remote decision cannot provide a paywall, do not strand the reader behind a blur.
      setGateDecision("unlocked")
      setGateReason(`Presentation result failed open: ${message}`)
      setInlineStatus("Article unlocked because the remote decision failed")
    }
  }, [getPresentationResult])

  useEffect(() => {
    void refreshGateDecision()
    return () => {
      evaluationGeneration.current += 1
    }
  }, [refreshGateDecision])

  const presentFullScreen = (placement: string) => {
    setFullScreenError(null)
    setFullScreenStatus("loading")
    setFullScreenPlacement(placement)
  }

  const previewFullScreen = () => {
    // A native paywall view/controller is single-use. Keep the embedded paywall mounted and
    // retrieve a separate placement for the full-screen presentation, matching the production flow.
    presentFullScreen(ALL_PLANS_PLACEMENT)
  }

  useSuperwallEvents({
    onCustomPaywallAction: (name) => {
      if (name === SHOW_ALL_PLANS_ACTION) {
        presentFullScreen(ALL_PLANS_PLACEMENT)
      }
    },
  })

  const paywall = (
    <PaywallView
      placement={INLINE_PLACEMENT}
      style={[styles.paywallFrame, mode === "overlay" && styles.overlayFrame]}
      renderFallback={(state) => {
        if (state.status === "loading") {
          return (
            <View pointerEvents="none" style={styles.paywallPlaceholder}>
              <ActivityIndicator color="#6d5dfc" />
              <Text style={styles.statusText}>{inlineStatus}</Text>
            </View>
          )
        }

        if (state.status === "error") {
          return (
            <View style={styles.paywallPlaceholder}>
              <Text style={styles.statusText}>Unable to load this offer.</Text>
              <Text style={styles.errorText}>{state.error}</Text>
            </View>
          )
        }

        // A skipped or dismissed inline paywall leaves no empty native view behind.
        return null
      }}
      onPresent={(info) => setInlineStatus(`Loaded ${info.name}`)}
      onDismiss={(_, result) => {
        setInlineStatus(`Inline result: ${result.type}`)
        if (result.type === "purchased" || result.type === "restored") {
          void refreshGateDecision()
        }
      }}
      onSkip={(reason) => {
        setInlineStatus(`Inline skipped: ${reason.type}`)
        setGateDecision("unlocked")
      }}
      onError={(error) => setInlineStatus(`Inline error: ${error}`)}
    />
  )

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.article}>
        <Text style={styles.eyebrow}>THE SUPERWALL JOURNAL</Text>
        <Text style={styles.title}>How thoughtful paywalls can keep readers in the story</Text>
        <Text style={styles.byline}>By Superwall · 8 min read</Text>

        <View style={styles.modePicker}>
          {(["inline", "overlay"] as const).map((value) => (
            <Pressable
              accessibilityRole="button"
              key={value}
              onPress={() => setMode(value)}
              style={[styles.modeButton, mode === value && styles.modeButtonSelected]}
            >
              <Text style={[styles.modeLabel, mode === value && styles.modeLabelSelected]}>
                {value === "inline" ? "Inline" : "Overlay"}
              </Text>
            </Pressable>
          ))}
        </View>
        <Text style={styles.gateStatus}>{gateReason}</Text>

        <Text style={styles.lede}>
          A subscription prompt does not have to interrupt the reading experience. An embedded
          paywall can sit naturally between editorial sections while still giving readers a clear
          path to every available plan.
        </Text>
        <Text style={styles.paragraph}>
          The first section remains readable, preserving context and trust. Superwall retrieves the
          paywall selected for the placement and mounts its native view directly inside this React
          Native layout.
        </Text>

        {mode === "inline" && gateDecision === "gated" && paywall}

        <View style={styles.gatedSection}>
          <Text style={styles.sectionTitle}>Designing the moment of conversion</Text>
          <Text style={styles.paragraph}>
            The strongest subscription moments feel like part of the product. They explain what
            comes next, connect the offer to the reader’s intent, and make alternate plans easy to
            discover without losing their place in the article.
          </Text>
          <Text style={styles.paragraph}>
            The embedded paywall can fire the custom action named {SHOW_ALL_PLANS_ACTION}. This
            example responds by retrieving {ALL_PLANS_PLACEMENT} and presenting it full-screen with
            the same getPaywall flow.
          </Text>
          {gateDecision !== "unlocked" && (
            <BlurView
              experimentalBlurMethod={Platform.OS === "android" ? "dimezisBlurView" : undefined}
              intensity={28}
              tint="light"
              pointerEvents="none"
              style={styles.blur}
            />
          )}
        </View>

        <Text style={styles.debugStatus}>Full-screen state: {fullScreenStatus}</Text>
        {fullScreenError && <Text style={styles.errorText}>{fullScreenError}</Text>}
        <Pressable
          accessibilityRole="button"
          onPress={previewFullScreen}
          style={styles.previewButton}
        >
          <Text style={styles.previewButtonLabel}>Preview full-screen presentation</Text>
        </Pressable>
        <View style={styles.bottomSpacer} />
      </ScrollView>

      {mode === "overlay" && gateDecision === "gated" && paywall}

      {fullScreenPlacement && (
        <Modal
          animationType="slide"
          onRequestClose={() => setFullScreenPlacement(null)}
          presentationStyle="fullScreen"
          statusBarTranslucent
          visible
        >
          <View style={styles.fullScreenPaywall}>
            <PaywallView
              key={fullScreenPlacement}
              placement={fullScreenPlacement}
              style={StyleSheet.absoluteFill}
              renderFallback={(state) =>
                state.status === "loading" ? (
                  <View pointerEvents="none" style={styles.fullScreenPlaceholder}>
                    <ActivityIndicator color="#6d5dfc" size="large" />
                  </View>
                ) : null
              }
              onPresent={(info) => setFullScreenStatus(`presented ${info.name}`)}
              onDismiss={(_, result, shouldDismiss) => {
                setFullScreenStatus(`dismissed: ${result.type}`)
                if (result.type === "purchased" || result.type === "restored") {
                  void refreshGateDecision()
                }
                if (shouldDismiss) setFullScreenPlacement(null)
              }}
              onSkip={(reason) => {
                setFullScreenStatus(`skipped: ${reason.type}`)
                setFullScreenPlacement(null)
              }}
              onError={(error) => {
                setFullScreenError(error)
                setFullScreenStatus("error")
                setFullScreenPlacement(null)
              }}
            />
          </View>
        </Modal>
      )}
    </View>
  )
}

export default function ArticlePaywallPage() {
  return (
    <SuperwallProvider
      apiKeys={{ ios: IOS_API_KEY, android: ANDROID_API_KEY }}
      options={{ testModeBehavior: __DEV__ ? "always" : "automatic" }}
    >
      <SuperwallLoading>
        <View style={styles.loadingScreen}>
          <ActivityIndicator size="large" color="#6d5dfc" />
        </View>
      </SuperwallLoading>
      <SuperwallLoaded>
        <ArticlePaywallDemo />
      </SuperwallLoaded>
    </SuperwallProvider>
  )
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f8f6f1",
  },
  loadingScreen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f8f6f1",
  },
  fullScreenPaywall: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  fullScreenPlaceholder: {
    ...StyleSheet.absoluteFill,
    alignItems: "center",
    justifyContent: "center",
  },
  article: {
    paddingHorizontal: 22,
    paddingTop: 28,
  },
  eyebrow: {
    color: "#6d5dfc",
    fontSize: 12,
    fontWeight: "800",
    letterSpacing: 1.8,
  },
  title: {
    color: "#171625",
    fontSize: 35,
    fontWeight: "800",
    letterSpacing: -1.2,
    lineHeight: 39,
    marginTop: 12,
  },
  byline: {
    color: "#77727f",
    fontSize: 14,
    marginTop: 12,
  },
  modePicker: {
    alignSelf: "flex-start",
    backgroundColor: "#e9e5dd",
    borderRadius: 12,
    flexDirection: "row",
    marginVertical: 24,
    padding: 4,
  },
  gateStatus: {
    color: "#85808a",
    fontSize: 12,
    marginBottom: 4,
    marginTop: -14,
  },
  modeButton: {
    borderRadius: 9,
    paddingHorizontal: 18,
    paddingVertical: 9,
  },
  modeButtonSelected: {
    backgroundColor: "#ffffff",
  },
  modeLabel: {
    color: "#716d75",
    fontSize: 14,
    fontWeight: "700",
    textTransform: "capitalize",
  },
  modeLabelSelected: {
    color: "#302b48",
  },
  lede: {
    color: "#292733",
    fontSize: 20,
    fontWeight: "600",
    lineHeight: 30,
  },
  paragraph: {
    color: "#4b4853",
    fontSize: 17,
    lineHeight: 28,
    marginTop: 18,
  },
  paywallFrame: {
    backgroundColor: "#ffffff",
    borderColor: "#e5e0d7",
    borderRadius: 20,
    borderWidth: 1,
    height: 330,
    marginHorizontal: -6,
    marginTop: 28,
    overflow: "hidden",
  },
  overlayFrame: {
    bottom: 18,
    height: 300,
    left: 16,
    marginHorizontal: 0,
    marginTop: 0,
    position: "absolute",
    right: 16,
    shadowColor: "#221e35",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.18,
    shadowRadius: 20,
  },
  paywallPlaceholder: {
    ...StyleSheet.absoluteFill,
    alignItems: "center",
    gap: 10,
    justifyContent: "center",
    padding: 24,
  },
  statusText: {
    color: "#6f6978",
    fontSize: 13,
    textAlign: "center",
  },
  gatedSection: {
    marginTop: 28,
    overflow: "hidden",
    paddingBottom: 24,
    position: "relative",
  },
  sectionTitle: {
    color: "#201e2a",
    fontSize: 26,
    fontWeight: "800",
    lineHeight: 32,
  },
  blur: {
    ...StyleSheet.absoluteFill,
    top: 76,
  },
  debugStatus: {
    color: "#85808a",
    fontSize: 12,
    marginTop: 16,
  },
  errorText: {
    color: "#b42318",
    fontSize: 12,
    lineHeight: 18,
    marginTop: 6,
  },
  previewButton: {
    alignItems: "center",
    backgroundColor: "#302b48",
    borderRadius: 12,
    marginTop: 12,
    paddingHorizontal: 18,
    paddingVertical: 13,
  },
  previewButtonLabel: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "700",
  },
  bottomSpacer: {
    height: 80,
  },
})
