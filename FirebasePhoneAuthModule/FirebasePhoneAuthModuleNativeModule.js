//  Created by react-native-create-bridge

import { NativeModules } from 'react-native'

const { FirebasePhoneAuthModule } = NativeModules

export default {
  exampleMethod () {
    return FirebasePhoneAuthModule.exampleMethod()
  },

  EXAMPLE_CONSTANT: FirebasePhoneAuthModule.EXAMPLE_CONSTANT
}
