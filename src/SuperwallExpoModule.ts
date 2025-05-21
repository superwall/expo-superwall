import { NativeModule, requireNativeModule } from "expo";

declare class SuperwallExpoModule extends NativeModule {
	PI: number;
	hello(): string;
	setValueAsync(value: string): Promise<void>;
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo");
