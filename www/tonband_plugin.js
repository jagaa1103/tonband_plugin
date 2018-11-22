var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'tonband_plugin', 'coolMethod', [arg0]);
};

exports.checkBluetooth = function(success, error){
    exec(success, error, 'tonband_plugin', 'checkBluetooth');
}

exports.startService = function(success, error){
    exec(success, error, 'tonband_plugin', 'startService');
}

exports.scan = function(success, error){
    exec(success, error, 'tonband_plugin', 'scan');
}

exports.connect = function(deviceID, success, error){
    exec(success, error, 'tonband_plugin', 'connect', [deviceID]);
}

exports.startLoop = function(success, error){
    exec(success, error, 'tonband_plugin', 'startLoop');
}