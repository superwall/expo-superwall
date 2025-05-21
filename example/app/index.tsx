import { Text, View } from "react-native";
import * as Settings from "superwall-expo";

export default function App() {
	return (
		<View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
			<Text>Superwall Expo Example</Text>
			<Text>Theme: {Settings.getTheme()}</Text>
		</View>
	);
}
