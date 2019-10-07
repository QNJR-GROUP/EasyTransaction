package com.yiqiniu.easytrans.extensionsuite.impl.database;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.yiqiniu.easytrans.stringcodec.ListableStringCodec;

/**
 * 
 * @author deyou
 *
 */
@SuppressWarnings("unused")
public class DatabaseStringCodecImpl implements ListableStringCodec {
    
    private Logger logger = LoggerFactory.getLogger(DatabaseStringCodecImpl.class);
    
    private static class DataObject {
        private int keyInt;
        private String typeStr;
        private String valueStr;
        public int getKeyInt() {
            return keyInt;
        }
        public void setKeyInt(int strInt) {
            this.keyInt = strInt;
        }
        public String getTypeStr() {
            return typeStr;
        }
        public void setTypeStr(String strType) {
            this.typeStr = strType;
        }
        public String getValueStr() {
            return valueStr;
        }
        public void setValueStr(String valueStr) {
            this.valueStr = valueStr;
        }
    }
    BeanPropertyRowMapper<DataObject> dataObjectMapper = new BeanPropertyRowMapper<DataObject>(DataObject.class);

    private String GET_ALL = "SELECT key_int,type_str,value_str FROM str_codec";
    private String GET_BY_ID = "SELECT key_int,type_str,value_str FROM str_codec where key_int = ?";
    private String FIND_ID = "SELECT key_int FROM str_codec where type_str = ? and value_str = ?";
//    private String INSERT_ID = "insert into str_codec(key_int,type_str,value_str,create_time) values(?,?,?,now())";
    private String INSERT_ID = "insert into str_codec(key_int,type_str,value_str,create_time) select ifnull(max(key_int),0) + 1,?,?,now() from str_codec ";
//    private String GET_MAX_ID_AND_LOCK = "select max(key_int) from str_codec for update";
    private String GET_MAX_ID_AND_LOCK = "select max(key_int) from str_codec";
    private String GET_KEY_AND_LOCK = "select key_int from str_codec where type_str = ? and value_str = ? ";
//    private String GET_KEY_AND_LOCK = "select key_int from str_codec where type_str = ? and value_str = ?";
	
	private JdbcTemplate jdbcTemplate;
	private DataSource dataSoruce;
	private PlatformTransactionManager transManager;
	private ConcurrentHashMap<String,ConcurrentHashMap<String, Integer>> vale2KeyMapping = new ConcurrentHashMap<String, ConcurrentHashMap<String,Integer>>();
	
	
    public DatabaseStringCodecImpl(String tablePrefix, DataSource dataSource, PlatformTransactionManager transManager) {
        this.dataSoruce = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transManager = transManager;
        
        tablePrefix = tablePrefix.trim();
        if(StringUtils.isNotBlank(tablePrefix)) {
            FIND_ID = addTablePrefix(tablePrefix, FIND_ID);
            INSERT_ID = addTablePrefix(tablePrefix, INSERT_ID);
            GET_MAX_ID_AND_LOCK = addTablePrefix(tablePrefix, GET_MAX_ID_AND_LOCK);
            GET_KEY_AND_LOCK = addTablePrefix(tablePrefix, GET_KEY_AND_LOCK);
            GET_ALL = addTablePrefix(tablePrefix, GET_ALL);
        }
        
        List<DataObject> result = jdbcTemplate.query(GET_ALL, dataObjectMapper);
        for(DataObject data:result) {
            ConcurrentHashMap<String, Integer> typeValue2Key = vale2KeyMapping.computeIfAbsent(data.getTypeStr(), key->new ConcurrentHashMap<>());
            typeValue2Key.computeIfAbsent(data.getValueStr(), key->data.getKeyInt());
        }
        
    }

    @Override
    public Integer findId(String stringType, String value) {

        ConcurrentHashMap<String, Integer> typeValue2Key = vale2KeyMapping.computeIfAbsent(stringType, key->new ConcurrentHashMap<>());
        int resultKey = typeValue2Key.computeIfAbsent(value, k->getOrInsertId(stringType,value));
        return resultKey;
    }

    private Integer getOrInsertId(String stringType, String value) {
        
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionDefinition.setTimeout(3);
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transManager, transactionDefinition);
        
        return transactionTemplate.execute(new TransactionCallback<Integer>() {

            @Override
            public Integer doInTransaction(TransactionStatus status) {
                
                
                for(int i = 0 ; i < 3 ; i ++) {
                    Integer keyInt = getValueId(stringType, value);
                    if(keyInt != null) {
                        return keyInt;
                    }
                    
                    try {
                        jdbcTemplate.update(INSERT_ID, new Object[] {stringType,value});
                    } catch (Exception e) {
                        logger.warn("insert failed,may cause by concurrent,try again, count:" + i + " msg:" + e.getMessage());
                    }
                }
                
                
                Integer valueId = getValueId(stringType, value);
                
                if(valueId == null) {
                    throw new RuntimeException("can not find specified id :" + value);
                }
                
                return valueId;
            }

            private Integer getValueId(String stringType, String value) {
                List<Integer> keyIntList = jdbcTemplate.queryForList(GET_KEY_AND_LOCK, new Object[] {stringType,value}, Integer.class);
                if(!CollectionUtils.isEmpty(keyIntList)) {
                    if(keyIntList.size() == 1) {
                        return keyIntList.get(0);
                    } else {
                        throw new RuntimeException("unexpected row count selected!");
                    }
                }
                return null;
            }
        });
    }

    @Override
    public String findString(String stringType, int id) {
        ConcurrentHashMap<String, Integer> typeValue2Key = vale2KeyMapping.computeIfAbsent(stringType, key->new ConcurrentHashMap<>());
        
        Set<Entry<String, Integer>> entrySet = typeValue2Key.entrySet();
        for(Entry<String, Integer> entry:entrySet) {
            Integer dbKey = entry.getValue();
            if(dbKey.equals(id)) {
                return entry.getKey();
            }
        }
        
        //get from db
        List<DataObject> item = jdbcTemplate.query(GET_BY_ID, new Object[] {id}, dataObjectMapper);
        if(item != null && item.size() == 1) {
            typeValue2Key.put(item.get(0).getValueStr(), item.get(0).getKeyInt());
            return item.get(0).getValueStr();
        }
        
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Map<String, Integer>> getMapStr2Id() {
        Object obj = vale2KeyMapping;
        return (Map<String, Map<String, Integer>>) obj;
    }
    
    private String addTablePrefix(String tablePrefix, String sql) {
        return sql.replace("str_codec", tablePrefix + "str_codec");
    }

}
