import React from 'react'
import { TouchableOpacity, Text, StyleSheet } from 'react-native'

interface TestButtonProps {
  title: string
  onPress: () => void
  disabled?: boolean
}

export const TestButton: React.FC<TestButtonProps> = ({ title, onPress, disabled = false }) => {
  return (
    <TouchableOpacity 
      style={[styles.button, disabled && styles.disabled]}
      onPress={onPress}
      disabled={disabled}
      activeOpacity={0.7}
    >
      <Text style={[styles.buttonText, disabled && styles.disabledText]}>
        {title}
      </Text>
    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  button: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    backgroundColor: '#007AFF',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 4,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '500',
  },
  disabled: {
    backgroundColor: '#CCCCCC',
  },
  disabledText: {
    color: '#666666',
  },
}) 