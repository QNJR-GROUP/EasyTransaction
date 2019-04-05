package com.yiqiniu.easytrans.monitor.server;

import com.yiqiniu.easytrans.monitor.StringCodecMonitor;
import com.yiqiniu.easytrans.stringcodec.ListableStringCodec;
import com.yiqiniu.easytrans.stringcodec.StringCodec;

public class ServerSideStringCodecMonitor implements StringCodecMonitor {
    
    
    private StringCodec stringCodec;
    
    public ServerSideStringCodecMonitor(StringCodec stringCodec) {
        super();
        this.stringCodec = stringCodec;
    }

    @Override
    public Object getString2IdMap() {
        
        if(stringCodec instanceof ListableStringCodec) {
            ListableStringCodec lsc = (ListableStringCodec) stringCodec;
            return lsc.getMapStr2Id();
        }
        
        return null;
    }


}
