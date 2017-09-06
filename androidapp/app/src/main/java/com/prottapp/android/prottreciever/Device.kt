package com.prottapp.android.prottreciever

class Device() {
    var token: String? = null
    var name: String? = null
    var os: Int? = null

    constructor(token: String?, name: String?, os: Int?): this() {
        this.token = token
        this.name = name
        this.os = os
    }
}