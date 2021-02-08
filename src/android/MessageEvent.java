package com.vishnu.socketio;

import org.json.JSONObject;

public class MessageEvent {
    private String name;
    private String event;
    private Object data;
    private boolean showAlert ;
    MessageEvent(String name,String event,Object data,Boolean showAlert){
        this.showAlert = showAlert;
        this.name = name;
        this.event= event;
        this.data = data;
    }

    public Boolean getShowAlert(){
        return  showAlert;
    }

    public Object getData() {
        return data;
    }
}
