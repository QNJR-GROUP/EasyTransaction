package com.yiqiniu.easytrans.stringcodec;

import java.util.Map;

/**
 * @author xu deyou
 *
 */
public interface ListableStringCodec extends StringCodec {
    Map<String/*type*/, Map<String,Integer>> getMapStr2Id();

}
