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
  DeviceEventEmitter,
  TextInput,
  Button
} from 'react-native';

const EventEmitter = new NativeEventEmitter(NativeModules.FirebasePhoneAuthModule)
const { FirebasePhoneAuthModule } = NativeModules

export default class MyNewApp extends Component {

  constructor(props) {
    super(props)
    this.cbFromNative = this.cbFromNative.bind(this);
    this.sendBtnClick = this.sendBtnClick.bind(this);
    this.resendBtnClick = this.resendBtnClick.bind(this);
    this.signoutBtnclick = this.signoutBtnclick.bind(this);
    this.verifyOtpBtnClick = this.verifyOtpBtnClick.bind(this);
    this.state = {
      "phoneNo": "",
      "otp": "",
    }
  }
  sendVerificationCode(phoneNumber) {
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
  sendBtnClick() {
    let { phoneNo } = this.state;
    if(phoneNo && phoneNo.length > 10){
      this.sendVerificationCode(phoneNo);
    }
    alert("Send button clicked: " + phoneNo);
  }
  resendBtnClick() {
    let { phoneNo } = this.state;
    FirebasePhoneAuthModule.resendVerificationCode(phoneNo).then(verificationId => {
      console.log('[PHONE_AUTH] Verification code REsent.', verificationId)
      this.setState({ isSending: false, verificationId })
    }).catch(error => {
      console.log('[PHONE_AUTH] Verification code REfailed.')
      console.error(error)
      this.setState({ isSending: false })
      this.showError(
        `Could not REsend confirmation code. Please try again.\n\nError: ${error}`,
        true
      )
    })
    alert("Resend button clicked: " + phoneNo);
  }
  signoutBtnclick() {
    let { phoneNo } = this.state;
    FirebasePhoneAuthModule.signOut().then(userNumber => {
      console.log('[PHONE_AUTH] ', userNumber ,' SignOut Successfully.!')
    }).catch(error => {
      console.log('[PHONE_AUTH] ERROR in SignOut.')
      console.error(error)
    })
  }
  verifyOtpBtnClick() {
    let { otp } = this.state;
    FirebasePhoneAuthModule.verifyPhoneNumberWithCode(otp).then(user => {
      console.log('[PHONE_AUTH] SignIn successfully...! >> ', user);
    }).catch(error => {
      console.log('[PHONE_AUTH] Error in SignIn >> ', error)
    })
  }
  cbFromNative(message) {
    console.log("Message >> ", message);
  }

  componentWillMount() {
    console.log("componentWillMount")
    FirebasePhoneAuthModule.exampleMethod("Ronit Kadwane", this.cbFromNative)
  }
  componentWillUnmount() {
    console.log("componentWillUnmount")
    if (EventEmitter.removeAllListeners) EventEmitter.removeAllListeners(FirebasePhoneAuthModule.ON_VERIFICATION_COMPLETED)
    if (EventEmitter.removeAllListeners) EventEmitter.removeAllListeners(FirebasePhoneAuthModule.ON_CODE_AUTO_RETRIEVAL_TIMEOUT)
  }

  componentDidMount() {
    console.log("componentDidMount")
    const phoneNumber = "+919824337236"

    EventEmitter.addListener(FirebasePhoneAuthModule.ON_VERIFICATION_COMPLETED, verificationCode => {
      console.log('[PHONE_AUTH] Verification completed.', verificationCode)
      this.setState({ isSending: false, verificationCode }, this.submit)
    })

    EventEmitter.addListener(FirebasePhoneAuthModule.ON_CODE_AUTO_RETRIEVAL_TIMEOUT, verificationId => {
      console.log('[PHONE_AUTH] Auto verification timeout.', verificationId)
      if (verificationId) this.setState({ verificationId })
    })

    console.log('[PHONE_AUTH] Sending verification code via sms...')
   
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Enter Your Phone Number
        </Text>
        <View style={{ flexDirection: 'column', justifyContent: 'space-between' }}>
          <View>
            <TextInput
              style={{ width: 200 }}
              placeholder={'Enter Phone Number'}
              onChangeText={(text) => this.setState({ phoneNo: text })}
              keyboardType='phone-pad'
            />
          </View>
          <View style={styles.buttons}>
            <Button
              onPress={this.sendBtnClick}
              title="Send Code"
              color="#841584"
              accessibilityLabel="Send code"
              style={styles.buttons}
            />
          </View>
          <View style={styles.buttons}>
            <Button
              onPress={this.resendBtnClick}
              title="Resend Code"
              color="#841584"
              accessibilityLabel="Resend code"
              style={styles.buttons}
            />
          </View>
          <View style={styles.buttons}>
            <Button
              onPress={this.signoutBtnclick}
              title="SignOut"
              color="#841584"
              accessibilityLabel="Call code"
              style={styles.buttons}
            />
          </View>
        </View>
        <Text style={styles.welcome}>
          Enter OTP in Below Box
      </Text>
        <View style={{ flexDirection: 'row' }}>
          <View>
            <TextInput
              style={{ width: 200 }}
              placeholder={'Enter OTP HERE'}
              onChangeText={(text) => this.setState({ otp: text })}
              keyboardType='numeric'
            />
          </View>
          <View>
            <Button
              onPress={this.verifyOtpBtnClick}
              title="Verify OTP"
              color="#841584"
              accessibilityLabel="Call code"
            />
          </View>
        </View>
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
  buttons: {
    marginBottom: 10
  }
});

AppRegistry.registerComponent('MyNewApp', () => MyNewApp);
