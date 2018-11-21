var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'tonband_plugin', 'coolMethod', [arg0]);
};

exports.checkBluetooth = function(success, error){
    exec(success, error, 'tonband_plugin', 'checkBluetooth');
}

exports.scan = function(success, error, serviceUUID){
    exec(success, error, 'tonband_plugin', 'scan', [serviceUUID]);
}

exports.connect = function(deviceID, success, error){
    exec(success, error, 'tonband_plugin', 'scan', [deviceID]);
}