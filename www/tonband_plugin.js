var exec = require('cordova/exec');

exports.checkBluetooth = function(success, error){
    exec(success, error, 'TonbandPlugin', 'checkBluetooth');
}

exports.startService = function(success, error){
    exec(success, error, 'TonbandPlugin', 'startService');
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

exports.startLoop = function(time, success, error){
    exec(success, error, 'TonbandPlugin', 'startLoop', [time]);
}

exports.resetSettings = function(time, success, error){
    exec(success, error, 'TonbandPlugin', 'resetSettings', [time]);
}