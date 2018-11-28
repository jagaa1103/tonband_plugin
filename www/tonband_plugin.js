var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'TonbandPlugin', 'coolMethod', [arg0]);
};

exports.checkBluetooth = function(success, error){
    exec(success, error, 'TonbandPlugin', 'checkBluetooth');
}

exports.startService = function(success, error){
    exec(success, error, 'TonbandPlugin', 'startService');
}

exports.scan = function(success, error){
    exec(success, error, 'TonbandPlugin', 'scan');
}

exports.connect = function(deviceID, success, error){
    exec(success, error, 'TonbandPlugin', 'connect', [deviceID]);
}

exports.startLoop = function(success, error){
    exec(success, error, 'TonbandPlugin', 'startLoop');
}

exports.resetSettings = function(time, success, error){
    exec(success, error, 'TonbandPlugin', 'resetSettings', [time]);
}