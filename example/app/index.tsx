import { Text, View } from "react-native";
import * as Superwall from "superwall-expo";

export default function App() {
	return (
		<View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
			<Text>Superwall Expo Example XD</Text>
			<Text>Api Key: {Superwall.getApiKey()}</Text>
		</View>
	);
}
