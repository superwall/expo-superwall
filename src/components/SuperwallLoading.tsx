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
 * Note: This will not render if there's a configuration error. Use `SuperwallError`
 * to handle error states.
 *
 * @param props - The properties for the SuperwallLoading component.
 * @param props.children - The content to render while Superwall is loading.
 * @returns The children if Superwall is loading/not configured, otherwise `null`.
 */
export const SuperwallLoading = ({ children }: { children: React.ReactNode }) => {
  const { isLoaded, hasError } = useSuperwall((state) => ({
    isLoaded: !state.isLoading && state.isConfigured,
    hasError: state.configurationError !== null,
  }))

  if (isLoaded || hasError) {
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
  const { isLoaded, hasError } = useSuperwall((state) => ({
    isLoaded: !state.isLoading && state.isConfigured,
    hasError: state.configurationError !== null,
  }))

  if (!isLoaded || hasError) {
    return null
  }

  return children
}

/**
 * @category Components
 * @since 0.1.0
 * A component that renders its children only when Superwall configuration has failed.
 *
 * This component can be used to display an error message or fallback UI
 * when the Superwall SDK fails to initialize. The children can be either
 * static React nodes or a render function that receives the error message.
 *
 * @param props - The properties for the SuperwallError component.
 * @param props.children - The content to render when Superwall has an error.
 *                         Can be static React nodes or a function that receives the error message string.
 * @returns The children if Superwall has failed, otherwise `null`.
 *
 * @example
 * ```tsx
 * // Static error UI
 * <SuperwallError>
 *   <Text>Failed to load Superwall</Text>
 * </SuperwallError>
 *
 * // Dynamic error UI with error message
 * <SuperwallError>
 *   {(error) => (
 *     <View>
 *       <Text>Failed to initialize Superwall</Text>
 *       <Text>{error}</Text>
 *       <Button title="Retry" onPress={handleRetry} />
 *     </View>
 *   )}
 * </SuperwallError>
 * ```
 */
export const SuperwallError = ({
  children,
}: {
  children: React.ReactNode | ((error: string) => React.ReactNode)
}) => {
  const error = useSuperwall((state) => state.configurationError)

  if (!error) {
    return null
  }

  return typeof children === "function" ? children(error) : children
}
