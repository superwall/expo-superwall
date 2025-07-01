import { useSuperwall } from "../useSuperwall"

/**
 * @category Components
 * @since 0.0.15
 * A component that renders its children only when Superwall is loading or not yet configured.
 *
 * This component can be used to display a loading indicator or a placeholder
 * while the Superwall SDK is initializing. Once Superwall is configured and
 * no longer in a loading state, this component will render `null`.
 *
 * @param props - The properties for the SuperwallLoading component.
 * @param props.children - The content to render while Superwall is loading.
 * @returns The children if Superwall is loading/not configured, otherwise `null`.
 */
export const SuperwallLoading = ({ children }: { children: React.ReactNode }) => {
  const isLoaded = useSuperwall((state) => !state.isLoading && state.isConfigured)

  if (isLoaded) {
    return null
  }

  return children
}

/**
 * @category Components
 * @since 0.0.15
 * A component that renders its children only when Superwall has finished loading and is configured.
 *
 * This component is useful for conditionally rendering parts of your UI that
 * depend on Superwall being ready. If Superwall is still loading or has not
 * been configured, this component will render `null`.
 *
 * @param props - The properties for the SuperwallLoaded component.
 * @param props.children - The content to render once Superwall is loaded and configured.
 * @returns The children if Superwall is loaded/configured, otherwise `null`.
 */
export const SuperwallLoaded = ({ children }: { children: React.ReactNode }) => {
  const isLoaded = useSuperwall((state) => !state.isLoading && state.isConfigured)

  if (!isLoaded) {
    return null
  }

  return children
}
