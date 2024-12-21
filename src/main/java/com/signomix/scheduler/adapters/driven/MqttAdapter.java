package com.signomix.scheduler.adapters.driven;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

public class MqttAdapter {

    @Channel("adminemail")
    Emitter<byte[]> adminEmailEmitter;

    @Channel("commands")
    Emitter<byte[]> commandEmitter;

    @Channel("devicecommands")
    Emitter<byte[]> deviceCommandEmitter;
    
}
