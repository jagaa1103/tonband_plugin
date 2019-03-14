var exec = require('cordova/exec');

exports.exitApp = function(success, error){
    exec(success, error, 'TonbandPlugin', 'exitApp');
}

exports.checkBluetooth = function(success, error){
    exec(success, error, 'TonbandPlugin', 'checkBluetooth');
}

exports.startService = function(success, error){
    exec(success, error, 'TonbandPlugin', 'startService');
}

exports.stopService = function(success, error){
    exec(success, error, 'TonbandPlugin', 'stopService');
}

exports.startScan = function(success, error){
    exec(success, error, 'TonbandPlugin', 'startScan');
}
exports.stopScan = function(success, error){
    exec(success, error, 'TonbandPlugin', 'stopScan');
}

exports.connect = function(deviceID, success, error){
    exec(success, error, 'TonbandPlugin', 'connect', [deviceID]);
}

exports.disconnect = function(success, error){
    exec(success, error, 'TonbandPlugin', 'disconnect');
}

exports.startLoop = function(time, success, error){
    exec(success, error, 'TonbandPlugin', 'startLoop', [time]);
}

exports.resetSettings = function(time, success, error){
    exec(success, error, 'TonbandPlugin', 'resetSettings', [time]);
}

exports.setAlarmTemperature = function(temp_hex, success, error){
    exec(success, error, 'TonbandPlugin', 'setAlarmTemperature', [temp_hex]);
}

exports.requestBattery = function(success, error){
    exec(success, error, 'TonbandPlugin', 'requestBattery');
}

exports.reconnectionStart =  function(success, error){
    exec(success, error, 'TonbandPlugin', 'reconnectionStart');
}