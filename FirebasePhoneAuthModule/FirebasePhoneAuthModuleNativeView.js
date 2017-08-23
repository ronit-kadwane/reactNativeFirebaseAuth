//  Created by react-native-create-bridge

import React, { Component } from 'react'
import { requireNativeComponent } from 'react-native'

const FirebasePhoneAuthModule = requireNativeComponent('FirebasePhoneAuthModule', FirebasePhoneAuthModuleView)

export default class FirebasePhoneAuthModuleView extends Component {
  render () {
    return <FirebasePhoneAuthModule {...this.props} />
  }
}

FirebasePhoneAuthModuleView.propTypes = {
  exampleProp: React.PropTypes.any
}
