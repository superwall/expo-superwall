import { NativeModule, requireNativeModule } from "expo";

declare class SuperwallExpoModule extends NativeModule {
	getTheme(): string;
	getApiKey(): string;
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo");
