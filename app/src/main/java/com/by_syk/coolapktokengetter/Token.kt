package com.by_syk.coolapktokengetter

/**
 * Created by ruixingchen on 10/09/2017.
 */
class Token {

    var time:Long
    var uuid:String
    var token:String

    constructor(time:Long, uuid:String, token:String) {
        this.time = time
        this.uuid = uuid
        this.token = token
    }

}