/**
 *  Xiaomi Smoke Dectector (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Xiaomi Smoke Detector", namespace: "fison67", author: "fison67", ocfDeviceType: "x.com.st.d.sensor.smoke", vid: "generic-smoke", mnmn:"SmartThings") {
		capability "Smoke Detector"
		capability "Configuration"
		capability "Health Check"
		capability "Sensor"
		capability "Refresh"
		capability "Battery"
         
        attribute "density", "string"        
        attribute "lastCheckin", "Date"
        
        command "refresh"
	}


	simulator {
	}

	tiles {
		multiAttributeTile(name:"smoke", type: "generic", width: 6, height: 4){
			tileAttribute ("device.smoke", key: "PRIMARY_CONTROL") {
               	attributeState "clear", label:"Clear", icon:"https://postfiles.pstatic.net/MjAxODA0MDJfMjQ1/MDAxNTIyNjcwOTc3Nzc1.0VHSMcddBSlQwbUJ9XSWcg7sa6NZ-8ljmi4CY2kRt1Mg.k0Yfm71SLHBj2PhJP8jjysHG1brChnS8762CyJju000g.PNG.shin4299/smoke_main_off.png?type=w3" , backgroundColor:"#ffffff"
            	attributeState "detected", label:"Smoke!", icon:"https://postfiles.pstatic.net/MjAxODA0MDJfMTkx/MDAxNTIyNjcwOTc3OTEz.yE02Rca33SFCkHoIG-8OZycz1izfsMJk_jnCft-BMc4g.D4n0ku_kWUwowtxySM6YOLhd6-5KrSQYl90rT0-n58gg.PNG.shin4299/smoke_main_on.png?type=w3" , backgroundColor:"#e86d13"
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}

/*		standardTile("smoke", "device.smoke", width: 2, height: 2) {
			state("clear", label:"Clear", icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff")
			state("detected", label:"Smoke!", icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")
		}
*/
        valueTile("density", "device.density", width: 2, height: 2) {
            state ("val", label:'${currentValue}obs./m', defaultState: true, 
            	backgroundColors:[
                    [value: 00, color: "#fde9e5"],
                    [value: 1000, color: "#600e00"]
                ]
             )
        }
        
        valueTile("battery", "device.battery", width: 2, height: 2) {
            state "val", label:'${currentValue}%', defaultState: true
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
	}
}
		main "smoke"
		details "smoke", "density", "battery", "refresh"

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
	log.debug "${params.key} : ${params.data}"
 	switch(params.key){
    case "smokeDetected":
    	sendEvent(name:"smoke", value: (params.data == "true" ? "detected" : "clear") )
    	break;
    case "density":
    	sendEvent(name:"density", value: params.data)
    	break;
    case "batteryLevel":
    	sendEvent(name:"battery", value: params.data)
    	break;
    }
    
    updateLastTime()
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
        sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
        sendEvent(name:"density", value: jsonObj.properties.density)
        sendEvent(name:"smoke", value: (jsonObj.properties.smokeDetected == "true" ? "detected" : "clear"))
        
        updateLastTime()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
