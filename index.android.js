/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  NativeEventEmitter,
  NativeModules,
  DeviceEventEmitter
} from 'react-native';

const EventEmitter = new NativeEventEmitter(NativeModules.FirebasePhoneAuthModule)
const { FirebasePhoneAuthModule } = NativeModules

export default class MyNewApp extends Component {

  constructor(props){
    super(props)
    this.cbFromNative = this.cbFromNative.bind(this);
    
  }

  cbFromNative(message){
    console.log("Message >> ", message);
  }

  componentWillMount(){
    console.log("componentWillMount")
    FirebasePhoneAuthModule.exampleMethod("Ronit Kadwane",this.cbFromNative)
  }
  componentWillUnmount () {
    console.log("componentWillUnmount")
    if (EventEmitter.removeAllListeners) EventEmitter.removeAllListeners(FirebasePhoneAuthModule.ON_VERIFICATION_COMPLETED)
    if (EventEmitter.removeAllListeners) EventEmitter.removeAllListeners(FirebasePhoneAuthModule.ON_CODE_AUTO_RETRIEVAL_TIMEOUT)
  }

  componentDidMount(){
    console.log("componentDidMount")
    const phoneNumber  = "Enter you phone number"

    EventEmitter.addListener(FirebasePhoneAuthModule.ON_VERIFICATION_COMPLETED, verificationCode => {
      console.log('[PHONE_AUTH] Verification completed.', verificationCode)
      this.setState({ isSending: false, verificationCode }, this.submit)
    })

    EventEmitter.addListener(FirebasePhoneAuthModule.ON_CODE_AUTO_RETRIEVAL_TIMEOUT, verificationId => {
      console.log('[PHONE_AUTH] Auto verification timeout.', verificationId)
      if (verificationId) this.setState({ verificationId })
    })

    console.log('[PHONE_AUTH] Sending verification code via sms...')
    this.setState({ isSending: true })

    FirebasePhoneAuthModule.verifyPhoneNumber(phoneNumber).then(verificationId => {
      console.log('[PHONE_AUTH] Verification code sent.', verificationId)
      this.setState({ isSending: false, verificationId })
    }).catch(error => {
      console.log('[PHONE_AUTH] Verification code failed.')
      console.error(error)
      this.setState({ isSending: false })
      this.showError(
        `Could not send confirmation code. Please try again.\n\nError: ${error}`,
        true
      )
    })
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.android.js
        </Text>
        <Text style={styles.instructions}>
          Double tap R on your keyboard to reload,{'\n'}
          Shake or press menu button for dev menu
        </Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('MyNewApp', () => MyNewApp);
